package com.mart.listlock.playactivity;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.LinearLayout;

import com.mart.listlock.R;
import com.mart.listlock.common.LogW;
import com.mart.listlock.common.UserInfo;
import com.mart.listlock.playactivity.spotifyobjects.SongInfoRemovableRow;
import com.mart.listlock.playactivity.spotifyobjects.SpotifySong;
import com.spotify.sdk.android.player.Connectivity;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service implements Player.NotificationCallback, Player.OperationCallback {

    private static final String LOG_TAG = MusicService.class.getName();
    public static final String KEY_PLAYBACK_EVENT = "playbackevent";
    public static final String MUSIC_SERVICE_ACTION = "musicserviceaction";

    private static SpotifyPlayer mPlayer;
    private List<SpotifySong> songs;
    private final IBinder musicBind = new MusicBinder();
    private LinearLayout songListLayout;
    private PlayActivity parent;

    private static Error error;

    @Override
    public void onCreate() {
        super.onCreate();
        LogW.d(LOG_TAG, "created");
        songs = new ArrayList<>();
        if (mPlayer != null) {
            mPlayer.addNotificationCallback(this);
            mPlayer.setConnectivityStatus(this, getNetworkConnectivity(MusicService.this));
        }
    }

    @Override
    public void onPlaybackEvent(PlayerEvent event) {
        LogW.d(LOG_TAG, "playback event received: " + event.name());

        Intent intent = new Intent();

        intent.putExtra(KEY_PLAYBACK_EVENT, event);

        if (getCurrentSong() != null && mPlayer.getPlaybackState().positionMs == getCurrentSong().getInfo().getLength()) {
            try {
                next();
            } catch (MusicServiceException e) {
                e.printStackTrace();
            }
        }

        if (intent.hasExtra(KEY_PLAYBACK_EVENT)) {
            intent.setAction(MUSIC_SERVICE_ACTION);
            sendBroadcast(intent);
        }
    }

    private Connectivity getNetworkConnectivity(Context context) {
        ConnectivityManager connectivityManager;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return Connectivity.fromNetworkType(activeNetwork.getType());
        } else {
            return Connectivity.OFFLINE;
        }
    }

    public void clearSongs() {
        LogW.d(LOG_TAG, "clearing song list");
        songs.clear();
        refreshSongTable();
    }

    @Override
    public void onPlaybackError(Error error) {
        LogW.d(LOG_TAG, "playback error received: " + error.name());
        pause();
        this.error = error;
    }

    public void addSong(SpotifySong song) {
        int lastUnlockedSongIndex = 0;

        for (int i = 0; i < songs.size(); i++) {
            if (!songs.get(i).isLocked()) {
                lastUnlockedSongIndex = i;
            }
        }

        int pos = lastUnlockedSongIndex + 2; // two positions behind last unlocked song

        if (pos > songs.size()) { // if position out of range
            pos = songs.size();
        }

        LogW.d(LOG_TAG, "adding song '" + song.getInfo().getName() + "' to list at position " + pos);
        songs.add(pos, song);
        refreshSongTable();
    }

    public void addAllSongs(List<SpotifySong> songs) {
        for (SpotifySong song : songs) {
            addSong(song);
        }
    }

    public boolean removeSongFromTable(String songId) throws MusicServiceException {
        for (int i = 0; i < songs.size(); i ++) {
            SpotifySong song = songs.get(i);
            if (song.getInfo().getId().equals(songId)) {
                if (i == 0) {
                    next();
                } else {
                    songs.remove(i);
                }

                refreshSongTable();
                return true;
            }
        }
        return false;
    }

    private void refreshSongTable() {
        LogW.d(LOG_TAG, "refreshing song table");

        if (songListLayout == null) {
            LogW.e(LOG_TAG, "no table attached");
            return;
        }

        final LinearLayout headers = (LinearLayout) songListLayout.findViewById(R.id.headers);
        final LinearLayout songList = (LinearLayout) songListLayout.findViewById(R.id.song_list);

        songList.post(new Runnable() {
            @Override
            public void run() {
                songList.removeAllViews();
            }
        });

        for (int i = 0; i < songs.size(); i++) {
            final SpotifySong song = songs.get(i);
            final boolean first = i == 0;
            songList.post(new Runnable() {
                @Override
                public void run() {
                    songList.addView(new SongInfoRemovableRow(song.getInfo(), parent, headers, first, song.isLocked()));
                }
            });
        }
    }

    public void attachSongListLayout(LinearLayout songListLayout) {
        this.songListLayout = songListLayout;
        refreshSongTable();
    }

    public void setContext(PlayActivity parent) {
        this.parent = parent;
    }

    public List<SpotifySong> getSongs() {
        return songs;
    }

    public static void resetError() {
        error = null;
    }

    @Override
    public void onSuccess() {
        Log.d(LOG_TAG, "operation succes");
    }

    @Override
    public void onError(Error error) {
        Log.e(LOG_TAG, "operation error " + error.name());
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void play() throws MusicServiceException {
        play((int) mPlayer.getPlaybackState().positionMs);
    }

    public void play(int position) throws MusicServiceException {
        LogW.d(LOG_TAG, "play called");

        if (songs.isEmpty())
            throw new MusicServiceException(MusicServiceException.ExceptionType.SONG_LIST_EMPTY);

        if (!UserInfo.isPremium())
            throw new MusicServiceException(MusicServiceException.ExceptionType.ACCOUNT_NOT_PREMIUM);

        LogW.d(LOG_TAG, "songs currently in list: " + songs.size());
        mPlayer.playUri(this, songs.get(0).getURI(), 0, position);
    }

    public void pause() {
        LogW.d(LOG_TAG, "pause called");
        mPlayer.pause(this);
    }

    public void next() throws MusicServiceException {
        LogW.d(LOG_TAG, "next called");
        if (songs.size() > 0) {
            songs.remove(0);
        }
        refreshSongTable();
        play(0);
    }

    public void seekToPosition(int positionInMs) {
        mPlayer.seekToPosition(this, positionInMs);
    }

    public static void setPlayer(SpotifyPlayer player) {
        MusicService.mPlayer = player;
    }

    public static SpotifyPlayer player() {
        return MusicService.mPlayer;
    }

    @Override
    public IBinder onBind(Intent intent) {
        LogW.d(LOG_TAG, "bound");
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        LogW.d(LOG_TAG, "unbound");
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogW.d(LOG_TAG, "started");
        return super.onStartCommand(intent, flags, startId);
    }

    public SpotifySong getCurrentSong() {
        if (songs == null || songs.isEmpty()) {
            return null;
        }
        return songs.get(0);
    }

    public static Error error() {
        return error;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogW.d(LOG_TAG, "service destroyed");
        if (mPlayer != null) {
            mPlayer.shutdown();
        }
    }

}

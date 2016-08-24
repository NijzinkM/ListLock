package com.mart.listlock.playactivity;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.mart.listlock.common.LogW;
import com.mart.listlock.playactivity.spotifyobjects.SongInfoRemovableRow;
import com.mart.listlock.playactivity.spotifyobjects.SpotifySong;
import com.mart.listlock.R;
import com.mart.listlock.common.UserInfo;
import com.spotify.sdk.android.player.PlayConfig;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service implements PlayerNotificationCallback {

    private static final String LOG_TAG = MusicService.class.getName();
    public static final String KEY_PLAYBACK_EVENT = "playbackevent";
    public static final String MUSIC_SERVICE_ACTION = "musicserviceaction";

    private static Player mPlayer;
    private List<SpotifySong> songs;
    private final IBinder musicBind = new MusicBinder();
    private int millis= 0;
    private LinearLayout songListLayout;
    private PlayActivity parent;
    private Throwable exception;

    @Override
    public void onCreate() {
        super.onCreate();
        LogW.d(LOG_TAG, "created");
        songs = new ArrayList<>();
        if (mPlayer != null) {
            mPlayer.addPlayerNotificationCallback(this);
        }
    }

    @Override
    public void onPlaybackEvent(EventType eventType, final PlayerState playerState) {
        LogW.d(LOG_TAG, "playback event received: " + eventType.name());

        Intent intent = new Intent();
        try {
            switch (eventType) {
                case PAUSE:
                    intent.putExtra(KEY_PLAYBACK_EVENT, PlaybackEvent.PAUSE);
                    break;
                case END_OF_CONTEXT:
//                    intent.putExtra(KEY_PLAYBACK_EVENT, PlaybackEvent.PAUSE);
                    next();
                    break;
                case TRACK_CHANGED:
                case PLAY:
                    intent.putExtra(KEY_PLAYBACK_EVENT, PlaybackEvent.PLAY);
                    break;
            }
        } catch (MusicServiceException e) {
            if (e.getExceptionType() == MusicServiceException.ExceptionType.SONG_LIST_EMPTY) {
                pause();
                intent.putExtra(KEY_PLAYBACK_EVENT, PlaybackEvent.RESET);
            } else {
                exception = e;
                intent.putExtra(KEY_PLAYBACK_EVENT, PlaybackEvent.ERROR);
            }
        }

        if (intent.hasExtra(KEY_PLAYBACK_EVENT)) {
            intent.setAction(MUSIC_SERVICE_ACTION);
            sendBroadcast(intent);
        }

        LogW.d(LOG_TAG, "position: " + playerState.positionInMs);
    }

    public Throwable getException() {
        return exception;
    }

    public void clearSongs() {
        LogW.d(LOG_TAG, "clearing song list");
        songs.clear();
        refreshSongTable();
    }

    public enum PlaybackEvent {
        PLAY, PAUSE, RESET, ERROR
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {
        LogW.d(LOG_TAG, "playback error received: " + errorType.name());
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

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void play() throws MusicServiceException {
        LogW.d(LOG_TAG, "play called");

        if (songs.isEmpty())
            throw new MusicServiceException(MusicServiceException.ExceptionType.SONG_LIST_EMPTY);

        if (!UserInfo.isPremium())
            throw new MusicServiceException(MusicServiceException.ExceptionType.ACCOUNT_NOT_PREMIUM);

        LogW.d(LOG_TAG, "songs currently in list: " + songs.size());
        mPlayer.play(PlayConfig.createFor(songs.get(0).getURI()).withInitialPosition(millis));
    }

    public void pause() {
        LogW.d(LOG_TAG, "pause called");
        mPlayer.pause();
    }

    public void next() throws MusicServiceException {
        LogW.d(LOG_TAG, "next called");
        if (songs.size() > 0) {
            songs.remove(0);
        }
        refreshSongTable();
        millis = 0;
        play();
    }

    public void seekToPosition(int positionInMs) {
        mPlayer.seekToPosition(positionInMs);
        this.millis = positionInMs;
    }

    public static void setPlayer(Player player) {
        MusicService.mPlayer = player;
    }

    public static Player player() {
        return MusicService.mPlayer;
    }

    public void setMillis(int millis) {
        this.millis = millis;
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogW.d(LOG_TAG, "service destroyed");
        if (mPlayer != null) {
            mPlayer.shutdown();
        }
    }

}

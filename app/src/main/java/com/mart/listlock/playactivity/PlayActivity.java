package com.mart.listlock.playactivity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.mart.listlock.R;
import com.mart.listlock.common.LogW;
import com.mart.listlock.common.SavedPreferences;
import com.mart.listlock.common.UserInfo;
import com.mart.listlock.common.Utils;
import com.mart.listlock.listlockactivity.ListLockActivity;
import com.mart.listlock.playactivity.playlistactivity.PlaylistActivity;
import com.mart.listlock.playactivity.searchactivity.SearchActivity;
import com.mart.listlock.playactivity.spotifyobjects.Playlist;
import com.mart.listlock.playactivity.spotifyobjects.PlaylistInfo;
import com.mart.listlock.playactivity.spotifyobjects.SpotifySong;
import com.mart.listlock.request.SpotifyWebRequestException;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.PlayerEvent;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PlayActivity extends AppCompatActivity {

    private static final String LOG_TAG = PlayActivity.class.getName();
    public static final int SEARCH_REQUEST_CODE = 9129;
    public static final int PLAYLIST_REQUEST_CODE = 1046;
    public static final String KEY_SONG_URI = "song_uri";
    public static final String KEY_PLAYLIST_ID = "playlist_id";
    public static final String KEY_PLAYLIST_OWNER = "playlist_owner";

    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;
    private SeekBar seekBar;
    private Button playPauseButton;
    private ServiceConnection musicConnection;
    private boolean playing;
    private Timer timer;
    private boolean trackingTouch;
    private BroadcastReceiver playbackEventReceiver;
    private IntentFilter intentFilter;
    private Typeface fontAwesome;
    private LinearLayout adminModeBanner;
    private AlertDialog pinDialog;
    private AlertDialog removeSongDialog;
    private AlertDialog errorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        LogW.d(LOG_TAG, "created with" + (savedInstanceState == null ? "out" : "") + " saved bundle");

        fontAwesome = Utils.createFont(this, "fontawesome-webfont.ttf");

        Button buttonPlay = (Button) findViewById(R.id.button_play);
        buttonPlay.setTypeface(fontAwesome);

        Button buttonAddSong = (Button) findViewById(R.id.button_add_song);
        buttonAddSong.setTypeface(fontAwesome);

        Button buttonAddPlaylist = (Button) findViewById(R.id.button_new_playlist);
        buttonAddPlaylist.setTypeface(fontAwesome);

        Button buttonNext = (Button) findViewById(R.id.button_skip);
        buttonNext.setTypeface(fontAwesome);

        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setMax(0);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                trackingTouch = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicService.seekToPosition(seekBar.getProgress());
                trackingTouch = false;
            }
        });

        playPauseButton = (Button) findViewById(R.id.button_play);

        adminModeBanner = (LinearLayout) findViewById(R.id.admin_mode_banner);

        musicConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LogW.d(LOG_TAG, "service connected: " + name.flattenToString());

                MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
                musicService = binder.getService();
                musicService.attachSongListLayout((LinearLayout) findViewById(R.id.song_table));
                musicService.setContext(PlayActivity.this);
                musicBound = true;

                if (!Utils.isNetworkAvailable(PlayActivity.this)) {
                    Utils.showTextBriefly(getString(R.string.no_internet), PlayActivity.this);
                } else if (musicService.getSongs().isEmpty()) {
                    Utils.doWhileLoading(new Utils.Action() {
                        @Override
                        public void execute() {
                            try {
                                for (SpotifySong song : SavedPreferences.getSongs(PlayActivity.this)) {
                                    musicService.addSong(song);
                                }
                            } catch (SpotifyWebRequestException e) {
                                LogW.e(LOG_TAG, "unable to add song", e);
                                Utils.showTextBriefly(getString(R.string.add_song_failed), PlayActivity.this);
                            }
                        }
                    }, PlayActivity.this);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                LogW.d(LOG_TAG, "service disconnected: " + name.flattenToString());
                musicBound = false;
            }
        };
    }

    private class MusicTimer extends Timer {
        public MusicTimer() {
            schedule(new TimerTask() {
                @Override
                public void run() {
                    PlaybackState playbackState = MusicService.player().getPlaybackState();

                    setPlaying(playbackState.isPlaying);
                    if (MusicService.error() != null) {
                        errorDialog = new AlertDialog.Builder(PlayActivity.this)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setMessage(getString(R.string.playback_error, MusicService.error()))
                                .show();
                        MusicService.resetError();
                    } else if (playing && !trackingTouch && !MusicService.player().isShutdown() && musicService != null && musicService.getCurrentSong() != null) {
                        updateSeekBar((int) musicService.getCurrentSong().getInfo().getLength(), (int) playbackState.positionMs);
                    }
                }
            }, 0, 1000);
        }
    }

    private class PlaybackEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            PlayerEvent playbackEvent = (PlayerEvent) intent.getSerializableExtra(MusicService.KEY_PLAYBACK_EVENT);
            LogW.d(LOG_TAG, "playback event received: " + playbackEvent);

            switch (playbackEvent) {
                case kSpPlaybackNotifyPlay:
                    setPlaying(true);
                    break;
                case kSpPlaybackNotifyPause:
                    setPlaying(false);
                    break;
            }
        }
    }

    private void updateSeekBar(int durationInMs, int positionInMs) {
        seekBar.setMax(durationInMs);
        seekBar.setProgress(positionInMs);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogW.d(LOG_TAG, "start called");

        playbackEventReceiver = new PlaybackEventReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(MusicService.MUSIC_SERVICE_ACTION);
        playIntent = new Intent(this, MusicService.class);
        startService(playIntent);
        bindService(playIntent, musicConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected  void onResume() {
        super.onResume();
        LogW.d(LOG_TAG, "resumed");
        registerReceiver(playbackEventReceiver, intentFilter);

        timer = new MusicTimer();

        if (ListLockActivity.inAdminMode()) {
            Utils.setAuthorized(adminModeBanner);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            final Bundle extras = data.getExtras();
            if (requestCode == SEARCH_REQUEST_CODE) {
                try {
                    SpotifySong newSong = new SpotifySong(extras.getString(KEY_SONG_URI));
                    newSong.setLocked(false);
                    musicService.addSong(newSong);
                    Utils.showTextBriefly(getString(R.string.song_added, newSong.getInfo().getName()), this);
                } catch (SpotifyWebRequestException e) {
                    LogW.e(LOG_TAG, "failed to request song", e);
                    Utils.showTextBriefly(getString(R.string.add_song_failed), this);
                }
            } else if (requestCode == PLAYLIST_REQUEST_CODE) {
                Utils.doWhileLoading(new Utils.Action() {
                    @Override
                    public void execute() {
                        try {
                            PlaylistInfo playlist = (PlaylistInfo) new Playlist(extras.getString(KEY_PLAYLIST_ID), extras.getString(KEY_PLAYLIST_OWNER), UserInfo.getAccessToken()).getInfo();
                            List<SpotifySong> songs = playlist.getSongs();
                            if (songs.isEmpty()) {
                                LogW.d(LOG_TAG, "no songs in playlist " + playlist.getName());
                                Utils.showTextBriefly(getString(R.string.no_songs_in_playlist, playlist.getName()), PlayActivity.this);
                            } else {
                                musicService.clearSongs();
                                MusicService.player().seekToPosition(musicService, 0);
                                updateSeekBar(0, 0);
                                musicService.pause();
                                musicService.addAllSongs(songs);
                            }
                        } catch (SpotifyWebRequestException e) {
                            LogW.e(LOG_TAG, "failed to request playlist", e);
                            Utils.showTextBriefly(getString(R.string.request_playlist_failed), PlayActivity.this);
                        }
                    }
                }, this);
            }

            SavedPreferences.setSongs(musicService.getSongs(), PlayActivity.this);
        }
    }

    public void onClickAddSong(View view) {
        LogW.d(LOG_TAG, "view to add song clicked");
        Intent intent = new Intent(this, SearchActivity.class);
        startActivityForResult(intent, SEARCH_REQUEST_CODE);
    }

    public void onClickAddPlaylist(View view) {
        LogW.d(LOG_TAG, "view to add playlist clicked");

        pinDialog = Utils.doWhenAuthorized(this, new Utils.Action() {
            @Override
            public void execute() {
                Intent intent = new Intent(PlayActivity.this, PlaylistActivity.class);
                startActivityForResult(intent, PLAYLIST_REQUEST_CODE);
            }
        }, adminModeBanner, false);
    }

    public void onClickNext(View view) {
        LogW.d(LOG_TAG, "view to skip clicked");
        pinDialog = Utils.doWhenAuthorized(this, new Utils.Action() {
            @Override
            public void execute() {
                try {
                    musicService.next();
                } catch (MusicServiceException e) {
                    if (e.getExceptionType() == MusicServiceException.ExceptionType.SONG_LIST_EMPTY) {
                        musicService.pause();
                        updateSeekBar(0, 0);
                    } else {
                        LogW.e(LOG_TAG, "failed to skip to next song", e);
                    }
                }
            }
        }, adminModeBanner, false);
    }

    public void onClickPlay(View view) {
        LogW.d(LOG_TAG, "view to play/pause clicked");

        try {
            if (playing) {
                musicService.pause();
            } else {
                musicService.play();
            }
        } catch (MusicServiceException e) {
            switch (e.getExceptionType()) {
                case SONG_LIST_EMPTY:
                    Utils.showTextBriefly(getString(R.string.song_list_empty), this);
                    break;
                case ACCOUNT_NOT_PREMIUM:
                    Utils.showTextProlonged(getString(R.string.not_premium), this);
                    break;
                case NO_TABLE_ATTACHED:
                    LogW.e(LOG_TAG, "no table attached");
                    Utils.showTextBriefly(getString(R.string.play_or_pause_failed), this);
                    break;
            }
        }
    }

    public void onClickSong(final View view) {
        LogW.d(LOG_TAG, "view to delete clicked");

        final SongInfoRow row = (SongInfoRow) view;
        final String title = row.getSongInfo().getName();

        removeSongDialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(getString(R.string.delete_song_conformation, title))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pinDialog = Utils.doWhenAuthorized(PlayActivity.this, new Utils.Action() {
                            @Override
                            public void execute() {
                                try {
                                    if (musicService.removeSongFromTable(((SongInfoRow) view).getSongInfo().getId())) {
                                        Utils.showTextBriefly(getString(R.string.song_removed, title), PlayActivity.this);
                                    } else {
                                        Utils.showTextBriefly(getString(R.string.song_not_in_list, title), PlayActivity.this);
                                    }
                                } catch (MusicServiceException e) {
                                    if (e.getExceptionType() != MusicServiceException.ExceptionType.SONG_LIST_EMPTY) {
                                        LogW.e(LOG_TAG, "failed to remove song " + title);
                                        Utils.showTextBriefly(getString(R.string.song_list_empty), PlayActivity.this);
                                    }
                                }
                            }
                        }, adminModeBanner, false);
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .create();
        removeSongDialog.show();
    }

    public void onClickAdminModeBanner(View view) {
        LogW.d(LOG_TAG, "view to leave admin mode clicked");
        Utils.setUnauthorized(adminModeBanner, this);
    }

    public void setPlaying(final boolean playing) {
        this.playing = playing;
        playPauseButton.post(new Runnable() {
            @Override
            public void run() {
                playPauseButton.setText(playing ? R.string.pause : R.string.play);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogW.d(LOG_TAG, "paused");

        timer.cancel();

        if (musicBound) {
            SavedPreferences.setSongs(musicService.getSongs(), PlayActivity.this);

            unregisterReceiver(playbackEventReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        LogW.d(LOG_TAG, "destroyed");
        unbindService(musicConnection);

        if (pinDialog != null && pinDialog.isShowing()) {
            pinDialog.dismiss();
        }

        if (removeSongDialog != null && removeSongDialog.isShowing()) {
            removeSongDialog.dismiss();
        }

        super.onDestroy();
    }
}

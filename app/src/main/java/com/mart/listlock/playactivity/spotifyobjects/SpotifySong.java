package com.mart.listlock.playactivity.spotifyobjects;

import com.mart.listlock.common.UserInfo;
import com.mart.listlock.playactivity.Song;
import com.mart.listlock.request.SpotifyWebRequest;
import com.mart.listlock.request.SpotifyWebRequestException;
import com.spotify.sdk.android.player.PlayConfig;

public class SpotifySong implements Song, SpotifyObject {

    private PlayConfig config;
    private SongInfo songInfo;
    private boolean locked;

    public SpotifySong(String uri) throws SpotifyWebRequestException {
        config = PlayConfig.createFor(uri);

        songInfo = SpotifyWebRequest.requestSongInfo(uri, UserInfo.getAccessToken());
    }

    public SpotifySong(SongInfo songInfo) {
        this.songInfo = songInfo;
        config = PlayConfig.createFor(songInfo.getUri());
    }

    @Override
    public String getURI() {
        return config.getUris().get(0);
    }

    @Override
    public SongInfo getInfo() {
        return songInfo;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}

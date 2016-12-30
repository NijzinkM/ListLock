package com.mart.listlock.playactivity.spotifyobjects;

import com.mart.listlock.common.UserInfo;
import com.mart.listlock.playactivity.Song;
import com.mart.listlock.request.SpotifyWebRequest;
import com.mart.listlock.request.SpotifyWebRequestException;

public class SpotifySong implements Song, SpotifyObject {

    private SongInfo songInfo;
    private boolean locked;

    public SpotifySong(String uri) throws SpotifyWebRequestException {
        songInfo = SpotifyWebRequest.requestSongInfo(uri, UserInfo.getAccessToken());
    }

    public SpotifySong(SongInfo songInfo) {
        this.songInfo = songInfo;
    }

    @Override
    public SongInfo getInfo() {
        return songInfo;
    }

    @Override
    public String getURI() {
        return songInfo.getUri();
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}

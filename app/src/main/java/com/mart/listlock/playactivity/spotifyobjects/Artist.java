package com.mart.listlock.playactivity.spotifyobjects;

import com.mart.listlock.playactivity.Info;
import com.mart.listlock.request.SpotifyWebRequest;
import com.mart.listlock.common.UserInfo;
import com.mart.listlock.request.SpotifyWebRequestException;

public class Artist implements SpotifyObject {

    private ArtistInfo artistInfo;

    public Artist(String uri) throws SpotifyWebRequestException {
        artistInfo = SpotifyWebRequest.requestArtistInfo(uri, UserInfo.getAccessToken());
    }

    public Artist(ArtistInfo artistInfo) {
        this.artistInfo = artistInfo;
    }

    @Override
    public Info getInfo() {
        return artistInfo;
    }
}

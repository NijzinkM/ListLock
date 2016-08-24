package com.mart.listlock.playactivity.spotifyobjects;

import com.mart.listlock.common.UserInfo;
import com.mart.listlock.playactivity.Info;
import com.mart.listlock.request.SpotifyWebRequest;
import com.mart.listlock.request.SpotifyWebRequestException;

public class Album implements SpotifyObject {

    AlbumInfo albumInfo;

    public Album(String uri) throws SpotifyWebRequestException {
        albumInfo = SpotifyWebRequest.requestAlbumInfo(uri, UserInfo.getAccessToken());
    }

    public Album(AlbumInfo albumInfo) {
        this.albumInfo = albumInfo;
    }

    @Override
    public Info getInfo() {
        return albumInfo;
    }
}

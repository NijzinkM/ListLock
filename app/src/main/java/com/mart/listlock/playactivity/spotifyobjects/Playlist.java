package com.mart.listlock.playactivity.spotifyobjects;

import com.mart.listlock.playactivity.Info;
import com.mart.listlock.request.SpotifyWebRequest;
import com.mart.listlock.request.SpotifyWebRequestException;

public class Playlist implements SpotifyObject {

    PlaylistInfo playlistInfo;

    public Playlist(String id, String accessToken) throws SpotifyWebRequestException {
        playlistInfo = SpotifyWebRequest.requestPlaylistInfo(id, accessToken);
    }

    @Override
    public Info getInfo() {
        return playlistInfo;
    }
}

package com.mart.listlock.playactivity.spotifyobjects;

import com.mart.listlock.playactivity.Info;

import java.util.ArrayList;
import java.util.List;

public class PlaylistInfo extends Info {

    private List<SpotifySong> songs = new ArrayList<>();

    public void override(PlaylistInfo playlistInfo) {
        super.override(playlistInfo);
        songs.addAll(playlistInfo.getSongs());
    }

    public void setSongs(List<SpotifySong> songs) {
        this.songs = songs;
    }

    public List<SpotifySong> getSongs() {
        return songs;
    }

    public void addSong(SpotifySong songInfo) {
        songs.add(songInfo);
    }
}

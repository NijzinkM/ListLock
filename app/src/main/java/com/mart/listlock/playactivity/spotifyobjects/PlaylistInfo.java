package com.mart.listlock.playactivity.spotifyobjects;

import com.mart.listlock.playactivity.Info;

import java.util.ArrayList;
import java.util.List;

public class PlaylistInfo extends Info {

    private List<SpotifySong> songs = new ArrayList<>();
    private String owner;

    public void override(PlaylistInfo playlistInfo) {
        super.override(playlistInfo);
        this.owner = playlistInfo.getOwner();
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}

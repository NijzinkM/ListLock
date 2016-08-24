package com.mart.listlock.playactivity;

public class LocalSong implements Song {

    private String uri;

    public LocalSong(String uri) {
        this.uri = uri;
    }

    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public boolean isLocked() {
        return false;
    }
}

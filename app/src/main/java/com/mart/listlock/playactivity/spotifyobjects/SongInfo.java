package com.mart.listlock.playactivity.spotifyobjects;

import com.mart.listlock.playactivity.Info;

import java.util.List;

public class SongInfo extends Info {

    private List<Artist> artists;
    private long length;
    private Album album;

    public void override(SongInfo songInfo) {
        super.override(songInfo);
        artists = songInfo.getArtists();
        length = songInfo.getLength();
        album = songInfo.getAlbum();
    }

    public void setArtists(List<Artist> artists) {
        this.artists = artists;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getLength() {
        return length;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public Album getAlbum() {
        return album;
    }

    @Override
    public String toString() {
        return "SongInfo{" +
                "id=" + id +
                ", name="+ name +
                ", type=" + type +
                ", uri=" + uri +
                ", artists=" + artists +
                ", length=" + length +
                ", album=" + album +
                '}';
    }
}

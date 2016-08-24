package com.mart.listlock.request;

public class SpotifyWebRequestException extends Exception {

    public SpotifyWebRequestException(Exception exception) {
        super(exception);
    }

    public SpotifyWebRequestException(int httpStatusCode) {
        super("SpotifyWebRequest returned http status code " + httpStatusCode);
    }

    public SpotifyWebRequestException(String s) {
        super(s);
    }
}

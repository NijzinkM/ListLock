package com.mart.listlock.request;


public abstract class DefaultGETResponseHandler extends GETResponseHandler {

    @Override
    protected void handle400(GETResponse response) {
        setException(new SpotifyWebRequestException(response.getHTTPStatusCode()));
    }

    @Override
    protected void handle401(GETResponse response) {
        setException(new SpotifyWebRequestException(response.getHTTPStatusCode()));
    }

    @Override
    protected void handle404(GETResponse response) {
        setException(new SpotifyWebRequestException(response.getHTTPStatusCode()));
    }
}

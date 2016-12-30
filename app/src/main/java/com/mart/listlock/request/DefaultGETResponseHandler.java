package com.mart.listlock.request;


public abstract class DefaultGETResponseHandler extends GETResponseHandler {

    @Override
    protected void handle400(HTTPResponse response) {
        setException(new SpotifyWebRequestException(response.getHTTPStatusCode()));
    }

    @Override
    protected void handle401(HTTPResponse response) {
        setException(new SpotifyWebRequestException(response.getHTTPStatusCode()));
    }

    @Override
    protected void handle404(HTTPResponse response) {
        setException(new SpotifyWebRequestException(response.getHTTPStatusCode()));
    }

    @Override
    protected void handle405(HTTPResponse response) {
        setException(new SpotifyWebRequestException(response.getHTTPStatusCode()));
    }
}

package com.mart.listlock.request;

import android.util.Log;

import com.mart.listlock.common.LogW;

public abstract class GETResponseHandler {

    private static final String LOG_TAG = GETResponseHandler.class.getName();

    SpotifyWebRequestException e;

    public void handleResponse(GETResponse response) throws SpotifyWebRequestException {
        if (response.getException() != null) {
            e = new SpotifyWebRequestException(response.getException());
        }

        final int statusCode = response.getHTTPStatusCode();

        LogW.d(LOG_TAG, "handling http status code " + statusCode);
        
        switch (statusCode) {
            case 200:
                handle200(response);
                break;
            case 400:
                handle400(response);
                break;
            case 401:
                handle401(response);
                break;
            case 404:
                handle404(response);
                break;
        }

        if (e != null)
            throw e;
    }

    protected abstract void handle200(GETResponse response);

    protected abstract void handle400(GETResponse response);

    protected abstract void handle401(GETResponse response);

    protected abstract void handle404(GETResponse response);

    protected void setException(SpotifyWebRequestException e) {
        this.e = e;
    }

    protected SpotifyWebRequestException getException() {
        return e;
    }
}

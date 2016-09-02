package com.mart.listlock.request;

import java.io.IOException;

public class HTTPResponse {

    private String responseText;
    private IOException exception;
    private int HTTPStatusCode;

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public IOException getException() {
        return exception;
    }

    public void setException(IOException exception) {
        this.exception = exception;
    }

    public int getHTTPStatusCode() {
        return HTTPStatusCode;
    }

    public void setHTTPStatusCode(int HTTPStatusCode) {
        this.HTTPStatusCode = HTTPStatusCode;
    }
}

package com.mart.listlock.playactivity;

public class MusicServiceException extends Throwable {

    private ExceptionType exceptionType;

    public MusicServiceException() {
        this(ExceptionType.DEFAULT);
    }

    public MusicServiceException(ExceptionType exceptionType) {
        super();
        this.exceptionType = exceptionType;
    }

    public enum ExceptionType {

        SONG_LIST_EMPTY, ACCOUNT_NOT_PREMIUM, NO_TABLE_ATTACHED, DEFAULT
    }

    public ExceptionType getExceptionType() {
        return exceptionType;
    }
}

package com.sara.backend.exceptions;

public class TooManyStreamsFromAddressException extends RuntimeException {

    public TooManyStreamsFromAddressException() {
        super(null, null, false, false);
    }
}

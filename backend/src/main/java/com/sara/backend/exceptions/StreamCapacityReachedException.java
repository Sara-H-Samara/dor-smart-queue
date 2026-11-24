package com.sara.backend.exceptions;

public class StreamCapacityReachedException extends RuntimeException {

    public StreamCapacityReachedException() {
        super(null, null, false, false);
    }
}

package com.sara.backend.support;

import java.util.List;

public record OpenStreams(
        OpenStreamConnection publicStream,
        OpenStreamConnection studentStream,
        OpenStreamConnection staffStream,
        OpenStreamConnection adminStream) {

    public List<OpenStreamConnection> all() {
        return List.of(publicStream, studentStream, staffStream, adminStream);
    }
}

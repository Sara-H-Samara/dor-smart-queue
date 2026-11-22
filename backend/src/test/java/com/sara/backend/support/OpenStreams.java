package com.sara.backend.support;

public record OpenStreams(
        OpenStreamConnection publicStream,
        OpenStreamConnection studentStream,
        OpenStreamConnection staffStream,
        OpenStreamConnection adminStream) {
}

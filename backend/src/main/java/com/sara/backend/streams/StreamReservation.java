package com.sara.backend.streams;

import java.util.concurrent.atomic.AtomicBoolean;

public class StreamReservation {

    private final Runnable releaseAction;
    private final AtomicBoolean released = new AtomicBoolean(false);

    StreamReservation(Runnable releaseAction) {
        this.releaseAction = releaseAction;
    }

    public void release() {
        if (released.compareAndSet(false, true)) {
            releaseAction.run();
        }
    }
}

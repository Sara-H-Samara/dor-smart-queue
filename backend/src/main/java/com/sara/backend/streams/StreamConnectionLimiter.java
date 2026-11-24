package com.sara.backend.streams;

import com.sara.backend.exceptions.StreamCapacityReachedException;
import com.sara.backend.exceptions.TooManyStreamsFromAddressException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class StreamConnectionLimiter {

    private final int maximumTotalStreams;
    private final int maximumStreamsPerAddress;
    private final Object countLock = new Object();
    private final Map<String, Integer> openStreamCountByAddress = new HashMap<>();
    private int openStreamTotal;

    public StreamConnectionLimiter(
            @Value("${dor.streams.max-total-streams:1000}") int maximumTotalStreams,
            @Value("${dor.streams.max-streams-per-address:100}") int maximumStreamsPerAddress) {
        this.maximumTotalStreams = maximumTotalStreams;
        this.maximumStreamsPerAddress = maximumStreamsPerAddress;
    }

    public StreamReservation reserveSlot(String clientAddress) {
        synchronized (countLock) {
            if (isAddressLimitReached(clientAddress)) {
                throw new TooManyStreamsFromAddressException();
            }
            if (isTotalLimitReached()) {
                throw new StreamCapacityReachedException();
            }
            openStreamCountByAddress.merge(clientAddress, 1, Integer::sum);
            openStreamTotal++;
        }
        return new StreamReservation(() -> releaseSlot(clientAddress));
    }

    int trackedAddressCount() {
        synchronized (countLock) {
            return openStreamCountByAddress.size();
        }
    }

    private void releaseSlot(String clientAddress) {
        synchronized (countLock) {
            openStreamTotal--;
            openStreamCountByAddress.computeIfPresent(clientAddress,
                    (address, openStreamCount) -> openStreamCount == 1 ? null : openStreamCount - 1);
        }
    }

    private boolean isAddressLimitReached(String clientAddress) {
        return openStreamCountByAddress.getOrDefault(clientAddress, 0) >= maximumStreamsPerAddress;
    }

    private boolean isTotalLimitReached() {
        return openStreamTotal >= maximumTotalStreams;
    }
}

package com.sara.backend.support;

import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class SimultaneousRequests {

    private SimultaneousRequests() {
    }

    public static List<MockHttpServletResponse> runAtTheSameTime(List<Callable<MockHttpServletResponse>> requests)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(requests.size());
        CountDownLatch startSignal = new CountDownLatch(1);
        try {
            List<Future<MockHttpServletResponse>> pendingResponses = new ArrayList<>();
            for (Callable<MockHttpServletResponse> request : requests) {
                pendingResponses.add(executor.submit(() -> {
                    startSignal.await();
                    return request.call();
                }));
            }
            startSignal.countDown();
            List<MockHttpServletResponse> responses = new ArrayList<>();
            for (Future<MockHttpServletResponse> pendingResponse : pendingResponses) {
                responses.add(getOrRethrow(pendingResponse));
            }
            return responses;
        } finally {
            executor.shutdownNow();
        }
    }

    public static List<Integer> statusesOf(List<MockHttpServletResponse> responses) {
        return responses.stream().map(MockHttpServletResponse::getStatus).sorted().toList();
    }

    private static MockHttpServletResponse getOrRethrow(Future<MockHttpServletResponse> pendingResponse)
            throws Exception {
        try {
            return pendingResponse.get();
        } catch (ExecutionException failure) {
            throw new AssertionError("A concurrent request failed", failure.getCause());
        }
    }
}

package com.alibaba.csp.sentinel.transport.provider;

import com.alibaba.csp.sentinel.command.CommandResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储请求集合
 * @author Cheney
 * @date 2020/12/22
 */
public class UnprocessedRequests {

    private static final Map<String, CompletableFuture<CommandResponse<String>>> UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public static void put(String requestId, CompletableFuture<CommandResponse<String>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }

    public static void complete(String requestId, CommandResponse<String> response) {
        CompletableFuture<CommandResponse<String>> future = UNPROCESSED_RESPONSE_FUTURES.remove(requestId);
        if (null != future) {
            future.complete(response);
        } else {
            throw new IllegalStateException();
        }
    }
}

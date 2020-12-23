package com.alibaba.csp.sentinel.transport.provider;

import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.CommandResponse;
import com.alibaba.csp.sentinel.transport.CommandCenter;
import com.alibaba.csp.sentinel.transport.enums.CompressTypeEnum;
import com.alibaba.csp.sentinel.transport.enums.SerializationTypeEnum;
import com.alibaba.csp.sentinel.transport.model.Node;
import com.alibaba.csp.sentinel.transport.model.TransportMessage;
import com.alibaba.csp.sentinel.transport.model.TransportRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Transport 提供者
 *
 * @author Cheney
 * @date 2020/12/22
 */
public abstract class TransportProvider {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    public CompletableFuture<CommandResponse<String>> sendRequest(byte messageType, Node node, CommandRequest request) {
        return this.sendRequest(SerializationTypeEnum.DEFAULT.getCode(), CompressTypeEnum.DEFAULT.getCode(), messageType, node, request);
    }

    public CompletableFuture<CommandResponse<String>> sendRequest(byte codec, byte compress, byte messageType, Node node, CommandRequest request) {
        // build return value
        CompletableFuture<CommandResponse<String>> resultFuture = new CompletableFuture<>();
        // get  server address related channel
        Channel channel = getChannel(node);
        if (channel == null) {
            resultFuture.completeExceptionally(new IllegalStateException());
        }
        if (channel.isActive()) {
            TransportRequest<CommandRequest> transportRequest = new TransportRequest<>();
            transportRequest.requestId(UUID.randomUUID().toString());
            transportRequest.data(request);
            // put unprocessed request
            UnprocessedRequests.put(transportRequest.requestId(), resultFuture);
            TransportMessage message = new TransportMessage();
            message.setData(transportRequest);
            message.setCodec(codec);
            message.setCompress(compress);
            message.setMessageType(messageType);
            channel.writeAndFlush(message).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    logger.debug("client send message: [{}]", message);
                } else {
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    logger.warn("Send failed:", future.cause());
                }
            });
        } else {
            resultFuture.completeExceptionally(new IllegalStateException());
        }
        return resultFuture;
    }

    public Channel getChannel(Node node) {
        Channel channel = ChannelProvider.get(node);
        return channel;
    }

}

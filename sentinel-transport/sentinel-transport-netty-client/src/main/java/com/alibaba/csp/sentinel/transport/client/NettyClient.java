package com.alibaba.csp.sentinel.transport.client;

import com.alibaba.csp.sentinel.transport.endpoint.Endpoint;
import com.alibaba.csp.sentinel.transport.netty.AbstractTransport;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Sentinel Netty Client
 *
 * @author Cheney
 * @date 2020/12/22
 */
@SuppressWarnings("rawtypes")
public final class NettyClient extends AbstractTransport<Bootstrap> {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(NettyClient.class);


    private Endpoint endpoint;
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    public static final int CONNECT_TIMEOUT = 5000;
    public static final int RECONNECT_TIMEOUT = 5000;

    public NettyClient(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    protected void init() {
        this.bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //  The timeout period of the connection.
                //  If this time is exceeded or the connection cannot be established, the connection fails.
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
                .handler(new NettyClientInitializer(NettyClient.this));
    }

    @Override
    public Channel connect() throws ExecutionException, InterruptedException {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(endpoint.getHost(), endpoint.getPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                logger.info("The client has connected [{}:{}] successful!", endpoint.getHost(), endpoint.getPort());
                completableFuture.complete(future.channel());
            } else {
                //throw new IllegalStateException();
                logger.error("connect to server error, try again after 5s.");
                Thread.sleep(RECONNECT_TIMEOUT);
                connect();
            }
        });
        this.channel = completableFuture.get();
        return channel;
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close();
        }
        eventLoopGroup.shutdownGracefully();
    }
}

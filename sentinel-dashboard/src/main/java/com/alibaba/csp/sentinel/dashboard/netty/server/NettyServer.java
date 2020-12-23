package com.alibaba.csp.sentinel.dashboard.netty.server;

import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.transport.netty.AbstractTransport;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Sentinel Netty Server
 *
 * @author Cheney
 * @date 2020/12/22
 */
public final class NettyServer extends AbstractTransport<ServerBootstrap> {

    public static final String DEFAULT_IP = "0.0.0.0";
    public static final Integer DEFAULT_PORT = 8619;

    public static final String NETTY_HOST_CONFIG = "csp.sentinel.netty.host";
    public static final String NETTY_PORT_CONFIG = "csp.sentinel.netty.port";

    private String ip;
    private Integer port;


    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    public NettyServer() {
        String host = SentinelConfig.getConfig(NETTY_HOST_CONFIG);
        if (host != null) {
            this.ip = host.trim();
            logger.info("sentinel netty host: {}", ip);
        } else {
            this.ip = DEFAULT_IP;
            logger.warn("sentinel netty host unset, use default host: {}", DEFAULT_IP);
        }
        try {
            String portStr = SentinelConfig.getConfig(NETTY_PORT_CONFIG);
            int tmpPort = Integer.parseInt(portStr);
            if (tmpPort > 0) {
                this.port = tmpPort;
                logger.info("sentinel netty port: {}", port);
            } else {
                this.port = DEFAULT_PORT;
                logger.warn("sentinel netty port error, use default host: {}", DEFAULT_PORT);
            }
        } catch (Exception e) {
            this.port = DEFAULT_PORT;
            logger.warn("sentinel netty port unset, use default host: {}", DEFAULT_PORT);
        }
    }

    public NettyServer(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    protected void init() {
        this.bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                .childOption(ChannelOption.TCP_NODELAY, true)
                // 是否开启 TCP 底层心跳机制
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                .option(ChannelOption.SO_BACKLOG, 128)
                .handler(new LoggingHandler(LogLevel.INFO))
                // 当客户端第一次进行请求的时候才会进行初始化
                .childHandler(new NettyServerInitializer(NettyServer.this));
    }

    @Override
    public Channel connect() throws ExecutionException, InterruptedException {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        // 绑定端口，同步等待绑定成功
        bootstrap.bind(ip, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                logger.info("[NettyHttpCommandCenter] Begin listening at port " + port);
                completableFuture.complete(future.channel());
            } else {
                RecordLog.warn("[HttpServer] Netty server bind error, port={}", port);
                completableFuture.completeExceptionally(future.cause());
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
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}

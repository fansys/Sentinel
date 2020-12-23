package com.alibaba.csp.sentinel.transport.client;

import com.alibaba.csp.sentinel.transport.codec.TransportMessageDecoder;
import com.alibaba.csp.sentinel.transport.codec.TransportMessageEncoder;
import com.alibaba.csp.sentinel.transport.netty.NettyChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * Netty Client ChannelPipeline initial.
 * @author Cheney
 * @date 2020/12/22
 */
public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {

    private NettyClient client;

    public NettyClientInitializer(NettyClient client) {
        this.client = client;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
        p.addLast(new TransportMessageEncoder());
        p.addLast(new TransportMessageDecoder());
        p.addLast(new NettyClientHeartbeatHandler(client));
        p.addLast(new NettyClientHandler(client));
    }
}

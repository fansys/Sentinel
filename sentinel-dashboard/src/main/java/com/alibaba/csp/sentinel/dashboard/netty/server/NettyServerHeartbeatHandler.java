package com.alibaba.csp.sentinel.dashboard.netty.server;

import com.alibaba.csp.sentinel.transport.constant.TransportConstants;
import com.alibaba.csp.sentinel.transport.model.Node;
import com.alibaba.csp.sentinel.transport.provider.ChannelProvider;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server Heartbeat Handler
 * @author Cheney
 * @date 2020/12/22
 */
public class NettyServerHeartbeatHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NettyServerHeartbeatHandler.class);

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Attribute attr = ctx.channel().attr(TransportConstants.TRANSPORT_CLIENT);
        Node node = null;
        if (attr != null) {
            node = (Node) attr.get();
        }
        super.channelInactive(ctx);
        if (node != null) {
            ChannelProvider.remove(node);
            logger.info("channel is removed. ip:{}, port:{}", node.getIp(), node.getPort());
        }
    }
}

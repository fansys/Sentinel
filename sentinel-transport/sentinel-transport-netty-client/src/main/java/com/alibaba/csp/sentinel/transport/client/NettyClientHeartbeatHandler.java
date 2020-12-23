package com.alibaba.csp.sentinel.transport.client;

import com.alibaba.csp.sentinel.Constants;
import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import com.alibaba.csp.sentinel.transport.constant.TransportConstants;
import com.alibaba.csp.sentinel.transport.enums.CompressTypeEnum;
import com.alibaba.csp.sentinel.transport.enums.SerializationTypeEnum;
import com.alibaba.csp.sentinel.transport.model.TransportMessage;
import com.alibaba.csp.sentinel.transport.model.TransportRequest;
import com.alibaba.csp.sentinel.transport.util.HttpCommandUtils;
import com.alibaba.csp.sentinel.util.AppNameUtil;
import com.alibaba.csp.sentinel.util.HostNameUtil;
import com.alibaba.csp.sentinel.util.PidUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Client Heartbeat Handler
 * @author Cheney
 * @date 2020/12/18
 */
public class NettyClientHeartbeatHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NettyClientHeartbeatHandler.class);

    private NettyClient client;

    public NettyClientHeartbeatHandler(NettyClient client) {
        this.client = client;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("client is closed. reconnect...");
        client.connect();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {
                logger.debug("write idle happen [{}]", ctx.channel().remoteAddress());
                Channel channel = client.getChannel();
                TransportMessage message = new TransportMessage();
                message.setCodec(SerializationTypeEnum.DEFAULT.getCode());
                message.setCompress(CompressTypeEnum.DEFAULT.getCode());
                message.setMessageType(TransportConstants.HEARTBEAT_REQUEST_TYPE);
                message.setData(getHeartbeatRequest());
                channel.writeAndFlush(message).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * build heartbeat request
     * @return
     */
    private TransportRequest<CommandRequest> getHeartbeatRequest() {
        CommandRequest request = new CommandRequest();
        request.addMetadata(HttpCommandUtils.REQUEST_TARGET, "/registry/machine");
        request.addParam("app", AppNameUtil.getAppName());
        request.addParam("v", Constants.SENTINEL_VERSION);
        request.addParam("version", String.valueOf(System.currentTimeMillis()));
        request.addParam("hostname", HostNameUtil.getHostName());
        request.addParam("ip", TransportConfig.getHeartbeatClientIp());
        request.addParam("port", "8719");
        request.addParam("pid", String.valueOf(PidUtil.getPid()));
        TransportRequest<CommandRequest> transportRequest = new TransportRequest<>();
        transportRequest.requestId(UUID.randomUUID().toString());
        transportRequest.data(request);
        return transportRequest;
    }
}

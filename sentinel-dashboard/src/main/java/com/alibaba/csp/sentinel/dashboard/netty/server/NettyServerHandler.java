package com.alibaba.csp.sentinel.dashboard.netty.server;

import com.alibaba.csp.sentinel.command.CommandHandler;
import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.CommandResponse;
import com.alibaba.csp.sentinel.transport.constant.TransportConstants;
import com.alibaba.csp.sentinel.transport.log.CommandCenterLog;
import com.alibaba.csp.sentinel.transport.model.Node;
import com.alibaba.csp.sentinel.transport.model.TransportResponse;
import com.alibaba.csp.sentinel.transport.netty.NettyChannelHandler;
import com.alibaba.csp.sentinel.transport.provider.ChannelProvider;
import com.alibaba.csp.sentinel.transport.util.HttpCommandUtils;
import com.alibaba.csp.sentinel.util.StringUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * Netty-based HTTP server handler for command center.
 *
 * @author Cheney
 * @date 2020/12/22
 */
public class NettyServerHandler extends NettyChannelHandler {

    public NettyServerHandler(NettyServer server) {
        super(server);
    }

    /**
     * 处理心跳响应，将心跳成功的channel加入ChannelProvider
     * @param response
     * @param ctx
     */
    @Override
    protected void handlerHeartbeat(CommandResponse response, ChannelHandlerContext ctx) {
        if (response.isSuccess()) {
            Channel channel = ctx.channel();
            Node node = (Node) response.getResult();
            node.setChannel(channel);
            ChannelProvider.set(node);
            channel.attr(TransportConstants.TRANSPORT_CLIENT).set(node);
        }
    }

    @Override
    protected TransportResponse<CommandResponse<String>> handleRequest(CommandRequest request, ChannelHandlerContext ctx) {
        TransportResponse transportResponse = TransportResponse.build()
                .code(OK.code());
        try {
            if (StringUtil.isBlank(HttpCommandUtils.getTarget(request))) {
                return transportResponse.code(BAD_REQUEST.code())
                        .data(CommandResponse.ofFailure(null, "Invalid command"));
            }
            String commandName = HttpCommandUtils.getTarget(request);
            // Find the matching command handler.
            ServerCommandHandler<String> serverCommandHandler = getHandler(commandName);
            if (serverCommandHandler != null) {
                CommandResponse<String> response = serverCommandHandler.handle(request);
                return transportResponse.data(response);
            } else {
                // No matching command handler.
                return transportResponse.code(BAD_REQUEST.code())
                        .data(CommandResponse.ofFailure(null, String.format("Unknown command \"%s\"", commandName)));
            }
        } catch (Exception ex) {
            CommandCenterLog.warn("Internal error", ex);
            return transportResponse.code(INTERNAL_SERVER_ERROR.code())
                    .data(CommandResponse.ofFailure(ex, SERVER_ERROR_MESSAGE));
        }
    }
}

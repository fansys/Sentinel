package com.alibaba.csp.sentinel.transport.client;

import com.alibaba.csp.sentinel.command.CommandHandler;
import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.CommandResponse;
import com.alibaba.csp.sentinel.transport.log.CommandCenterLog;
import com.alibaba.csp.sentinel.transport.model.TransportResponse;
import com.alibaba.csp.sentinel.transport.netty.NettyChannelHandler;
import com.alibaba.csp.sentinel.transport.util.HttpCommandUtils;
import com.alibaba.csp.sentinel.util.StringUtil;
import io.netty.channel.ChannelHandlerContext;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * Netty-based HTTP Client handler for command center.
 *
 * @author Cheney
 * @date 2020/12/22
 */
public class NettyClientHandler extends NettyChannelHandler {

    public NettyClientHandler(NettyClient client) {
        super(client);
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
            CommandHandler<String> commandHandler = getHandler(commandName);
            if (commandHandler != null) {
                CommandResponse<String> response = commandHandler.handle(request);
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

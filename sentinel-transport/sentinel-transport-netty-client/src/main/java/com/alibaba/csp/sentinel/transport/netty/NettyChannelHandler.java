/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.transport.netty;

import com.alibaba.csp.sentinel.command.CommandHandler;
import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.CommandResponse;
import com.alibaba.csp.sentinel.transport.constant.TransportConstants;
import com.alibaba.csp.sentinel.transport.provider.UnprocessedRequests;
import com.alibaba.csp.sentinel.transport.model.TransportMessage;
import com.alibaba.csp.sentinel.transport.model.TransportRequest;
import com.alibaba.csp.sentinel.transport.model.TransportResponse;
import com.alibaba.csp.sentinel.transport.enums.CompressTypeEnum;
import com.alibaba.csp.sentinel.transport.enums.SerializationTypeEnum;
import com.alibaba.csp.sentinel.transport.log.CommandCenterLog;
import com.alibaba.csp.sentinel.transport.util.HttpCommandUtils;
import com.alibaba.csp.sentinel.util.StringUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * Netty-based HTTP server handler for command center.
 * <p>
 * Note: HTTP chunked is not tested!
 *
 * @author Eric Zhao
 */
public abstract class NettyChannelHandler extends SimpleChannelInboundHandler<Object> {

    protected static final String SERVER_ERROR_MESSAGE = "Command server error";

    protected AbstractTransport transport;

    public NettyChannelHandler(AbstractTransport transport) {
        this.transport = transport;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        TransportMessage tmp = (TransportMessage) msg;
        byte messageType = tmp.getMessageType();
        if (messageType == TransportConstants.REQUEST_TYPE) {
            TransportRequest<CommandRequest> transportRequest = (TransportRequest<CommandRequest>) tmp.getData();
            TransportResponse<CommandResponse<String>> transportResponse = handleRequest(transportRequest.data(), ctx);
            transportResponse.requestId(transportRequest.requestId());
            writeResponse(TransportConstants.RESPONSE_TYPE, transportResponse, ctx);
        } else if (messageType == TransportConstants.RESPONSE_TYPE) {
            TransportResponse<CommandResponse> transportResponse = (TransportResponse<CommandResponse>) tmp.getData();
            CommandResponse<String> response = transportResponse.data();
            UnprocessedRequests.complete(transportResponse.requestId(), response);
        } else if (messageType == TransportConstants.HEARTBEAT_REQUEST_TYPE) {
            TransportRequest transportRequest = (TransportRequest) tmp.getData();
            TransportResponse transportResponse = handleRequest(transportRequest.data(), ctx);
            transportResponse.requestId(transportRequest.requestId());
            handlerHeartbeat(transportResponse.data(), ctx);
            writeResponse(TransportConstants.HEARTBEAT_RESPONSE_TYPE, transportResponse, ctx);
        } else if (messageType == TransportConstants.HEARTBEAT_RESPONSE_TYPE) {
            TransportResponse<String> transportResponse = (TransportResponse<String>) tmp.getData();
            CommandResponse<String> response = transportResponse.data();
            //UnprocessedRequests.complete(transportResponse.requestId(), response);
        }
    }

    protected abstract TransportResponse<CommandResponse<String>> handleRequest(CommandRequest request, ChannelHandlerContext ctx);

    protected void writeResponse(byte messageType, TransportResponse response, ChannelHandlerContext ctx) {
        TransportMessage message = new TransportMessage();
        message.setCodec(SerializationTypeEnum.DEFAULT.getCode());
        message.setCompress(CompressTypeEnum.DEFAULT.getCode());
        message.setMessageType(messageType);
        message.setData(response);
        ctx.write(message);
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    protected void handlerHeartbeat(CommandResponse response, ChannelHandlerContext ctx) {
        throw new RuntimeException("not implemented");
    };

    protected <T> T getHandler(String commandName) {
        if (StringUtil.isEmpty(commandName)) {
            return null;
        }
        Map<String, Object> handlerMap = transport.getHandlerMap();
        Object handler = handlerMap.get(commandName);
        return (T) handler;
    }

}

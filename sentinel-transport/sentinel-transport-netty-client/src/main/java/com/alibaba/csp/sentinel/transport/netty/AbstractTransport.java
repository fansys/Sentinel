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
import com.alibaba.csp.sentinel.transport.codec.TransportMessageDecoder;
import com.alibaba.csp.sentinel.transport.codec.TransportMessageEncoder;
import com.alibaba.csp.sentinel.transport.endpoint.Endpoint;
import com.alibaba.csp.sentinel.transport.log.CommandCenterLog;
import com.alibaba.csp.sentinel.util.StringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Eric Zhao
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractTransport<B> {

    protected final Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    protected Channel channel;
    protected B bootstrap;

    protected Map<String, Object> handlerMap = new ConcurrentHashMap();

    /**
     * 启动Transport
     *
     * @throws Exception
     */
    public void start() throws Exception {
        init();
        connect().closeFuture().sync();
    }

    /**
     * 初始化 Bootstrap
     */
    protected abstract void init();

    /**
     * 启动服务
     *
     * @return
     */
    public abstract Channel connect() throws ExecutionException, InterruptedException;

    /**
     * 关闭服务
     */
    public abstract void close();

    /**
     * 注册可执行的命令
     *
     * @param commandName
     * @param handler
     */
    public void registerCommand(String commandName, Object handler) {
        if (StringUtil.isEmpty(commandName) || handler == null) {
            return;
        }

        if (handlerMap.containsKey(commandName)) {
            CommandCenterLog.warn("[NettyHttpCommandCenter] Register failed (duplicate command): " + commandName);
            return;
        }

        handlerMap.put(commandName, handler);
    }

    /**
     * 批量注册可执行的命令
     *
     * @param handlerMap
     */
    public void registerCommands(Map<String, Object> handlerMap) {
        if (handlerMap != null) {
            for (Entry<String, Object> e : handlerMap.entrySet()) {
                registerCommand(e.getKey(), e.getValue());
            }
        }
    }

    /**
     * 获取 Channel
     *
     * @return
     */
    public Channel getChannel() {
        return channel;
    }

    public <T> Map<String, T> getHandlerMap() {
        return (Map<String, T>) handlerMap;
    }
}

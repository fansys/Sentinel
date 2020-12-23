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
package com.alibaba.csp.sentinel.dashboard.netty.server;

import com.alibaba.csp.sentinel.command.annotation.CommandMapping;
import com.alibaba.csp.sentinel.spi.ServiceLoaderUtil;
import com.alibaba.csp.sentinel.util.StringUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Provides and filters command handlers registered via SPI.
 *
 * @author Eric Zhao
 */
public class ServerCommandHandlerProvider implements Iterable<ServerCommandHandler> {

    private final ServiceLoader<ServerCommandHandler> serviceLoader = ServiceLoaderUtil.getServiceLoader(
            ServerCommandHandler.class);

    /**
     * Get all command handlers annotated with {@link CommandMapping} with command name.
     *
     * @return list of all named command handlers
     */
    public Map<String, ServerCommandHandler> namedHandlers() {
        Map<String, ServerCommandHandler> map = new HashMap<>();
        for (ServerCommandHandler handler : serviceLoader) {
            String name = parseCommandName(handler);
            if (!StringUtil.isEmpty(name)) {
                map.put(name, handler);
            }
        }
        return map;
    }

    private String parseCommandName(ServerCommandHandler handler) {
        CommandMapping commandMapping = handler.getClass().getAnnotation(CommandMapping.class);
        if (commandMapping != null) {
            return commandMapping.name();
        } else {
            return null;
        }
    }

    @Override
    public Iterator<ServerCommandHandler> iterator() {
        return serviceLoader.iterator();
    }

    private static final ServerCommandHandlerProvider INSTANCE = new ServerCommandHandlerProvider();

    public static ServerCommandHandlerProvider getInstance() {
        return INSTANCE;
    }
}

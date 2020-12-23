/*
 * Copyright 1999-2019 Alibaba Group Holding Ltd.
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
package com.alibaba.csp.sentinel.dashboard.netty;

import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.transport.CommandCenter;
import com.alibaba.csp.sentinel.transport.ServerCommandCenter;
import com.alibaba.csp.sentinel.util.SpiLoader;

/**
 * Provider for a universal {@link CommandCenter} instance.
 *
 * @author cdfive
 * @since 1.5.0
 */
public final class ServerCommandCenterProvider {

    private static ServerCommandCenter serverCommandCenter = null;

    static {
        resolveInstance();
    }

    private static void resolveInstance() {
        ServerCommandCenter resolveCommandCenter = SpiLoader.loadHighestPriorityInstance(ServerCommandCenter.class);

        if (resolveCommandCenter == null) {
            RecordLog.warn("[CommandCenterProvider] WARN: No existing CommandCenter found");
        } else {
            serverCommandCenter = resolveCommandCenter;
            RecordLog.info("[CommandCenterProvider] CommandCenter resolved: {}", resolveCommandCenter.getClass()
                .getCanonicalName());
        }
    }

    /**
     * Get resolved {@link CommandCenter} instance.
     *
     * @return resolved {@code CommandCenter} instance
     */
    public static ServerCommandCenter getCommandCenter() {
        return serverCommandCenter;
    }

    private ServerCommandCenterProvider() {}
}

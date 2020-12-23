package com.alibaba.csp.sentinel.dashboard.netty;

import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.init.InitOrder;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.transport.ServerCommandCenter;

/**
 * ServerCommandCenter 初始化
 * @author Cheney
 * @date 2020/12/22
 */
@InitOrder(-1)
public class ServerCommandCenterInitFunc implements InitFunc {

    @Override
    public void init() throws Exception {
        ServerCommandCenter serverCommandCenter = ServerCommandCenterProvider.getCommandCenter();

        if (serverCommandCenter == null) {
            RecordLog.warn("[CommandCenterInitFunc] Cannot resolve ServerCommandCenter");
            return;
        }

        serverCommandCenter.beforeStart();
        serverCommandCenter.start();
        RecordLog.info("[CommandCenterInit] Starting server command center: "
                + serverCommandCenter.getClass().getCanonicalName());
    }
}
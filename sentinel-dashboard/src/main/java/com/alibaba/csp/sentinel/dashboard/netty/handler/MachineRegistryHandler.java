package com.alibaba.csp.sentinel.dashboard.netty.handler;

import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.CommandResponse;
import com.alibaba.csp.sentinel.command.annotation.CommandMapping;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineDiscovery;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.netty.server.ServerCommandHandler;
import com.alibaba.csp.sentinel.transport.model.Node;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Title
 *
 * @author zhangcheng.a
 * @date 2020/12/21
 */
@Component
@CommandMapping(name = "/registry/machine", desc = "get cluster client config")
public class MachineRegistryHandler implements ServerCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(MachineRegistryHandler.class);

    private static AppManagement _appManagement;

    @Override
    public CommandResponse handle(CommandRequest request) {
        String app = request.getParam("app");
        if (app == null) {
            app = MachineDiscovery.UNKNOWN_APP_NAME;
        }
        String ip = request.getParam("ip");
        if (ip == null) {
            return CommandResponse.ofFailure(null, "ip can't be null");
        }
        String portStr = request.getParam("port");
        if (portStr == null) {
            return CommandResponse.ofFailure(null, "port can't be null");
        }
        Integer port = Integer.valueOf(portStr);
        if (port == -1) {
            logger.info("Receive heartbeat from " + ip + " but port not set yet");
            return CommandResponse.ofFailure(null, "your port not set yet");
        }
        String v = request.getParam("v");
        String sentinelVersion = StringUtil.isEmpty(v) ? "unknown" : v;
        String versionStr = request.getParam("version");
        Long version = versionStr == null ? System.currentTimeMillis() : Long.valueOf(versionStr);
        Integer appType = Integer.valueOf(request.getParam("app_type", "0"));
        String hostname = request.getParam("hostname");
        try {
            MachineInfo machineInfo = new MachineInfo();
            machineInfo.setApp(app);
            machineInfo.setAppType(appType);
            machineInfo.setHostname(hostname);
            machineInfo.setIp(ip);
            machineInfo.setPort(port);
            machineInfo.setHeartbeatVersion(version);
            machineInfo.setLastHeartbeat(System.currentTimeMillis());
            machineInfo.setVersion(sentinelVersion);
            _appManagement.addMachine(machineInfo);
            return CommandResponse.ofSuccess(new Node(ip, port));
        } catch (Exception e) {
            logger.error("Receive heartbeat error", e);
            return CommandResponse.ofFailure(null, e.getMessage());
        }
    }

    @Resource
    public void setAppManagement(AppManagement appManagement) {
        _appManagement = appManagement;
    }
}

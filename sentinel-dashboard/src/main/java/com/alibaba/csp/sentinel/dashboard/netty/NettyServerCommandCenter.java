package com.alibaba.csp.sentinel.dashboard.netty;

import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.CommandResponse;
import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.dashboard.netty.server.NettyServer;
import com.alibaba.csp.sentinel.dashboard.netty.server.ServerCommandHandlerProvider;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.spi.SpiOrder;
import com.alibaba.csp.sentinel.transport.ServerCommandCenter;
import com.alibaba.csp.sentinel.transport.constant.TransportConstants;
import com.alibaba.csp.sentinel.transport.model.Node;
import com.alibaba.csp.sentinel.transport.provider.TransportProvider;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Netty Server CommandCenter
 *
 * @author Cheney
 * @date 2020/12/22
 */
@SpiOrder(SpiOrder.LOWEST_PRECEDENCE - 100)
public class NettyServerCommandCenter extends TransportProvider implements ServerCommandCenter {

    private final NettyServer server = new NettyServer();

    private static NettyServerCommandCenter serverCommandCenter;

    @SuppressWarnings("PMD.ThreadPoolCreationRule")
    private final ExecutorService pool = Executors.newSingleThreadExecutor(
            new NamedThreadFactory("sentinel-netty-command-center-executor"));

    @Override
    public void start() throws Exception {
        pool.submit(() -> {
            try {
                server.start();
            } catch (Exception ex) {
                RecordLog.warn("[NettyHttpCommandCenter] Failed to start Netty transport server", ex);
                ex.printStackTrace();
            }
        });
    }

    @Override
    public void stop() throws Exception {
        server.close();
        pool.shutdownNow();
    }

    @Override
    public void beforeStart() throws Exception {
        // Register handlers
        Map handlers = ServerCommandHandlerProvider.getInstance().namedHandlers();
        server.registerCommands(handlers);

        serverCommandCenter = this;
    }

    public static CompletableFuture<CommandResponse<String>> sendRequest(Node node, CommandRequest request) {
        return serverCommandCenter.sendRequest(TransportConstants.REQUEST_TYPE, node, request);
    }
}

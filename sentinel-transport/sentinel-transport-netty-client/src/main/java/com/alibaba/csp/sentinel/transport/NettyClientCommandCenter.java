package com.alibaba.csp.sentinel.transport;

import com.alibaba.csp.sentinel.command.CommandHandlerProvider;
import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.CommandResponse;
import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.spi.SpiOrder;
import com.alibaba.csp.sentinel.transport.client.NettyClient;
import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import com.alibaba.csp.sentinel.transport.constant.TransportConstants;
import com.alibaba.csp.sentinel.transport.endpoint.Endpoint;
import com.alibaba.csp.sentinel.transport.model.Node;
import com.alibaba.csp.sentinel.transport.provider.TransportProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Netty Client CommandCenter
 *
 * @author Cheney
 * @date 2020/12/22
 */
@SpiOrder(SpiOrder.LOWEST_PRECEDENCE - 100)
public class NettyClientCommandCenter extends TransportProvider implements CommandCenter {

    private static Set<NettyClient> clientSet = new HashSet<>();

    public static final String THREAD_POOL_NAME = "sentinel-client-command-center-executor";

    private ExecutorService pool;

    private List<Endpoint> consoleServerList;

    @Override
    public void start() throws Exception {
        clientSet.stream().forEach(client -> {
            pool.submit(() -> {
                try {
                    client.start();
                } catch (Exception ex) {
                    RecordLog.warn("[NettyHttpCommandCenter] Failed to start Netty transport server", ex);
                    ex.printStackTrace();
                }
            });
        });

    }

    @Override
    public void stop() throws Exception {
        clientSet.stream().forEach(client -> client.close());
        pool.shutdownNow();
    }

    @Override
    public void beforeStart() throws Exception {
        // 获取consoleServer
        this.consoleServerList = TransportConfig.getConsoleServerList();
        if (consoleServerList.size() == 0) {
            return;
        }

        // Register handlers
        Map handlers = CommandHandlerProvider.getInstance().namedHandlers();
        consoleServerList.stream().forEach(endpoint -> {
            NettyClient client = new NettyClient(endpoint);
            client.registerCommands(handlers);
            clientSet.add(client);
        });

        // 初始化线程池
        pool = new ThreadPoolExecutor(consoleServerList.size(), consoleServerList.size()
                , 5, TimeUnit.SECONDS, new ArrayBlockingQueue(1)
                , new NamedThreadFactory(THREAD_POOL_NAME));
    }

    /**
     * 发送普通请求
     *
     * @param node
     * @param request
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public CompletableFuture<CommandResponse<String>> sendRequest(Node node, CommandRequest request) {
        return super.sendRequest(TransportConstants.REQUEST_TYPE, node, request);
    }

    /**
     * 发送心跳
     *
     * @param node
     * @param request
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public CommandResponse<String> sendHeartbeat(Node node, CommandRequest request) {
        try {
            return super.sendRequest(TransportConstants.HEARTBEAT_REQUEST_TYPE, node, request).get();
        } catch (Exception e) {
            logger.error("send heartbeat to {}:{} error.", node.getIp(), node.getPort());
        }
        return null;
    }
}

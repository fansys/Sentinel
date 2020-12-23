package com.alibaba.csp.sentinel.dashboard.metric;

import com.alibaba.csp.sentinel.Constants;
import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.CommandResponse;
import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppInfo;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.netty.NettyServerCommandCenter;
import com.alibaba.csp.sentinel.dashboard.repository.metric.MetricsRepository;
import com.alibaba.csp.sentinel.node.metric.MetricNode;
import com.alibaba.csp.sentinel.transport.model.Node;
import com.alibaba.csp.sentinel.transport.util.HttpCommandUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Title
 *
 * @author zhangcheng.a
 * @date 2020/12/23
 */
@Component
public class NettyMetricFetcher {

    private static final long MAX_LAST_FETCH_INTERVAL_MS = 1000 * 15;
    private static final long FETCH_INTERVAL_SECOND = 6;
    private final static String METRIC_URL_PATH = "metric";
    private static Logger logger = LoggerFactory.getLogger(NettyMetricFetcher.class);
    private final long intervalSecond = 1;

    private Map<String, AtomicLong> appLastFetchTime = new ConcurrentHashMap<>();

    @Autowired
    private MetricsRepository<MetricEntity> metricStore;
    @Autowired
    private AppManagement appManagement;

    @SuppressWarnings("PMD.ThreadPoolCreationRule")
    private ScheduledExecutorService fetchScheduleService = Executors.newScheduledThreadPool(1,
            new NamedThreadFactory("sentinel-dashboard-metrics-fetch-task"));
    private ExecutorService fetchService;
    private ExecutorService fetchWorker;

    public NettyMetricFetcher() {
        int cores = Runtime.getRuntime().availableProcessors() * 2;
        long keepAliveTime = 0;
        int queueSize = 2048;
        RejectedExecutionHandler handler = new ThreadPoolExecutor.DiscardPolicy();
        fetchService = new ThreadPoolExecutor(cores, cores,
                keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
                new NamedThreadFactory("sentinel-dashboard-metrics-fetchService"), handler);
        fetchWorker = new ThreadPoolExecutor(cores, cores,
                keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
                new NamedThreadFactory("sentinel-dashboard-metrics-fetchWorker"), handler);
        start();
    }

    private void start() {
        fetchScheduleService.scheduleAtFixedRate(() -> {
            try {
                fetchAllApp();
            } catch (Exception e) {
                logger.info("fetchAllApp error:", e);
            }
        }, 10, intervalSecond, TimeUnit.SECONDS);
    }

    private void writeMetric(Map<String, MetricEntity> map) {
        if (map.isEmpty()) {
            return;
        }
        Date date = new Date();
        for (MetricEntity entity : map.values()) {
            entity.setGmtCreate(date);
            entity.setGmtModified(date);
        }
        metricStore.saveAll(map.values());
    }

    /**
     * Traverse each APP, and then pull the metric of all machines for that APP.
     */
    private void fetchAllApp() {
        List<String> apps = appManagement.getAppNames();
        if (apps == null) {
            return;
        }
        for (final String app : apps) {
            fetchService.submit(() -> {
                try {
                    doFetchAppMetric(app);
                } catch (Exception e) {
                    logger.error("fetchAppMetric error", e);
                }
            });
        }
    }

    /**
     * fetch metric between [startTime, endTime], both side inclusive
     */
    private void fetchOnce(String app, long startTime, long endTime, int maxWaitSeconds) {
        if (maxWaitSeconds <= 0) {
            throw new IllegalArgumentException("maxWaitSeconds must > 0, but " + maxWaitSeconds);
        }
        AppInfo appInfo = appManagement.getDetailApp(app);
        // auto remove for app
        if (appInfo.isDead()) {
            logger.info("Dead app removed: {}", app);
            appManagement.removeApp(app);
            return;
        }
        Set<MachineInfo> machines = appInfo.getMachines();
        logger.debug("enter fetchOnce(" + app + "), machines.size()=" + machines.size()
                + ", time intervalMs [" + startTime + ", " + endTime + "]");
        if (machines.isEmpty()) {
            return;
        }
        final String msg = "fetch";
        AtomicLong unhealthy = new AtomicLong();
        final AtomicLong success = new AtomicLong();
        final AtomicLong fail = new AtomicLong();

        long start = System.currentTimeMillis();
        /** app_resource_timeSecond -> metric */
        final Map<String, MetricEntity> metricMap = new ConcurrentHashMap<>(16);
        final CountDownLatch latch = new CountDownLatch(machines.size());
        for (final MachineInfo machine : machines) {
            // auto remove
            if (machine.isDead()) {
                latch.countDown();
                appManagement.getDetailApp(app).removeMachine(machine.getIp(), machine.getPort());
                logger.info("Dead machine removed: {}:{} of {}", machine.getIp(), machine.getPort(), app);
                continue;
            }
            if (!machine.isHealthy()) {
                latch.countDown();
                unhealthy.incrementAndGet();
                continue;
            }
            CompletableFuture<CommandResponse<String>> responseFuture = sendRequest(machine.getIp(), machine.getPort(), startTime, endTime, false);
            responseFuture.thenApply(response -> {
                success.incrementAndGet();
                try {
                    if (response.isSuccess()) {
                        String result = response.getResult();
                        if (StringUtils.isNotEmpty(result)) {
                            String[] lines = result.split("\n");
                            handleBody(lines, machine, metricMap);
                        }
                    } else {
                        logger.error(msg + " metric {}:{} failed. result:{}", response.getResult());
                        fail.incrementAndGet();
                    }
                } catch (Exception e) {
                    logger.error(msg + " metric {}:{} error:", machine.getIp(), machine.getPort(), e);
                } finally {
                    latch.countDown();
                }
                return null;
            });
            responseFuture.exceptionally(ex -> {
                fail.incrementAndGet();
                latch.countDown();
                logger.error(msg + " metric {}:{} error:", machine.getIp(), machine.getPort(), ex);
                return null;
            });
        }
        try {
            latch.await(maxWaitSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.info(msg + " metric, wait http client error:", e);
        }
        long cost = System.currentTimeMillis() - start;
        //logger.info("finished " + msg + " metric for " + app + ", time intervalMs [" + startTime + ", " + endTime
        //    + "], total machines=" + machines.size() + ", dead=" + dead + ", fetch success="
        //    + success + ", fetch fail=" + fail + ", time cost=" + cost + " ms");
        writeMetric(metricMap);
    }

    private CompletableFuture<CommandResponse<String>> sendRequest(String ip, Integer port, long startTime, long endTime, boolean refetch) {
        Node node = new Node(ip, port);
        CommandRequest request = new CommandRequest();
        request.addMetadata(HttpCommandUtils.REQUEST_TARGET, METRIC_URL_PATH);
        request.addParam("startTime", String.valueOf(startTime));
        request.addParam("endTime", String.valueOf(endTime));
        request.addParam("refetch", String.valueOf(refetch));
        return NettyServerCommandCenter.sendRequest(node, request);

    }

    private void doFetchAppMetric(final String app) {
        long now = System.currentTimeMillis();
        long lastFetchMs = now - MAX_LAST_FETCH_INTERVAL_MS;
        if (appLastFetchTime.containsKey(app)) {
            lastFetchMs = Math.max(lastFetchMs, appLastFetchTime.get(app).get() + 1000);
        }
        // trim milliseconds
        lastFetchMs = lastFetchMs / 1000 * 1000;
        long endTime = lastFetchMs + FETCH_INTERVAL_SECOND * 1000;
        if (endTime > now - 1000 * 2) {
            // to near
            return;
        }
        // update last_fetch in advance.
        appLastFetchTime.computeIfAbsent(app, a -> new AtomicLong()).set(endTime);
        final long finalLastFetchMs = lastFetchMs;
        final long finalEndTime = endTime;
        try {
            // do real fetch async
            fetchWorker.submit(() -> {
                try {
                    fetchOnce(app, finalLastFetchMs, finalEndTime, 5);
                } catch (Exception e) {
                    logger.info("fetchOnce(" + app + ") error", e);
                }
            });
        } catch (Exception e) {
            logger.info("submit fetchOnce(" + app + ") fail, intervalMs [" + lastFetchMs + ", " + endTime + "]", e);
        }
    }

    private void handleBody(String[] lines, MachineInfo machine, Map<String, MetricEntity> map) {
        //logger.info("handleBody() lines=" + lines.length + ", machine=" + machine);
        if (lines.length < 1) {
            return;
        }

        for (String line : lines) {
            try {
                MetricNode node = MetricNode.fromThinString(line);
                if (shouldFilterOut(node.getResource())) {
                    continue;
                }
                /*
                 * aggregation metrics by app_resource_timeSecond, ignore ip and port.
                 */
                String key = buildMetricKey(machine.getApp(), node.getResource(), node.getTimestamp());
                MetricEntity entity = map.get(key);
                if (entity != null) {
                    entity.addPassQps(node.getPassQps());
                    entity.addBlockQps(node.getBlockQps());
                    entity.addRtAndSuccessQps(node.getRt(), node.getSuccessQps());
                    entity.addExceptionQps(node.getExceptionQps());
                    entity.addCount(1);
                } else {
                    entity = new MetricEntity();
                    entity.setApp(machine.getApp());
                    entity.setTimestamp(new Date(node.getTimestamp()));
                    entity.setPassQps(node.getPassQps());
                    entity.setBlockQps(node.getBlockQps());
                    entity.setRtAndSuccessQps(node.getRt(), node.getSuccessQps());
                    entity.setExceptionQps(node.getExceptionQps());
                    entity.setCount(1);
                    entity.setResource(node.getResource());
                    map.put(key, entity);
                }
            } catch (Exception e) {
                logger.warn("handleBody line exception, machine: {}, line: {}", machine.toLogString(), line);
            }
        }
    }

    private String buildMetricKey(String app, String resource, long timestamp) {
        return app + "__" + resource + "__" + (timestamp / 1000);
    }

    private boolean shouldFilterOut(String resource) {
        return RES_EXCLUSION_SET.contains(resource);
    }

    private static final Set<String> RES_EXCLUSION_SET = new HashSet<String>() {{
        add(Constants.TOTAL_IN_RESOURCE_NAME);
        add(Constants.SYSTEM_LOAD_RESOURCE_NAME);
        add(Constants.CPU_USAGE_RESOURCE_NAME);
    }};
}

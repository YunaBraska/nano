package berlin.yuna.nano.services.metric.logic;

import berlin.yuna.nano.core.model.Config;
import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.core.model.NanoThread;
import berlin.yuna.nano.core.model.Service;
import berlin.yuna.nano.core.model.Unhandled;
import berlin.yuna.nano.helper.event.model.Event;
import berlin.yuna.nano.helper.logger.logic.LogQueue;
import berlin.yuna.nano.helper.logger.model.LogLevel;
import berlin.yuna.nano.services.http.model.ContentType;
import berlin.yuna.nano.services.http.model.HttpHeaders;
import berlin.yuna.nano.services.http.model.HttpObject;
import berlin.yuna.nano.services.metric.model.MetricCache;
import berlin.yuna.nano.services.metric.model.MetricUpdate;
import berlin.yuna.nano.core.Nano;

import java.io.File;
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static berlin.yuna.nano.helper.event.model.EventType.*;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class MetricService extends Service {
    private final MetricCache metrics = new MetricCache();
    protected String prometheusPath;
    protected String dynamoPath;
    protected String influx;
    protected String wavefront;

    public MetricService() {
        super(null, false);
    }

    @Override
    public void start(final Supplier<Context> contextSupplier) {
        AtomicReference<Optional<String>> basePath = new AtomicReference<>(Optional.empty());
        isReady.set(false, true, run -> {
            updateSystemMetrics();
            basePath.set(Optional.ofNullable(contextSupplier.get().get(String.class, Config.CONFIG_METRIC_SERVICE_BASE_PATH.id())).or(() -> Optional.of("/metrics")));
        });

        prometheusPath = contextSupplier.get().getOpt(String.class, Config.CONFIG_METRIC_SERVICE_PROMETHEUS_PATH.id()).orElseGet(() -> basePath.get().map(base -> base + "/prometheus").orElse(null));
        dynamoPath = contextSupplier.get().getOpt(String.class, Config.CONFIG_METRIC_SERVICE_DYNAMO_PATH.id()).orElseGet(() -> basePath.get().map(base -> base + "/dynamo").orElse(null));
        influx = contextSupplier.get().getOpt(String.class, Config.CONFIG_METRIC_SERVICE_INFLUX_PATH.id()).orElseGet(() -> basePath.get().map(base -> base + "/influx").orElse(null));
        wavefront = contextSupplier.get().getOpt(String.class, Config.CONFIG_METRIC_SERVICE_WAVEFRONT_PATH.id()).orElseGet(() -> basePath.get().map(base -> base + "/wavefront").orElse(null));
    }

    @Override
    public void stop(final Supplier<Context> contextSupplier) {
        isReady.set(true, false, run -> {
            metrics.gauges().clear();
            metrics.timers().clear();
            metrics.counters().clear();
        });
        //remove listener
    }

    @Override
    public Object onFailure(final Unhandled error) {
        return null;
    }

    @Override
    public void onEvent(final Event event) {
        super.onEvent(event);
        event
            .ifPresentAck(EVENT_APP_HEARTBEAT, Nano.class, this::updateMetrics)
            .ifPresentAck(EVENT_METRIC_UPDATE, MetricUpdate.class, this::updateMetric)
            .ifPresent(EVENT_APP_LOG_LEVEL, LogLevel.class, level -> {
                Arrays.stream(LogLevel.values()).filter(other -> other != level).forEach(other -> metrics.gaugeSet("logger", 0, Map.of("level", other.name())));
                metrics.gaugeSet("logger", 1, Map.of("level", level.name()));
            })
            .ifPresent(EVENT_APP_LOG_QUEUE, LogQueue.class, logger::logQueue);
        addMetricsEndpoint(event);

    }

    protected void addMetricsEndpoint(Event event) {
        event
            .ifPresent(EVENT_HTTP_REQUEST, HttpObject.class, request ->
                Optional.ofNullable(prometheusPath)
                    .filter(request::pathMatch)
                    .filter(path -> request.isMethodGet())
                    .ifPresent(path ->
                        request.response()
                            .statusCode(200)
                            .body(metrics.prometheus())
                            .headerMap(Map.of(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_PLAIN)).send(event)
                    )
            )
            .ifPresent(EVENT_HTTP_REQUEST, HttpObject.class, request ->
                Optional.ofNullable(dynamoPath)
                    .filter(request::pathMatch)
                    .filter(path -> request.isMethodGet())
                    .ifPresent(path ->
                        request.response()
                            .statusCode(200)
                            .body(metrics.dynatrace())
                            .headerMap(Map.of(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_PLAIN)).send(event)
                    )
            )
            .ifPresent(EVENT_HTTP_REQUEST, HttpObject.class, request ->
                Optional.ofNullable(influx)
                    .filter(request::pathMatch)
                    .filter(path -> request.isMethodGet())
                    .ifPresent(path ->
                        request.response()
                            .statusCode(200)
                            .body(metrics.influx())
                            .headerMap(Map.of(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_PLAIN)).send(event)
                    )
            )
            .ifPresent(EVENT_HTTP_REQUEST, HttpObject.class, request ->
                Optional.ofNullable(wavefront)
                    .filter(request::pathMatch)
                    .filter(path -> request.isMethodGet())
                    .ifPresent(path ->
                        request.response()
                            .statusCode(200)
                            .body(metrics.wavefront())
                            .headerMap(Map.of(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_PLAIN)).send(event)
                    )
            );
    }

    public void updateMetric(final MetricUpdate metric) {
        switch (metric.type()) {
            case GAUGE -> metrics.gaugeSet(metric.name(), metric.value().doubleValue(), metric.tags());
            case COUNTER -> metrics.counterIncrement(metric.name(), metric.tags());
            case TIMER_START -> metrics.timerStart(metric.name(), metric.tags());
            case TIMER_END -> metrics.timerStop(metric.name(), metric.tags());
        }
    }

    public MetricCache metrics() {
        return metrics;
    }

    public MetricService updateMetrics(final Nano nano) {
        updateNanoMetrics(nano);
        updateCpuMetrics();
        updateDiscMetrics();
        updateMemoryMetrics();
        updatePoolMetrics();
        updateThreadMetrics();
        updateBufferMetrics();
        updateClassLoaderMetrics();
        updateCompilerMetrics();
        Context.tryExecute(() -> {
            metrics.gaugeSet("service.metrics.gauges", metrics.gauges().size());
            metrics.gaugeSet("service.metrics.timers", metrics.timers().size());
            metrics.gaugeSet("service.metrics.counters", metrics.counters().size());
            metrics.gaugeSet("service.metrics.bytes", estimateMetricCacheSize());
        });
        return this;
    }

    public void updateCompilerMetrics() {
        Context.tryExecute(() -> {
            final CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
            if (compilationMXBean.isCompilationTimeMonitoringSupported()) {
                metrics.gaugeSet("jvm.compilation.time.ms", compilationMXBean.getTotalCompilationTime());
            }
        });
    }

    public void updateClassLoaderMetrics() {
        Context.tryExecute(() -> {
            final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
            metrics.gaugeSet("jvm.classes.loaded", classLoadingMXBean.getLoadedClassCount());
            metrics.gaugeSet("jvm.classes.unloaded", classLoadingMXBean.getUnloadedClassCount());
        });
    }

    public void updateBufferMetrics() {
        Context.tryExecute(() -> {
            final String suffix = ".bytes";
            for (final BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
                final Map<String, String> tags = new HashMap<>();
                tags.put("id", pool.getName());
                metrics.gaugeSet("jvm.buffer.count." + pool.getName() + suffix, pool.getCount(), tags);
                metrics.gaugeSet("jvm.buffer.memory.used." + pool.getName() + suffix, pool.getMemoryUsed(), tags);
                metrics.gaugeSet("jvm.buffer.total.capacity." + pool.getName() + suffix, pool.getTotalCapacity(), tags);
            }
        });
    }

    public void updateThreadMetrics() {
        Context.tryExecute(() -> {
            metrics.gaugeSet("jvm.threads.live", Thread.activeCount());
            metrics.gaugeSet("jvm.threads.nano", NanoThread.activeNanoThreads());
            metrics.gaugeSet("jvm.threads.carrier", NanoThread.activeCarrierThreads());
        });
        Context.tryExecute(() -> {
            final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            metrics.gaugeSet("jvm.threads.daemon", threadMXBean.getDaemonThreadCount());
            metrics.gaugeSet("jvm.threads.live", threadMXBean.getThreadCount());
            metrics.gaugeSet("jvm.threads.peak", threadMXBean.getPeakThreadCount());

            Arrays.stream(threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(ThreadInfo::getThreadState, Collectors.counting()))
                .forEach((state, count) -> metrics.gaugeSet("jvm.threads.states", count, Map.of("state", state.toString().toLowerCase())));
        });
    }

    public void updatePoolMetrics() {
        Context.tryExecute(() -> {
            final List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
            for (final MemoryPoolMXBean pool : pools) {
                final String area = pool.getType() == MemoryType.HEAP ? "heap" : "nonheap";
                final MemoryUsage usage = pool.getUsage();

                metrics.gaugeSet("jvm.memory.max.bytes", usage.getMax(), Map.of("area", area, "id", pool.getName()));
                metrics.gaugeSet("jvm.memory.used.bytes", usage.getUsed(), Map.of("area", area, "id", pool.getName()));
                metrics.gaugeSet("jvm.memory.committed.bytes", usage.getCommitted(), Map.of("area", area, "id", pool.getName()));
            }
        });
    }

    public void updateMemoryMetrics() {
        Context.tryExecute(() -> {
            final Runtime runtime = Runtime.getRuntime();
            metrics.gaugeSet("system.cpu.cores", runtime.availableProcessors());
            metrics.gaugeSet("jvm.memory.max.bytes", runtime.maxMemory());
            metrics.gaugeSet("jvm.memory.used.bytes", (double) runtime.totalMemory() - runtime.freeMemory());
        });
    }

    public void updateDiscMetrics() {
        Context.tryExecute(() -> {
            final File disk = new File("/");
            metrics.gaugeSet("disk.free.bytes", disk.getFreeSpace());
            metrics.gaugeSet("disk.total.bytes", disk.getTotalSpace());
        });
    }

    public void updateCpuMetrics() {
        Context.tryExecute(() -> {
            final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
            if (osMXBean instanceof final com.sun.management.OperatingSystemMXBean sunOsMXBean) {
                metrics.gaugeSet("process.cpu.usage", sunOsMXBean.getProcessCpuLoad());
                metrics.gaugeSet("system.cpu.usage", sunOsMXBean.getCpuLoad());
            }
            metrics.gaugeSet("system.load.average.1m", osMXBean.getSystemLoadAverage());
        });
    }

    public void updateNanoMetrics(final Nano nano) {
        Context.tryExecute(() -> {
            nano.services().stream()
                .collect(Collectors.groupingBy(service -> service.getClass().getSimpleName(), Collectors.counting()))
                .forEach((className, count) -> metrics.gaugeSet("application.services", count, Map.of("class", className)));
            metrics.gaugeSet("application.schedulers", nano.schedulers().size());
            metrics.gaugeSet("application.listeners", nano.listeners().size());
        });
    }

    public void updateSystemMetrics() {
        final String numberRegex = "\\D";
        metrics.gaugeSet("application.pid", ProcessHandle.current().pid());
        updateJavaVersion();
        updateArch();
        updateOs();
        Context.tryExecute(() -> metrics.gaugeSet("system.version", Double.parseDouble(System.getProperty("os.version").replaceAll(numberRegex, ""))));
    }

    public void updateOs() {
        Context.tryExecute(() -> {
            String osName = System.getProperty("os.name");
            osName = osName == null ? "" : osName.toLowerCase();
            final List<String> osPrefixes = List.of("linux", "mac", "windows", "aix", "irix", "hp-ux", "os/400", "freebsd", "openbsd", "netbsd", "os/2", "solaris", "sunos", "mips", "z/os");
            final List<String> unix = List.of("linux", "mac", "aix", "irix", "hp-ux", "freebsd", "openbsd", "netbsd", "solaris", "sunos");

            final String finalOsName = osName;
            metrics.gaugeSet("system.type", IntStream.range(0, osPrefixes.size()).filter(i -> finalOsName.startsWith(osPrefixes.get(i))).findFirst().orElse(-1) + 1d);
            metrics.gaugeSet("system.unix", unix.stream().anyMatch(finalOsName::startsWith) ? 1 : 0);
        });
    }

    public void updateArch() {
        Context.tryExecute(() -> {
            final String metricName = "system.arch.bit";
            String arch = System.getProperty("os.arch");
            arch = arch == null ? "" : arch.toLowerCase();
            if (arch.contains("64")) {
                metrics.gaugeSet(metricName, 64);
            } else if (Stream.of("x86", "686", "386", "368").anyMatch(arch::contains)) {
                metrics.gaugeSet(metricName, 32);
            } else {
                final String number = arch.replaceAll("\\D", "");
                metrics.gaugeSet(metricName, number.isEmpty() ? 0 : Double.parseDouble(number));
            }
        });
    }

    public void updateJavaVersion() {
        Context.tryExecute(() -> {
            String version = System.getProperty("java.version");
            version = version.startsWith("1.") ? version.substring(2) : version;
            version = version.contains(".") ? version.substring(0, version.indexOf(".")) : version;
            metrics.gaugeSet("java.version", Double.parseDouble(version.replaceAll("\\D", "")));
        });
    }

    public long estimateMetricCacheSize() {
        long totalSize = 0;
        // Calculate size for counters, gauges, and timers
        totalSize += estimateMapSize(new HashMap<>(metrics.counters()), 28) +
            estimateMapSize(new HashMap<>(metrics.gauges()), 24) +
            estimateMapSize(new HashMap<>(metrics.timers()), 16);

        return totalSize;
    }

    private long estimateMapSize(final Map<String, MetricCache.Metric<?>> map, final long numberSize) {
        long size = 36L;
        for (final Map.Entry<String, MetricCache.Metric<?>> entry : map.entrySet()) {
            size += 32L + // Entry overhead
                estimateStringSize(entry.getKey()) + // Key size
                estimateMetricSize(entry.getValue(), numberSize); // Value size
        }
        return size;
    }

    private long estimateMetricSize(final MetricCache.Metric<?> metric, final long numberSize) {
        long size = 48; // TreeMap overhead for tags
        size += estimateStringSize(metric.metricName()); // Metric name size
        size += numberSize; // Number size (AtomicLong, Double, Long)
        for (final Map.Entry<String, String> tag : metric.tags().entrySet()) {
            size += estimateStringSize(tag.getKey()) + estimateStringSize(tag.getValue()); // Tag key-value sizes
        }
        return size;
    }

    private long estimateStringSize(final String string) {
        return 24 + (long) string.length() * 2; // String object overhead + 2 bytes per character
    }

    public String prometheusPath() {
        return prometheusPath;
    }

    public String dynamoPath() {
        return dynamoPath;
    }

    public String influx() {
        return influx;
    }

    public String wavefront() {
        return wavefront;
    }
}

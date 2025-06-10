package com.sandeep.api.config;

import com.sandeep.api.util.TestRunIdUtil;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.exporter.PushGateway;
import io.prometheus.client.hotspot.DefaultExports;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.testng.ISuite;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class PrometheusTestMetrics {
    // Use Prometheus-compliant label names (no dots, only [a-zA-Z_][a-zA-Z0-9_]* )
    private static final String LABEL_TEST_SUITE = "test_suite";
    private static final String LABEL_AUT = "aut";
    private static final String LABEL_TEST_RUN_ID = "test_run_id";
    private static final String LABEL_TEST_CLASS = "test_class";
    private static final String LABEL_TEST_NAME = "test_name";
    private static final String LABEL_STATUS = "test_status";
    private static final String UNKNOWN = "unknown";
    private static final String ENV_TEST_RUN_ID = "test_run_id";

    private PrometheusTestMetrics() { /* Utility class */ }

    private static final Counter testResultCounter = Counter.build()
            .name("test_result_total")
            .help("Total number of test results by status.")
            .labelNames(LABEL_TEST_SUITE, LABEL_AUT, LABEL_TEST_RUN_ID, LABEL_TEST_CLASS, LABEL_TEST_NAME, LABEL_STATUS)
            .register();
    private static final Histogram testDurationHistogram = Histogram.build()
            .name("test_duration_seconds")
            .help("Test execution duration in seconds.")
            .labelNames(LABEL_TEST_SUITE, LABEL_AUT, LABEL_TEST_RUN_ID, LABEL_TEST_CLASS, LABEL_TEST_NAME, LABEL_STATUS)
            .register();
    private static final AtomicReference<HTTPServer> prometheusServer = new AtomicReference<>();
    private static final Object serverLock = new Object();

    public static void startServerIfNeeded() {
        if (prometheusServer.get() == null) {
            synchronized (serverLock) {
                if (prometheusServer.get() == null) {
                    try {
                        DefaultExports.initialize();
                        prometheusServer.set(new HTTPServer(8081));
                    } catch (Exception e) {
                        log.error("Failed to start Prometheus HTTPServer", e);
                    }
                }
            }
        }
    }

    public static void recordTestResult(String suite, String aut, String testRunId, String className, String testName, String status, double durationSeconds) {
        log.debug("PrometheusTestMetrics.recordTestResult: suite={}, aut={}, testRunId={}, className={}, testName={}, status={}, durationSeconds={}", suite, aut, testRunId, className, testName, status, durationSeconds);
        testResultCounter.labels(suite, aut, testRunId, className, testName, status).inc();
        testDurationHistogram.labels(suite, aut, testRunId, className, testName, status).observe(durationSeconds);
    }

    public static String extractAut(ISuite suite) {
        String aut = System.getenv("AUT");
        if (StringUtils.isBlank(aut)) {
            aut = System.getProperty("aut");
        }
        if (StringUtils.isBlank(aut) && suite.getAttribute("aut") != null) {
            aut = suite.getAttribute("aut").toString();
        }
        return (StringUtils.isNotBlank(aut)) ? aut : UNKNOWN;
    }

    public static String extractTestRunId(ISuite suite) {
        return TestRunIdUtil.resolveTestRunId(suite);
    }

    public static void pushMetricsToGateway() {
        String gatewayAddress = System.getenv().getOrDefault("PUSHGATEWAY_ADDRESS", "localhost:9091");
        PushGateway pushGateway = new PushGateway(gatewayAddress);
        try {
            // Use a grouping key for uniqueness (e.g., test_run_id, aut, suite)
            java.util.Map<String, String> groupingKey = new java.util.HashMap<>();
            groupingKey.put(ENV_TEST_RUN_ID, System.getenv().getOrDefault("TEST_RUN_ID", UNKNOWN));
            groupingKey.put("aut", System.getenv().getOrDefault("AUT", UNKNOWN));
            groupingKey.put("suite", System.getenv().getOrDefault("SUITE", UNKNOWN));
            pushGateway.pushAdd(io.prometheus.client.CollectorRegistry.defaultRegistry, "api_test_automation", groupingKey);
        } catch (IOException e) {
            log.error("Failed to push metrics to Pushgateway", e);
        }
    }
}

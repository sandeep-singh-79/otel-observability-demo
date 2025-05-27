package com.sandeep.api.listeners;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import lombok.extern.slf4j.Slf4j;
import org.testng.ISuite;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class PrometheusTestMetrics {
    private static final String LABEL_SUITE = "suite";
    private static final String LABEL_AUT = "aut";
    private static final String LABEL_TEST_RUN_ID = "test_run_id";
    private static final String LABEL_TEST_CLASS = "test_class";
    private static final String LABEL_TEST_NAME = "test_name";
    private static final String LABEL_STATUS = "status";

    private PrometheusTestMetrics() { /* Utility class */ }

    private static final Counter testResultCounter = Counter.build()
            .name("test_result_total")
            .help("Total number of test results by status.")
            .labelNames(LABEL_SUITE, LABEL_AUT, LABEL_TEST_RUN_ID, LABEL_TEST_CLASS, LABEL_TEST_NAME, LABEL_STATUS)
            .register();
    private static final Histogram testDurationHistogram = Histogram.build()
            .name("test_duration_seconds")
            .help("Test execution duration in seconds.")
            .labelNames(LABEL_SUITE, LABEL_AUT, LABEL_TEST_RUN_ID, LABEL_TEST_CLASS, LABEL_TEST_NAME, LABEL_STATUS)
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
        testResultCounter.labels(suite, aut, testRunId, className, testName, status).inc();
        testDurationHistogram.labels(suite, aut, testRunId, className, testName, status).observe(durationSeconds);
    }

    public static String extractAut(ISuite suite) {
        String aut = System.getenv("AUT");
        if (aut == null || aut.isEmpty()) {
            aut = System.getProperty("aut");
        }
        if ((aut == null || aut.isEmpty()) && suite.getAttribute("aut") != null) {
            aut = suite.getAttribute("aut").toString();
        }
        return (aut != null && !aut.isEmpty()) ? aut : "unknown";
    }

    public static String extractTestRunId(ISuite suite) {
        String testRunId = System.getenv("TEST_RUN_ID");
        if (testRunId == null || testRunId.isEmpty()) {
            testRunId = System.getProperty("test_run_id");
        }
        if ((testRunId == null || testRunId.isEmpty()) && suite.getAttribute("test_run_id") != null) {
            testRunId = suite.getAttribute("test_run_id").toString();
        }
        return (testRunId != null && !testRunId.isEmpty()) ? testRunId : "unknown";
    }
}

package com.sandeep.api.listeners;


import com.sandeep.api.config.OpenTelemetryConfig;
import com.sandeep.api.config.PrometheusTestMetrics;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import lombok.extern.slf4j.Slf4j;
import org.testng.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class OpenTelemetryTestListener implements ISuiteListener, IInvokedMethodListener {

    private OpenTelemetrySdk openTelemetrySdk;
    private Tracer tracer;
    private final Map<String, Span> suiteSpans = new ConcurrentHashMap<>();

    private String currentSuiteName;
    private String currentAut;
    private String currentTestRunId;

    @Override
    public void onStart (ISuite suite) {
        openTelemetrySdk = (OpenTelemetrySdk) OpenTelemetryConfig.getOpenTelemetry();
        tracer = openTelemetrySdk.getTracer("api-tests");

        Span suiteSpan = tracer.spanBuilder("suite: " + suite.getName())
                             .setAttribute("suite.name", suite.getName())
                             .startSpan();

        suiteSpans.put(suite.getName(), suiteSpan);
        suiteSpan.makeCurrent();

        log.info("✅ OpenTelemetry initialized and suite span started for: {}", suite.getName());

        // Start Prometheus HTTPServer only once (delegated)
        PrometheusTestMetrics.startServerIfNeeded();

        // Extract suite, aut, test_run_id using helper
        currentSuiteName = suite.getName();
        currentAut = PrometheusTestMetrics.extractAut(suite);
        currentTestRunId = PrometheusTestMetrics.extractTestRunId(suite);
    }

    @Override
    public void onFinish (ISuite suite) {
        Span suiteSpan = suiteSpans.remove(suite.getName());
        if (suiteSpan != null) {
            suiteSpan.setAttribute("suite.status", "finished");
            suiteSpan.end();
        }

        if (openTelemetrySdk != null) {
            openTelemetrySdk.getSdkTracerProvider().shutdown();
            log.info("🧹 OpenTelemetry tracer provider shutdown after suite finish");
        }

        // Push metrics to Pushgateway for short-lived jobs
        PrometheusTestMetrics.pushMetricsToGateway();
    }

    @Override
    public void beforeInvocation (IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            String testName = method.getTestMethod().getMethodName();
            String className = method.getTestMethod().getRealClass().getSimpleName();
            String spanName = className + "." + testName;

            Span span = tracer.spanBuilder(spanName)
                            .setAttribute("test.name", testName)
                            .setAttribute("test.class", className)
                            .setAttribute("test.description", method.getTestMethod().getDescription())
                            .setAttribute("test.run_id", String.valueOf(System.currentTimeMillis()))
                            .startSpan();

            Scope scope = span.makeCurrent();
            testResult.setAttribute("currentSpan", span);
            testResult.setAttribute("currentScope", scope);
        }
    }

    @Override
    public void afterInvocation (IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            Span span = (Span) testResult.getAttribute("currentSpan");
            Scope scope = (Scope) testResult.getAttribute("currentScope");
            long startTime = testResult.getStartMillis();
            long endTime = testResult.getEndMillis();
            double durationSeconds = (endTime - startTime) / 1000.0;

            String testName = method.getTestMethod().getMethodName();
            String className = method.getTestMethod().getRealClass().getSimpleName();
            String status = switch (testResult.getStatus()) {
                case ITestResult.SUCCESS -> "pass";
                case ITestResult.FAILURE -> "fail";
                case ITestResult.SKIP -> "skipped";
                default -> "unknown";
            };

            if (span != null) {
                span.setAttribute("test.status", status);
                if (testResult.getThrowable() != null) {
                    span.recordException(testResult.getThrowable());
                    span.setStatus(StatusCode.ERROR, testResult.getThrowable().getMessage());
                } else {
                    span.setStatus(StatusCode.OK);
                }
                span.addEvent("Test execution completed");
                span.end();
            }

            // Delegate Prometheus metrics
            PrometheusTestMetrics.recordTestResult(currentSuiteName, currentAut, currentTestRunId, className, testName, status, durationSeconds);

            if (scope != null) {
                scope.close();
            }
        }
    }
}
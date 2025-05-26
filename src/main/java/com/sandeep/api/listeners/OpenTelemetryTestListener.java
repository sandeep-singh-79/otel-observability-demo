package com.sandeep.api.listeners;


import com.sandeep.api.util.OpenTelemetryConfig;
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

    @Override
    public void onStart(ISuite suite) {
        openTelemetrySdk = (OpenTelemetrySdk) OpenTelemetryConfig.getOpenTelemetry();
        tracer = openTelemetrySdk.getTracer("api-tests");

        Span suiteSpan = tracer.spanBuilder("suite: " + suite.getName())
                             .setAttribute("suite.name", suite.getName())
                             .startSpan();

        suiteSpans.put(suite.getName(), suiteSpan);
        suiteSpan.makeCurrent();

        log.info("✅ OpenTelemetry initialized and suite span started for: {}", suite.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        Span suiteSpan = suiteSpans.remove(suite.getName());
        if (suiteSpan != null) {
            suiteSpan.setAttribute("suite.status", "finished");
            suiteSpan.end();
        }

        if (openTelemetrySdk != null) {
            openTelemetrySdk.getSdkTracerProvider().shutdown();
            log.info("🧹 OpenTelemetry tracer provider shutdown after suite finish");
        }
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
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
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            Span span = (Span) testResult.getAttribute("currentSpan");
            Scope scope = (Scope) testResult.getAttribute("currentScope");

            if (span != null) {
                String status = switch (testResult.getStatus()) {
                    case ITestResult.SUCCESS -> "pass";
                    case ITestResult.FAILURE -> "fail";
                    case ITestResult.SKIP -> "skipped";
                    default -> "unknown";
                };
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

            if (scope != null) {
                scope.close();
            }
        }
    }
}
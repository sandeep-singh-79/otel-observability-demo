package com.sandeep.api.listeners;


import com.sandeep.api.util.OpenTelemetryConfig;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import lombok.extern.slf4j.Slf4j;
import org.testng.*;

@Slf4j
public class OpenTelemetryTestListener implements ISuiteListener, IInvokedMethodListener {

    private OpenTelemetrySdk openTelemetrySdk;
    private Tracer tracer;

    @Override
    public void onStart (ISuite suite) {
        tracer = OpenTelemetryConfig.getOpenTelemetry().getTracer("api-tests");

        System.out.println("✅ OpenTelemetry initialized at suite start");
    }

    @Override
    public void onFinish (ISuite suite) {
        if (openTelemetrySdk != null) {
            openTelemetrySdk.getSdkTracerProvider().shutdown();
            System.out.println("🧹 OpenTelemetry tracer provider shutdown after suite finish");
        }
    }

    @Override
    public void beforeInvocation (IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            String testName = method.getTestMethod().getMethodName();
            Span span = tracer.spanBuilder(testName)
                            .setAttribute("test.name", testName)
                            .setAttribute("test.class", method.getTestMethod().getRealClass().getSimpleName())
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

            if (span != null) {
                if (testResult.getStatus() == ITestResult.FAILURE) {
                    span.setAttribute("test.status", "fail");
                    span.recordException(testResult.getThrowable());
                } else {
                    span.setAttribute("test.status", "pass");
                }
                span.end();
            }
            if (scope != null) {
                scope.close();
            }
        }
    }
}
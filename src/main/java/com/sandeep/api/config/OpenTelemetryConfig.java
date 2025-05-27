package com.sandeep.api.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import java.util.UUID;

public class OpenTelemetryConfig {

    private OpenTelemetryConfig() {
        // Prevent instantiation
    }

    /**
     * Singleton holder class idiom to ensure thread-safe lazy initialization
     */
    private static class Holder {
        private static final OpenTelemetry openTelemetry = createOpenTelemetry();

        private static OpenTelemetry createOpenTelemetry() {
            Resource serviceNameResource = Resource.getDefault().toBuilder()
                                               .put(ResourceAttributes.SERVICE_NAME, "api-test-automation")
                                               .put(ResourceAttributes.SERVICE_INSTANCE_ID, UUID.randomUUID().toString())
                                               .put("environment", System.getProperty("env", "dev"))
                                               .build();

            OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                                                    .setEndpoint("http://localhost:4317")
                                                    .build();

            SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                                                   .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                                                   .setResource(Resource.getDefault().merge(serviceNameResource))
                                                   .build();

            return OpenTelemetrySdk.builder()
                       .setTracerProvider(tracerProvider)
                       .buildAndRegisterGlobal();
        }
    }

    /**
     * Public accessor for the singleton OpenTelemetry instance
     */
    public static OpenTelemetry getOpenTelemetry() {
        return Holder.openTelemetry;
    }
}


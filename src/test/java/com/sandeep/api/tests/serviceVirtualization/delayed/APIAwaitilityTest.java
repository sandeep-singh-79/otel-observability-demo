package com.sandeep.api.tests.serviceVirtualization.delayed;

import com.sandeep.api.tests.BaseAPITest;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.sandeep.api.base.EndPoints.UNKNOWN;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.Method.GET;
import static java.time.Duration.ofSeconds;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.awaitility.Awaitility.await;

@Slf4j
public class APIAwaitilityTest extends BaseAPITest {
    @BeforeMethod
    public void initTestSetup() {
        /*apiBase = new ApiBase(config.getProperty("baseUrl"), Byte.parseByte(config.getProperty("basePort", "80")),
                config.getProperty("basePath"));*/
    }

    @Test
    public void TestDelayedResponse() throws IOException {
        Span parentSpan = Span.current();
        Tracer tracer = GlobalOpenTelemetry.getTracer("api-tests");
        long startTime = System.currentTimeMillis();
        try {
            // Arrange
            Span arrangeSpan = tracer.spanBuilder("request").setParent(Context.current().with(parentSpan)).startSpan();
            try (Scope arrangeScope = arrangeSpan.makeCurrent()) {
                this.jsonBody = readFileToString(
                        new java.io.File("src/test/resources/test_data/mockData/listResourceResponse.json"),
                        java.nio.charset.StandardCharsets.UTF_8);
                wireMockServer.stubFor(get(urlMatching(UNKNOWN + "\\?delay=[0-9]+"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withFixedDelay(3000)
                                .withHeader("Content-Type", JSON.toString())
                                .withBody(jsonBody)));
                arrangeSpan.addEvent("Stub created for GET /unknown?delay=3");
            } finally { arrangeSpan.end(); }

            // Request/Response/Assertion as child spans of the main test span
            final Response[] responseHolder = new Response[1];
            await()
                .between(ofSeconds(2), ofSeconds(10))
                .untilAsserted(() -> {
                    // Request span
                    Span requestSpan = tracer.spanBuilder("request").setParent(Context.current().with(parentSpan)).startSpan();
                    try (Scope requestScope = requestSpan.makeCurrent()) {
                        try {
                            responseHolder[0] = apiBase
                                    .get_response(GET, UNKNOWN + "?delay=3")
                                    .andReturn();
                            requestSpan.addEvent("Request sent", Attributes.of(
                                AttributeKey.stringKey("request.url"), UNKNOWN + "?delay=3",
                                AttributeKey.stringKey("request.method"), "GET"
                            ));
                            // Add request headers (excluding Authorization)
                            responseHolder[0].getHeaders().asList().stream()
                                .filter(h -> !h.getName().equalsIgnoreCase("Authorization"))
                                .forEach(h -> requestSpan.setAttribute("response.header." + h.getName(), h.getValue()));
                            requestSpan.setAttribute("response.status", responseHolder[0].getStatusCode());
                            requestSpan.setAttribute("response.body.length", responseHolder[0].getBody().asString().length());
                        } catch (Exception e) {
                            requestSpan.recordException(e);
                            requestSpan.setStatus(StatusCode.ERROR, "Request step failed");
                            throw e;
                        }
                    } finally { requestSpan.end(); }

                    // Assertion span
                    Span assertionSpan = tracer.spanBuilder("assertion").setParent(Context.current().with(parentSpan)).startSpan();
                    try (Scope assertionScope = assertionSpan.makeCurrent()) {
                        try {
                            responseHolder[0].then().statusCode(200);
                            assertionSpan.addEvent("Asserted status code 200");
                        } catch (AssertionError ae) {
                            assertionSpan.recordException(ae);
                            assertionSpan.setStatus(StatusCode.ERROR, "Assertion failed");
                            throw ae;
                        }
                    } finally { assertionSpan.end(); }
                });
        } finally {
            parentSpan.setAttribute("test.execution.time.ms", System.currentTimeMillis() - startTime);
        }
    }
}

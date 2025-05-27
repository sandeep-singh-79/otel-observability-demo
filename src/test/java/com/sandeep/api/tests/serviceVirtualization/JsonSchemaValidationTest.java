package com.sandeep.api.tests.serviceVirtualization;

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
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.sandeep.api.base.EndPoints.USERS;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.Method.GET;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;

@Slf4j
public class JsonSchemaValidationTest extends BaseAPITest {

    @BeforeMethod
    public void init() {
        /*apiBase = new ApiBase(config.getProperty("baseUrl"),
                Byte.parseByte(config.getProperty("basePort", "80")),
                config.getProperty("basePath"));*/
        try {
            this.jsonBody = FileUtils.readFileToString(
                    new File("src/test/resources/test_data/mockData/usersResponse.json"),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(e.getCause().toString());
            e.printStackTrace();
        }
    }

    @Test
    public void validateJsonSchemaForGetUsers() throws Exception {
        Span parentSpan = Span.current();
        Tracer tracer = GlobalOpenTelemetry.getTracer("api-tests");
        long startTime = System.currentTimeMillis();
        // Arrange
        Span arrangeSpan = tracer.spanBuilder("request").setParent(Context.current().with(parentSpan)).startSpan();
        try (Scope arrangeScope = arrangeSpan.makeCurrent()) {
            wireMockServer.stubFor(get(urlEqualTo(USERS.toString()))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", JSON.toString())
                            .withBody(jsonBody)));
            arrangeSpan.addEvent("Stub created for GET /users");
        } finally { arrangeSpan.end(); }

        // Request/Response
        Response localResponse = null;
        Span reqSpan = tracer.spanBuilder("request-response").setParent(Context.current().with(parentSpan)).startSpan();
        try (Scope reqScope = reqSpan.makeCurrent()) {
            try {
                localResponse = apiBase.get_response(GET, USERS).andReturn();
                reqSpan.addEvent("Request sent", Attributes.of(
                    AttributeKey.stringKey("request.url"), USERS.toString(),
                    AttributeKey.stringKey("request.method"), "GET"
                ));
                localResponse.getHeaders().asList().stream()
                    .filter(h -> !h.getName().equalsIgnoreCase("Authorization"))
                    .forEach(h -> reqSpan.setAttribute("response.header." + h.getName(), h.getValue()));
                reqSpan.setAttribute("response.status", localResponse.getStatusCode());
                reqSpan.setAttribute("response.body.length", localResponse.getBody().asString().length());
            } catch (Exception e) {
                reqSpan.recordException(e);
                reqSpan.setStatus(StatusCode.ERROR, "Request step failed");
                throw e;
            }
        } finally { reqSpan.end(); }

        // Assertion
        Span assertionSpan = tracer.spanBuilder("assertion").setParent(Context.current().with(parentSpan)).startSpan();
        try (Scope assertionScope = assertionSpan.makeCurrent()) {
            try {
                localResponse
                        .then()
                        .statusCode(200)
                        .assertThat()
                        .body(matchesJsonSchema(new java.io.File("src/test/resources/test_data/jsonSchema/reqresResponseSchema.json")));
                assertionSpan.addEvent("Asserted status code 200 and schema validation");
            } catch (AssertionError ae) {
                assertionSpan.recordException(ae);
                assertionSpan.setStatus(StatusCode.ERROR, "Assertion failed");
                throw ae;
            }
        } finally { assertionSpan.end(); }
        parentSpan.setAttribute("test.execution.time.ms", System.currentTimeMillis() - startTime);
    }
}

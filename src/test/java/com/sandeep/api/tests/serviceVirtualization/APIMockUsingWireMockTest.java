package com.sandeep.api.tests.serviceVirtualization;

import com.sandeep.api.tests.BaseAPITest;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.sandeep.api.base.EndPoints.USERS;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.common.Attributes.of;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.Method.GET;
import static org.testng.Assert.assertTrue;

@Slf4j
@Listeners(value = {com.sandeep.api.listeners.OpenTelemetryTestListener.class, 
                    com.aventstack.extentreports.testng.listener.ExtentITestListenerClassAdapter.class})
public class APIMockUsingWireMockTest extends BaseAPITest {
    private Response response;

    @BeforeMethod
    public void setupPreReqs() {
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
    public void verifyForFirstNameInResponse() {
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
        Span reqSpan = tracer.spanBuilder("request-response").setParent(Context.current().with(parentSpan)).startSpan();
        try (Scope reqScope = reqSpan.makeCurrent()) {
            try {
                response = apiBase.get_response(GET, USERS).andReturn();
                reqSpan.addEvent("Request sent", of(
                    stringKey("request.url"), USERS.toString(),
                    stringKey("request.method"), "GET"
                ));
                response.getHeaders().asList().stream()
                    .filter(h -> !h.getName().equalsIgnoreCase("Authorization"))
                    .forEach(h -> reqSpan.setAttribute("response.header." + h.getName(), h.getValue()));
                reqSpan.setAttribute("response.status", response.getStatusCode());
                reqSpan.setAttribute("response.body.length", response.getBody().asString().length());
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
                response.then().statusCode(200);
                boolean hasGeorge = response.getBody().jsonPath().getList("data.first_name").contains("George");
                assertionSpan.setAttribute("assertion.contains.george", hasGeorge);
                assertionSpan.addEvent("Checked for 'George' in response: " + hasGeorge);
                assertTrue(hasGeorge);
            } catch (AssertionError ae) {
                assertionSpan.recordException(ae);
                assertionSpan.setStatus(StatusCode.ERROR, "Assertion failed");
                throw ae;
            }
        } finally { assertionSpan.end(); }
        parentSpan.setAttribute("test.execution.time.ms", System.currentTimeMillis() - startTime);
    }

    @Test
    public void TestUserNotFoundHasStatusCode404() {
        Span parentSpan = Span.current();
        Tracer tracer = GlobalOpenTelemetry.getTracer("api-tests");
        long startTime = System.currentTimeMillis();
        // Arrange
        Span arrangeSpan = tracer.spanBuilder("request").setParent(Context.current().with(parentSpan)).startSpan();
        try (Scope arrangeScope = arrangeSpan.makeCurrent()) {
            wireMockServer.stubFor(get(urlEqualTo(USERS.toString()))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", JSON.toString())
                            .withBody("{}")));
            arrangeSpan.addEvent("Stub created for GET /users with 404");
        } finally { arrangeSpan.end(); }

        // Request/Response
        Span reqSpan = tracer.spanBuilder("request-response").setParent(Context.current().with(parentSpan)).startSpan();
        try (Scope reqScope = reqSpan.makeCurrent()) {
            try {
                response = apiBase.get_response(GET, USERS).andReturn();
                reqSpan.addEvent("Request sent", of(
                    stringKey("request.url"), USERS.toString(),
                    stringKey("request.method"), "GET"
                ));
                response.getHeaders().asList().stream()
                    .filter(h -> !h.getName().equalsIgnoreCase("Authorization"))
                    .forEach(h -> reqSpan.setAttribute("response.header." + h.getName(), h.getValue()));
                reqSpan.setAttribute("response.status", response.getStatusCode());
                reqSpan.setAttribute("response.body.length", response.getBody().asString().length());
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
                response.then().statusCode(404);
                assertionSpan.addEvent("Asserted status code 404");
            } catch (AssertionError ae) {
                assertionSpan.recordException(ae);
                assertionSpan.setStatus(StatusCode.ERROR, "Assertion failed");
                throw ae;
            }
        } finally { assertionSpan.end(); }
        parentSpan.setAttribute("test.execution.time.ms", System.currentTimeMillis() - startTime);
    }
}

package com.sandeep.api.tests.contract.pact;

import au.com.dius.pact.consumer.PactVerificationResult;
import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.RequestResponsePact;
import com.sandeep.api.base.ApiBase;
import com.sandeep.api.tests.contract.pact.util.PactUtil;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;

import static au.com.dius.pact.consumer.ConsumerPactRunnerKt.runConsumerTest;
import static au.com.dius.pact.core.model.PactSpecVersion.V4;
import static com.sandeep.api.base.EndPoints.USERS;
import static io.restassured.http.Method.GET;
import static java.lang.String.format;
import static org.testng.Assert.assertTrue;

@Slf4j
public class PactConsumerTest {
    private Response response;

    @Test
    public void testConsumerContractForGetAllUsers() {
        Tracer tracer = GlobalOpenTelemetry.getTracer("api-tests");

        // Arrange
        Span contractSpan = tracer.spanBuilder("Step: Define Pact Contract").startSpan();
        RequestResponsePact responsePact;
        try (Scope scope = contractSpan.makeCurrent()) {
            responsePact = PactUtil.getGetRequestResponsePact("A set of users exist",
                    "reqResUserConsumer",
                    "reqResUserProvider",
                    "ReqRes user fetch test",
                    format("/api%s", USERS));

            contractSpan.setAttribute("contract.consumer", "reqResUserConsumer");
            contractSpan.setAttribute("contract.provider", "reqResUserProvider");
            contractSpan.setAttribute("contract.path", format("/api/%s", USERS));
        } finally {
            contractSpan.end();
        }

        PactVerificationResult pactConsumerResult;
        Span executionSpan = tracer.spanBuilder("Step: Execute Pact Consumer Test").startSpan();
        try (Scope scope = executionSpan.makeCurrent()) {
            MockProviderConfig pactConsumerConfig = MockProviderConfig.httpConfig("localhost",
                    8090,
                    V4);

            pactConsumerResult = runConsumerTest(responsePact,
                    pactConsumerConfig,
                    (mockServer, pactTestExecutionContext) -> {
                        // Action
                        try {
                            response = new ApiBase(mockServer.getUrl(), mockServer.getPort(), "/api")
                                    .get_response(GET, USERS)
                                    .andReturn();
                        } catch (NullPointerException e) {
                            log.error("Unable to initialize Response object as null was returned!");
                        }
                        return null;
                    });
            executionSpan.setAttribute("mock.url", "http://localhost:8090/api");
        } finally {
            executionSpan.end();
        }

        Span assertionSpan = tracer.spanBuilder("Step: Assert API response").startSpan();
        try (Scope scope = assertionSpan.makeCurrent()) {
            if (pactConsumerResult instanceof PactVerificationResult.Error) {
                throw new RuntimeException(((PactVerificationResult.Error) pactConsumerResult).getError());
            }

            // Assert
            response.then().statusCode(200);
            var hasGeorge = response.getBody()
                             .jsonPath()
                             .getList("data.first_name")
                             .contains("George");
            assertionSpan.setAttribute("response.contains.george", hasGeorge);
            assertTrue(hasGeorge);
            //assertEquals(new Ok(), pactConsumerResult)
        } finally {
            assertionSpan.end();
        }
    }
}

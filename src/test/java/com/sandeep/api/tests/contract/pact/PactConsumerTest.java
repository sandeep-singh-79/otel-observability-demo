package com.sandeep.api.tests.contract.pact;

import au.com.dius.pact.consumer.PactVerificationResult;
import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.RequestResponsePact;
import com.sandeep.api.base.ApiBase;
import com.sandeep.api.tests.contract.pact.util.PactUtil;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
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
        Tracer tracer = GlobalOpenTelemetry.getTracer("cdc-tests");

        // Arrange
        Span contractSpan = tracer.spanBuilder("Step: Define Pact Contract").startSpan();
        RequestResponsePact responsePact;
        try (Scope scope = contractSpan.makeCurrent()) {
            responsePact = PactUtil.getGetRequestResponsePact("A set of users exist",
                    "reqResUserConsumer",
                    "reqResUserProvider",
                    "ReqRes user fetch test",
                    format("/api%s", USERS));

            var contractAttributes = Attributes.builder()
                                         .put("contract.consumer", "reqResUserConsumer")
                                         .put("contract.provider", "reqResUserProvider")
                                         .put("contract.path", format("/api/%s", USERS))
                                         .build();
            contractSpan.setAllAttributes(contractAttributes);
            // add pact related details as event to contractSpan
            contractSpan.addEvent("Pact Contract Created", contractAttributes);
        } finally {
            contractSpan.end();
        }

        PactVerificationResult pactConsumerResult;
        Span executionSpan = tracer.spanBuilder("Step: Execute Pact Consumer Test").startSpan();
        try (Scope scope = executionSpan.makeCurrent()) {
            MockProviderConfig pactConsumerConfig = MockProviderConfig.httpConfig("localhost",
                    8090,
                    V4);

            executionSpan.addEvent("MockProviderConfig initialized", Attributes.of(
                AttributeKey.stringKey("mock.host"), "localhost",
                AttributeKey.stringKey("mock.port"), "8090"
            ));

            executionSpan.addEvent("Running Pact Consumer Test");

            pactConsumerResult = runConsumerTest(responsePact,
                    pactConsumerConfig,
                    (mockServer, pactTestExecutionContext) -> {
                        // Action
                        try {
                            executionSpan.addEvent("Sending GET request to /api/users");
                            response = new ApiBase(mockServer.getUrl(), mockServer.getPort(), "/api")
                                    .get_response(GET, USERS)
                                    .andReturn();
                            executionSpan.addEvent("Received response", Attributes.of(
                                AttributeKey.longKey("http.status_code"), (long) response.getStatusCode()
                            ));
                        } catch (NullPointerException e) {
                            executionSpan.addEvent("Unable to initialize Response object as null was returned!");
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
                assertionSpan.addEvent("Pact verification failed", Attributes.of(
                    AttributeKey.stringKey("error"), ((PactVerificationResult.Error) pactConsumerResult).getError().toString()
                ));
                throw new RuntimeException(((PactVerificationResult.Error) pactConsumerResult).getError());
            }

            // Assert
            assertionSpan.addEvent("Asserting response status code == 200");
            response.then().statusCode(200);
            var hasGeorge = response.getBody()
                             .jsonPath()
                             .getList("data.first_name")
                             .contains("George");
            assertionSpan.setAttribute("response.contains.george", hasGeorge);
            assertionSpan.addEvent("Checked for 'George' in response: " + hasGeorge);
            assertTrue(hasGeorge);
            //assertEquals(new Ok(), pactConsumerResult)
        } finally {
            assertionSpan.end();
        }
    }
}

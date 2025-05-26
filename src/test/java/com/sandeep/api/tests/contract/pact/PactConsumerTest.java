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
import io.opentelemetry.api.trace.StatusCode;
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
    public void testConsumerContractForGetAllUsers () {
        Tracer tracer = GlobalOpenTelemetry.getTracer("api-test-automation");

        Span contractSpan = tracer.spanBuilder("PactConsumerTest.DefinePactContract").startSpan();
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
            contractSpan.addEvent("Pact Contract Created", contractAttributes);
        } catch (Exception e) {
            contractSpan.recordException(e);
            contractSpan.setStatus(StatusCode.ERROR, "Contract creation failed");
            throw e;
        } finally {
            contractSpan.end();
        }

        PactVerificationResult pactConsumerResult;
        Span executionSpan = tracer.spanBuilder("PactConsumerTest.ExecuteConsumerTest").startSpan();
        try (Scope scope = executionSpan.makeCurrent()) {
            MockProviderConfig pactConsumerConfig = MockProviderConfig.httpConfig("localhost", 8090, V4);
            executionSpan.addEvent("MockProviderConfig initialized", Attributes.of(
                AttributeKey.stringKey("mock.host"), "localhost",
                AttributeKey.stringKey("mock.port"), "8090"
            ));

            executionSpan.addEvent("Running Pact Consumer Test");

            pactConsumerResult = runConsumerTest(responsePact, pactConsumerConfig, (mockServer, pactCtx) -> {
                try {
                    executionSpan.addEvent("Sending GET request to /api/users");
                    response = new ApiBase(mockServer.getUrl(), mockServer.getPort(), "/api")
                                   .get_response(GET, USERS)
                                   .andReturn();
                    executionSpan.addEvent("Received response", Attributes.of(
                        AttributeKey.longKey("http.status_code"), (long) response.getStatusCode()
                    ));
                } catch (Exception e) {
                    executionSpan.recordException(e);
                    executionSpan.setStatus(StatusCode.ERROR, "Request execution failed");
                    throw e;
                }
                return null;
            });
            executionSpan.setAttribute("mock.url", "http://localhost:8090/api");
        } catch (Exception e) {
            executionSpan.recordException(e);
            executionSpan.setStatus(StatusCode.ERROR, "Test execution failed");
            throw e;
        } finally {
            executionSpan.end();
        }

        Span assertionSpan = tracer.spanBuilder("PactConsumerTest.AssertApiResponse").startSpan();
        try (Scope scope = assertionSpan.makeCurrent()) {
            if (pactConsumerResult instanceof PactVerificationResult.Error) {
                Throwable error = ((PactVerificationResult.Error) pactConsumerResult).getError();
                assertionSpan.recordException(error);
                assertionSpan.setStatus(StatusCode.ERROR, "Pact verification failed");
                throw new RuntimeException(error);
            }

            assertionSpan.addEvent("Asserting response status code == 200");
            response.then().statusCode(200);

            var hasGeorge = response.getBody()
                                .jsonPath()
                                .getList("data.first_name")
                                .contains("George");

            assertionSpan.setAttribute("response.contains.george", hasGeorge);
            assertionSpan.addEvent("Checked for 'George' in response: " + hasGeorge);
            assertTrue(hasGeorge);
        } catch (Exception e) {
            assertionSpan.recordException(e);
            assertionSpan.setStatus(StatusCode.ERROR, "Assertion failed");
            throw e;
        } finally {
            assertionSpan.end();
        }
    }
}

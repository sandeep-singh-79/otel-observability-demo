package com.sandeep.api.tests.contract.pact;

import au.com.dius.pact.consumer.PactVerificationResult;
import au.com.dius.pact.consumer.model.MockProviderConfig;
import au.com.dius.pact.core.model.RequestResponsePact;
import com.sandeep.api.base.ApiBase;
import com.sandeep.api.tests.contract.pact.util.PactUtil;
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
        // Arrange
        RequestResponsePact responsePact = PactUtil.getGetRequestResponsePact("A set of users exist",
                "reqResUserConsumer",
                "reqResUserProvider",
                "ReqRes user fetch test",
                format("/api%s", USERS));

        MockProviderConfig pactConsumerConfig = MockProviderConfig.httpConfig("localhost",
                8090,
                V4);

        PactVerificationResult pactConsumerResult = runConsumerTest(responsePact,
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

        if (pactConsumerResult instanceof PactVerificationResult.Error) {
            throw new RuntimeException(((PactVerificationResult.Error) pactConsumerResult).getError());
        }

        // Assert
        response.then().statusCode(200);
        assertTrue(response.getBody()
                .jsonPath()
                .getList("data.first_name")
                .contains("George"));
        //assertEquals(new Ok(), pactConsumerResult)
    }
}

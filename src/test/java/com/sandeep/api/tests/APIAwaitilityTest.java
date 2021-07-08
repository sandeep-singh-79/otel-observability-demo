package com.sandeep.api.tests;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.sandeep.api.base.EndPoints.UNKNOWN;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.Method.GET;
import static org.awaitility.Awaitility.await;

@Slf4j
public class APIAwaitilityTest extends BaseAPITest {
    @BeforeMethod
    public void initTestSetup() {
        /*apiBase = new ApiBase(config.getProperty("baseUrl"), Byte.parseByte(config.getProperty("basePort", "80")),
                config.getProperty("basePath"));*/
    }

    private Response response;

    @Test
    public void TestDelayedResponse() throws IOException {
        this.jsonBody = FileUtils.readFileToString(
                new File("src/test/resources/test_data/mockData/listResourceResponse.json"),
                StandardCharsets.UTF_8);
        // Arrange
        wireMockServer.stubFor(get(urlMatching(UNKNOWN + "\\?delay=[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(3000)
                        .withHeader("Content-Type", JSON.toString())
                        .withBody(jsonBody)));

        // Action
        await()
            .between(Duration.ofSeconds(2), Duration.ofSeconds(10))
            .untilAsserted(() -> {
                try {
                    response = apiBase
                            .get_response(GET, UNKNOWN + "?delay=3")
                            .andReturn();
                } catch (NullPointerException e) {
                    log.error("Unable to initialize Response object as null was returned!");
                }

                // Assert
                response.then().statusCode(200);
            });
    }
}

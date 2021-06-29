package com.sandeep.api.tests;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.sandeep.api.base.EndPoints.USERS;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.Method.GET;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Slf4j
public class APIAwaitilityTest extends BaseAPITest {
    private static String jsonBody = "";

    @BeforeMethod
    public void initTestSetup() {
        try {
            jsonBody = FileUtils.readFileToString(
                    new File("src/test/resources/test_data/mockData/usersResponse.json"),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(e.getCause().toString());
            e.printStackTrace();
        }

        /*apiBase = new ApiBase(config.getProperty("baseUrl"), Byte.parseByte(config.getProperty("basePort", "80")),
                config.getProperty("basePath"));*/
    }

    private Response response;

    @Test
    public void TestDelayedResponse() {
        // Arrange
        wireMockServer.stubFor(get(urlMatching(format("%s?delay", USERS)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(5000)
                        .withHeader("Content-Type", JSON.toString())
                        .withBody(jsonBody)));

        // Action
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    try {
                        Map<String, String> queryParams = new HashMap<>();
                        queryParams.put("delay", "4");
                        response = apiBase
                                .get_response(GET, USERS)
                                .andReturn();
                    } catch (NullPointerException e) {
                        log.error("Unable to initialize Response object as null was returned!");
                    }

                    // Assert
                    assertThat(response.statusCode(), is(200));
                    //assertTrue(response.getBody().jsonPath().getList("data.first_name").contains("George"));
                });
    }
}

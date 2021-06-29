package com.sandeep.api.tests;

import com.github.fge.jsonschema.SchemaVersion;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.sandeep.api.base.EndPoints.USERS;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.Method.GET;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static org.testng.Assert.assertTrue;

@Slf4j
public class APIMockUsingWireMockTest extends BaseAPITest {
    private static String jsonBody = "";

    @BeforeMethod
    public void initJsonBody() {
        try {
            jsonBody = FileUtils.readFileToString(
                    new File("src/test/resources/test_data/mockData/usersResponse.json"),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(e.getCause().toString());
            e.printStackTrace();
        }
    }

    private Response response;

    @Test
    public void verifyForFirstNameInResponse() {
        // Arrange
        wireMockServer.stubFor(get(urlEqualTo(USERS.toString()))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", JSON.toString())
                        .withBody(jsonBody)));

        // Action
        try {
            response = apiBase.get_response(GET, USERS).andReturn();
        } catch (NullPointerException e) {
            log.error("Unable to initialize Response object as null was returned!");
        }

        // Assert
        response.then().statusCode(200);
        assertTrue(response.getBody().jsonPath().getList("data.first_name").contains("George"));
    }

    @Test
    public void TestUserNotFoundHasStatusCode404() {
        wireMockServer.stubFor(get(urlEqualTo(USERS.toString()))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", JSON.toString())
                        .withBody("{}")
                ));

        // Action
        try {
            response = apiBase.get_response(GET, USERS).andReturn();
        } catch (NullPointerException e) {
            log.error("Unable to initialize Response object as null was returned!");
        }

        // Assert
        response.then().statusCode(404);
    }

    @Test (enabled = false)
    // this one works. disabled as apiBase object not initialized to the correct endpoint
    public void validateJsonSchemaForGetUsers() throws FileNotFoundException {
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.newBuilder()
                .setValidationConfiguration(
                        ValidationConfiguration.newBuilder()
                                .setDefaultVersion(SchemaVersion.DRAFTV4).freeze())
                .freeze();
        try {
            response = apiBase.get_response(GET, USERS).andReturn();
        } catch (NullPointerException e) {
            log.error("Unable to initialize Response object as null was returned!");
        }

        response
                .then()
                .statusCode(200)
                .assertThat()
                .body(matchesJsonSchema(new FileInputStream("src/test/resources/test_data/jsonSchema/reqresResponseSchema.json"))
                        .using(jsonSchemaFactory));

    }
}

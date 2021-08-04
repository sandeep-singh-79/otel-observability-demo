package com.sandeep.api.tests.serviceVirtualization;

import com.github.fge.jsonschema.SchemaVersion;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.sandeep.api.tests.BaseAPITest;
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
    // this one works. disabled as apiBase object not initialized to the correct endpoint
    public void validateJsonSchemaForGetUsers() throws FileNotFoundException {
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.newBuilder()
                .setValidationConfiguration(
                        ValidationConfiguration.newBuilder()
                                .setDefaultVersion(SchemaVersion.DRAFTV4).freeze())
                .freeze();

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
        response
                .then()
                .statusCode(200)
                .assertThat()
                .body(matchesJsonSchema(new FileInputStream("src/test/resources/test_data/jsonSchema/reqresResponseSchema.json"))
                        .using(jsonSchemaFactory));

    }
}

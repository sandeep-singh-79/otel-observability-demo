package com.sandeep.api.tests;

import com.github.fge.jsonschema.SchemaVersion;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.sandeep.api.base.ApiBase;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static com.sandeep.api.base.EndPoints.USERS;
import static io.restassured.http.Method.GET;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;

@Slf4j
public class JsonSchemaValidationTest extends BaseAPITest {

    @BeforeMethod
    public void init() {
        apiBase = new ApiBase(config.getProperty("baseUrl"),
                Byte.parseByte(config.getProperty("basePort", "80")),
                config.getProperty("basePath"));
    }

    @Test
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

package com.sandeep.api.tests.contract.pact.util;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.core.model.RequestResponsePact;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;

@Slf4j
public class PactUtil {

    /**
     * The method generates a consumer pact file for a get request
     * @param consumer
     * @param provider
     * @param contractDescription
     * @param path
     * @return
     */
    @NotNull
    public static RequestResponsePact getGetRequestResponsePact(String consumer, String provider, String contractDescription, String path) {
        return ConsumerPactBuilder
                .consumer(consumer)
                .hasPactWith(provider)
                .uponReceiving(contractDescription)
                .path(path)
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(getJsonContentTypeHeader())
                .body(initJsonBodyFromFile(new File("src/test/resources/test_data/mockData/usersResponse.json")))
                .toPact();
    }


    /**
     * The method returns the json body in String format on the basis of the supplied file.
     * It returns an empty string in case of an exception
     * @param jsonBodyFile
     * @return              json string representation from the supplied file
     */
    @NotNull
    public static String initJsonBodyFromFile(File jsonBodyFile) {
        String jsonBody;
        try {
            jsonBody = FileUtils.readFileToString(
                    jsonBodyFile,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(e.getCause().toString());
            e.printStackTrace();
            jsonBody = "";
        }
        return jsonBody;
    }

    /**
     *
     * @return  A hashmap of header values
     */
    @NotNull
    public static Map<String, String> getJsonContentTypeHeader() {
        Map<String, String> pactResponseHeader = new HashMap<>();
        pactResponseHeader.put("Content-Type", JSON.toString());
        return pactResponseHeader;
    }
}

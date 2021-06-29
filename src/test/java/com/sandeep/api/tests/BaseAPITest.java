package com.sandeep.api.tests;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.sandeep.api.base.ApiBase;
import com.sandeep.api.config.FrameworkConfig;
import com.sandeep.api.config.PropertyFileReader;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.asserts.SoftAssert;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@Slf4j
@Listeners(value = com.aventstack.extentreports.testng.listener.ExtentITestListenerClassAdapter.class)
public abstract class BaseAPITest {
    protected Properties config = FrameworkConfig.getInstance().getConfigProperties();
    protected ApiBase apiBase;
    protected RequestSpecification requestSpec;
    protected Properties test_data_props = new PropertyFileReader(new File(System.getProperty("user.dir") +
            "/src/test/resources/test_data/api_test_data.properties")).getPropertyFile();
    protected Response response;
    protected String base_url;
    protected byte base_port;
    protected Headers headers;
    protected SoftAssert soft_assert = new SoftAssert();
    static protected WireMockServer wireMockServer = new WireMockServer(options().port(8089)); //No-args constructor will start on port 8080, no HTTPS;
    protected String jsonBody;

    @BeforeClass
    public void setup() {
        try {
            jsonBody = FileUtils.readFileToString(
                    new File("src/test/resources/test_data/mockData/usersResponse.json"),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(e.getCause().toString());
            e.printStackTrace();
        }

        wireMockServer.start();

        /*apiBase = new ApiBase(config.getProperty("baseUrl"), Byte.valueOf(config.getProperty("basePort", "80")),
                config.getProperty("basePath"));*/
        apiBase = new ApiBase("http://localhost", 8089, "");
    }

    @AfterClass
    public static void cleanup() {
        RestAssured.reset();
        wireMockServer.stop();
    }

    @AfterMethod
    public void log_api_test_result(ITestResult test_result) {
        int method_status = test_result.getStatus();
        String test_status = "";
        switch (method_status) {
            case ITestResult.SUCCESS:
                test_status = "SUCCESS";
                break;
            case ITestResult.FAILURE:
                test_status = "FAILURE";
                break;
            case ITestResult.SKIP:
                test_status = "SKIP";
                break;
        }

        Parameter[] params = test_result.getMethod().getConstructorOrMethod().getMethod().getParameters();
        log.info("Test {} with param(s) {} executed with result: {}", test_result.getMethod().getMethodName(),
                Arrays.stream(params).map(Parameter::getName).collect(Collectors.toList()), test_status);
    }

    protected ApiBase init_api_base(final String base_url,
                                    final byte base_port,
                                    final String end_point,
                                    final Headers headers,
                                    final ContentType contentType) {
        return new ApiBase(base_url, base_port, end_point)
                .set_request_headers(headers)
                .set_content_type(contentType);
    }

    protected List<Map<String, Object>> get_lst_nodes(Response response, String json_path) {
        return response
                .jsonPath()
                .get(json_path);
    }
}

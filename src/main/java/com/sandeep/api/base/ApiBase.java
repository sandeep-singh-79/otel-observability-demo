package com.sandeep.api.base;

import io.restassured.RestAssured;
import io.restassured.authentication.BasicAuthScheme;
import io.restassured.authentication.PreemptiveBasicAuthScheme;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.*;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Map;

@Slf4j
public class ApiBase {
    private RequestSpecification requestSpecification;
    private RequestSpecBuilder specBuilder;

    public ApiBase(String baseURI, int port, String basePath) {
        specBuilder = new RequestSpecBuilder().setBaseUri(baseURI).setPort(port).setBasePath(basePath);
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    public ApiBase set_preemptive_basic_auth(final String user, final String password) {
        PreemptiveBasicAuthScheme authScheme = new PreemptiveBasicAuthScheme();
        authScheme.setUserName(user);
        authScheme.setPassword(password);
        specBuilder.setAuth(authScheme);

        return this;
    }

    public ApiBase set_base_path(String base_path) {
        if (StringUtils.isNotBlank(base_path)) specBuilder.setBasePath(base_path);
        else throw new RuntimeException("base path supplied is blank!");

        return this;
    }

    public ApiBase set_contentType_and_body(final ContentType contentType, final String body) {
        return set_content_type(contentType).set_body(body);
    }

    public ApiBase set_content_type(final String content_type) {
        specBuilder.setContentType(content_type);
        return this;
    }

    public ApiBase set_content_type(final ContentType content_type) {
        return set_content_type(content_type.toString());
    }

    public ApiBase set_body(final String body) {
        specBuilder.setBody(body);
        return this;
    }

    public ApiBase set_basic_auth(final String user, final String password) {
        BasicAuthScheme authScheme = new BasicAuthScheme();
        authScheme.setUserName(user);
        authScheme.setPassword(password);
        specBuilder.setAuth(authScheme);

        return this;
    }

    public ApiBase set_session_config(final String session_id) {
        specBuilder.setSessionId(session_id);

        return this;
    }

    public ApiBase set_request_headers(final Headers headers) {
        headers.forEach(header -> specBuilder.addHeader(header.getName(), header.getValue()));
        return this;
    }

    public RequestSpecification build_request_spec() {
        requestSpecification = specBuilder.build().given();
        return requestSpecification;
    }

    public Response get_response(final Method method, final EndPoints end_point) throws NullPointerException {
        return this.get_response(method, end_point.toString());
    }

    public ApiBase set_multi_part(final String param_name, final String param_value) {
        specBuilder.addMultiPart(param_name, param_value);
        return this;
    }

    public ApiBase set_multi_part(String param_name, final File file) {
        specBuilder.addMultiPart(param_name, file);
        return this;
    }

    public ApiBase set_path_params(final String param_name, final String param_value) {
        specBuilder.addPathParam(param_name, param_value);
        return this;
    }

    public ApiBase set_query_params(final Map<String, ?> params) {
        requestSpecification.params(params);
        return this;
    }

    public ApiBase set_cookie(Cookie cookie) {
        specBuilder.addCookie(cookie);
        return this;
    }

    public ApiBase set_cookies(Cookies cookies) {
        specBuilder.addCookies(cookies);
        return this;
    }

    public static ApiBase init_api_base(final String base_url,
                                        final byte base_port,
                                        final String end_point,
                                        final Headers headers,
                                        final ContentType contentType,
                                        final Cookie cookie) {
        return init_api_base(base_url, base_port, end_point, headers, contentType)
                .set_cookie(cookie);
    }

    public static ApiBase init_api_base(final String base_url,
                                        final byte base_port,
                                        final String end_point,
                                        final Headers headers,
                                        final ContentType contentType) {
        return init_api_base(base_url, base_port, end_point, headers)
                .set_content_type(contentType);
    }

    public static ApiBase init_api_base(final String base_url,
                                        final byte base_port,
                                        final String end_point,
                                        final Headers headers) {
        return new ApiBase(base_url, base_port, end_point)
                .set_request_headers(headers);
    }

    public Response get_response(final Method method, final String end_point) throws NullPointerException {
        Response response = null;
        if (requestSpecification == null && specBuilder != null) {
            requestSpecification = build_request_spec();
        }

        switch (method.toString()) {
            case "GET":
                response = RestAssured.given().spec(requestSpecification).when().get(end_point);
                break;
            case "POST":
                response = RestAssured.given().spec(requestSpecification).when().post(end_point);
                break;
            case "PUT":
                response = RestAssured.given().spec(requestSpecification).when().put(end_point);
                break;
            case "DELETE":
                response = RestAssured.given().spec(requestSpecification).when().delete(end_point);
                break;
        }

        return response;
    }
}

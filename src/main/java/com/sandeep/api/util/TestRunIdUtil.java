package com.sandeep.api.util;

import org.testng.ISuite;

public class TestRunIdUtil {
    private static final String ENV_TEST_RUN_ID = "TEST_RUN_ID";

    private TestRunIdUtil() { /* Utility class */ }

    public static String resolveTestRunId(ISuite suite) {
        String testRunId = System.getenv(ENV_TEST_RUN_ID);
        if (testRunId == null || testRunId.isEmpty()) {
            testRunId = System.getProperty(ENV_TEST_RUN_ID);
        }
        if ((testRunId == null || testRunId.isEmpty()) && suite != null) {
            Object suiteParam = suite.getParameter(ENV_TEST_RUN_ID);
            testRunId = suiteParam != null ? suiteParam.toString() : null;
        }
        if (testRunId == null || testRunId.isEmpty()) {
            testRunId = "local-run-" + System.currentTimeMillis();
        }
        return testRunId;
    }
}

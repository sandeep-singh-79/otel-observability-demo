package com.sandeep.api.util;

import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import static org.testng.Assert.fail;

@Slf4j
public class Utils {
    public static String getDate () {
        DateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        Date dateobj = new Date();
        return (df.format(dateobj));
    }

    public static String getTimeStamp () {
        return (getTimeStamp("dd-MMM-yyyy HH:mm:ss").replaceAll(" ", ""));
    }

    public static String getTimeStamp (String dateFormat) {
        DateFormat df = new SimpleDateFormat(dateFormat);
        Date dateobj = new Date();
        return (df.format(dateobj));
    }

    public static void log_exception (Exception exception) {
        log.error("Exception encountered during execution! Please see message: {}", exception.getMessage());
        log.error(Arrays.toString(exception.getStackTrace()));
    }

    public static void log_exception (String message, Exception exception) {
        log.error(message);
        log.error(Arrays.toString(exception.getStackTrace()));
    }

    public static void log_exception_and_fail (Exception exception) {
        log_exception(exception);
        fail();
    }

    public static void log_exception_and_fail (String message, Exception exception) {
        log_exception(message, exception);
        fail();
    }

    public static int get_random_index (final int min, final int max) {
        return (new Random().nextInt(max - min + 1) + min) - 1;
    }
}

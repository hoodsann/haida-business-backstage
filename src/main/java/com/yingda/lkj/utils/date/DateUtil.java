package com.yingda.lkj.utils.date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author hood  2019/11/19
 */
public class DateUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateUtil.class);

    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static Date toDate(String value) {
        return toDate(value, DEFAULT_DATE_PATTERN);
    }

    public static Date toDate(String value, String pattern) {
        if (value == null)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            return sdf.parse(value);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Timestamp toTimestamp(String value) {
        return toTimestamp(value, DEFAULT_DATE_PATTERN);
    }

    public static Timestamp toTimestamp(String value, String pattern) {
        Date date = toDate(value, pattern);
        if (date == null)
            return null;

        return new Timestamp(date.getTime());
    }

    public static String format(Timestamp timestamp, String pattern) {
        if (timestamp == null)
            return "";

        return new SimpleDateFormat(pattern).format(new Date(timestamp.getTime()));
    }

    public static String format(Timestamp timestamp) {
        if (timestamp == null)
            return "";

        return new SimpleDateFormat(DEFAULT_DATE_PATTERN).format(new Date(timestamp.getTime()));
    }

}

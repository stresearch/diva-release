package com.visym.collector.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateUtil {

    public static final String UTC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    public static String getTotalVideoTime(int duration) {
        int min = (int)TimeUnit.MILLISECONDS.toMinutes(duration);
        int sec = (int)TimeUnit.MILLISECONDS.toSeconds(duration);
        return String.format(Locale.getDefault(), "%s:%s",
                countDigit(min) == 1 ? "0" + min : "" + min, countDigit(sec) == 1 ? "0" + sec : "" + sec);
    }

    public static String convertToSecString(double sec){
        long seconds = TimeUnit.MILLISECONDS.toSeconds((int) (sec * 1000));
        return  "00:" + (seconds < 10 ? "0"+seconds : seconds);
    }

    private static int countDigit(int n)
    {
        int count = 0;
        if (n == 0){
            count++;
            return count;
        }
        while (n != 0) {
            n = n / 10;
            ++count;
        }
        return count;
    }

    public static String getDateInUTC(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(UTC_DATE_FORMAT, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }
}

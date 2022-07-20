package com.data.http.utlis;

import com.google.inject.internal.cglib.core.$LocalVariablesSorter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
    static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    static Date date;

    public static String dateutils(int days) throws ParseException {
        date = simpleDateFormat.parse("1899-12-30");
        Calendar now = Calendar.getInstance();
        now.setTime(date);
        now.add(now.DATE, days);
        date = now.getTime();
        return simpleDateFormat.format(date);
    }

    public static void main(String[] args) throws ParseException {

        String dateutils = DateUtils.dateutils(44746);
        System.out.println(dateutils);

    }
}

package com.data.ceph.utils;


import java.text.SimpleDateFormat;
import java.util.Date;

public class StringSplitUtils {

    public static String deleteCharString6(String sourceString, char chElemData) {
        String tmpString = "";
        tmpString += chElemData;
        tmpString.subSequence(0, 0);
        String deleteString = "";
        deleteString = sourceString.replace(tmpString, deleteString.subSequence(0, 0));
        return deleteString;
    }

    public static void main(String[] args) {
        String sourceString = "@time,@sout";
        char chElemData = '@';

        String s = deleteCharString6(sourceString, chElemData);

        System.out.println(s);


        String str = "\"request\": \"POST /thanos-objstore/01G5K9EJT8KNP3T29226372R3B/index?uploads= HTTP/1.1\"";
        String[] split = str.split("/", 3);
        System.out.println(split[0]);
        System.out.println(split[1]);
        System.out.println(split[2]);

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        System.out.println("date===="+format.format(date));
    }
}

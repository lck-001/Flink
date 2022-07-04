package com.data.ceph.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {

    public static void main(String[] args) {

        String str = "AWS4-HMAC-SHA256 Credential=AZG7VD1DTUUQC91SPJ0M/20220505/default/s3/aws4_request, SignedHeaders=content-length;content-md5;host;x-amz-content-sha256;x-amz-date, Signature=ead51df4f4e421e8fe19268bcc3f9b4c453ca59ab51e6d7c081ef7efaa58bad4";

        Pattern pattern = Pattern.compile("^AWS (.+):.*|^AWS4-HMAC-SHA256 Credential=(.*?)/");
        Matcher s = pattern.matcher(str);
        //使用正则式匹配字符串

        if (s.find()) {
            System.out.println( s.group(1) == null ? s.group(2) : s.group(1));
            System.out.println("group1===="+s.group(1));
            System.out.println("group2===="+s.group(2));
        }
    }
}

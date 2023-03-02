package com.data.http.utlis;

import com.alibaba.fastjson.JSON;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import static com.data.http.conf.HttpConf.*;

public class HttpTableUtils extends RichSourceFunction<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpTableUtils.class);
    private static String token_get_url;
    private static String table_get_url;
    private static HttpURLConnection con = null;
    private static BufferedReader in = null;


    @Override
    public void open(Configuration parameters) throws Exception {
        Map<String, String> paramMap = getRuntimeContext().getExecutionConfig().getGlobalJobParameters().toMap();
        APP_ID = paramMap.get("app_id");
        APP_SECRET = paramMap.get("app_secret");
        token_get_url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal/?app_id="
                + URLEncoder.encode(APP_ID, "utf-8")
                + "&app_secret=" + URLEncoder.encode(APP_SECRET, "utf-8");
        table_get_url = "https://open.feishu.cn/open-apis/sheets/v2/spreadsheets/shtcnzFXmzctrVxKl1jUabWuQte/values/0c48ca!A2:C";
    }

    public HttpTableUtils(){
    }

    @Override
    public void run(SourceContext<String> sourceContext) throws Exception {
            ACCESS_TOKEN = JSON.parseObject(httpGet(token_get_url)).getString("tenant_access_token");
            sourceContext.collect(httpGet(table_get_url));
    }

    @Override
    public void cancel() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (con != null) {
            con.disconnect();
        }
    }

    private static String httpGet(String getUrl) {

        StringBuilder inputString = new StringBuilder();

        try {
            URL url = new URL(getUrl);
            con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            if (getUrl.contains("tenant_access_token")){
                con.setRequestMethod("POST");
            }
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("charset", "utf-8");
            if (ACCESS_TOKEN != null) {
                con.setRequestProperty("Authorization", "Bearer " + ACCESS_TOKEN);
            }
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                inputString.append(inputLine);
            }
        } catch (Exception var16) {
            LOGGER.warn("httpget threw: ", var16);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (con != null) {
                    con.disconnect();
                }
            } catch (Exception var15) {
                LOGGER.warn("httpget finally block threw: ", var15);
            }
        }
        return inputString.toString();
    }
}

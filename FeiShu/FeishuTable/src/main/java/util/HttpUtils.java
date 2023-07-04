package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static conf.HttpConf.ACCESS_TOKEN;


public class HttpUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);
    private static HttpURLConnection con = null;
    private static BufferedReader in = null;

    public static String httpGet(String getUrl) {

        StringBuilder inputString = new StringBuilder();

        try {
            URL url = new URL(getUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            if (getUrl.contains("tenant_access_token")) {
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
            while ((inputLine = in.readLine()) != null) {
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

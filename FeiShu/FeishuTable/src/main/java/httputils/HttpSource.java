package httputils;

import com.alibaba.fastjson.JSON;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.SimpleCounter;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.table.data.RowData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpUtils;
import java.net.URLEncoder;
import static conf.HttpConf.ACCESS_TOKEN;

public class HttpSource extends RichSourceFunction<RowData> {

    private volatile boolean isRunning = true;
    private String url;
    private String appId;
    private String appSecret;
    private String appToken;
    private String tableId;
    private String viewId;
    private String httpFilter;
    private long requestInterval;
    private DeserializationSchema<RowData> deserializer;
    // count out event
    private transient Counter counter;

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);
    private static String token_get_url;
    private static String table_get_url;
    private static String table_query_url;


    public HttpSource(String url, String appId, String appSecret, String appToken, String tableId, String viewId, String httpFilter, long requestInterval, DeserializationSchema<RowData> deserializer) {
        this.url = url;
        this.appId = appId;
        this.appSecret = appSecret;
        this.appToken = appToken;
        this.tableId = tableId;
        this.viewId = viewId;
        this.httpFilter = httpFilter;
        this.requestInterval = requestInterval;
        this.deserializer = deserializer;
    }

    @Override
    public void open(Configuration parameters) throws Exception {

        counter = new SimpleCounter();
        this.counter = getRuntimeContext()
                .getMetricGroup()
                .counter("myCounter");

        token_get_url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal/?app_id="
                + URLEncoder.encode(appId, "utf-8")
                + "&app_secret=" + URLEncoder.encode(appSecret, "utf-8");
        table_get_url = url
                + URLEncoder.encode(appToken, "utf-8")
                + "/tables/" + URLEncoder.encode(tableId, "utf-8")
                + "/records?view_id=" + URLEncoder.encode(viewId, "utf-8")
                + (!"null".equals(httpFilter) ? "&filter=" + URLEncoder.encode(httpFilter, "utf-8") : "");
        table_query_url = table_get_url;
    }

    @Override
    public void run(SourceContext<RowData> ctx) throws Exception {
        while (isRunning) {
            try {
                while (isRunning) {
                    ACCESS_TOKEN = JSON.parseObject(HttpUtils.httpGet(token_get_url)).getString("tenant_access_token");
                    String tableJson = HttpUtils.httpGet(table_query_url);
                    String page_token = JSON.parseObject(tableJson).getJSONObject("data").getString("page_token");
                    if (page_token == null || "".equals(page_token)) {
                        isRunning = false;
                    } else {
                        table_query_url = table_get_url + "&page_token=" + URLEncoder.encode(page_token, "utf-8");
                        // deserializer csv message
                        RowData deserialize = deserializer.deserialize(tableJson.getBytes());
                        ctx.collect(deserialize);
                        this.counter.inc();
                    }
                }

                Thread.sleep(requestInterval);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}
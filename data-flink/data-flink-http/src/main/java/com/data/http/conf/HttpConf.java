package com.data.http.conf;

import org.apache.flink.api.java.utils.ParameterTool;

public class HttpConf {
    public static String APP_ID;
    public static String APP_SECRET;
    public static String APP_TOKEN;
    public static String TABLE_ID;
    public static String VIEW_ID;
    public static String ACCESS_TOKEN;
    public static String HTTP_FILTER;
    public static String HTTP_URL;
    public static String CLICKHOUSE_URL ;
    public static String CLICKHOUSE_USERNAME ;
    public static String CLICKHOUSE_PASSWORD ;
    public static String CLICKHOUSE_TABLENAME ;
    public static final String CLICKHOUSE_DRIVER = "ru.yandex.clickhouse.ClickHouseDriver";
    public static String FSSTATEBACKEND ;

    public HttpConf(ParameterTool propertiesargs ){
        APP_ID = propertiesargs.get("app_id");
        APP_SECRET = propertiesargs.get("app_secret");
        APP_TOKEN = propertiesargs.get("app_token");
        TABLE_ID = propertiesargs.get("table_id");
        VIEW_ID = propertiesargs.get("view_id");
        HTTP_FILTER = propertiesargs.get("http_filter");
        HTTP_URL = propertiesargs.get("http_url");
        CLICKHOUSE_URL = propertiesargs.get("clickhouse_url");
        CLICKHOUSE_USERNAME = propertiesargs.get("clickhouse_username");
        CLICKHOUSE_PASSWORD = propertiesargs.get("clickhouse_password");
        CLICKHOUSE_TABLENAME = propertiesargs.get("clickhouse_tablename");
        FSSTATEBACKEND = propertiesargs.get("fs_stateBackend");
    }
}

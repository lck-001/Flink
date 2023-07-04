package conf;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.yaml.snakeyaml.Yaml;
import util.SchemaUtil;

import java.io.*;
import java.net.URI;
import java.util.Map;
import java.util.Set;


public class HttpConf {
    public static Map CONFIG_MAP;
    public static Map BASE_MAP;
    public static Map SOURCE_MAP;
    public static Map SINK_MAP;
    public static Map SINK_CLICKHOUSE_MAP;

    public static String JOB_NAME = "FeiShuMultidimensionalTable";
    public static String SOURCE_TYPE = "http";
    public static Integer CHECKPOINT_TIME = 6000;
    public static Integer CHECKPOINT_PAUSE = 5000 ;
    public static Integer CHECKPOINT_TIMEOUT = 120000;
    public static String CHECKPOINT_PATH;
    public static Integer MAX_CONCURRENT_CHECKPOINT = 1;
    public static Integer TOLERABLE_CHECKPOINT_FAILURE_NUMBER;
    public static Integer RESTART_ATTEMPTS;
    public static Integer RESTART_DELAY_INTERVAL;


    public static String APP_ID;
    public static String APP_SECRET;
    public static String HTTP_URL;
    public static String APP_TOKEN;
    public static String TABLE_ID;
    public static String VIEW_ID;
//    public static String APP_ID = "cli_a24e455d667b500c";
//    public static String APP_SECRET = "OMBShEjrNTINIphrIZ8mphWOSAdf1ES2";
//    public static String HTTP_URL = "https://open.feishu.cn/open-apis/bitable/v1/apps/";
//    public static String APP_TOKEN = "bascnzY7kmzeDb15yLIEIbGWNud";
//    public static String TABLE_ID = "tbl0TT803viYHbBh";
//    public static String VIEW_ID = "vew6W7btMg";
    public static String HTTP_FILTER;
    public static String ACCESS_TOKEN;
    public static String TABLE_SCHEMA;

    public static String CLICKHOUSE_URL;
    public static String CLICKHOUSE_USERNAME;
    public static String CLICKHOUSE_PASSWORD;
    public static String CLICKHOUSE_DATABASE;
    public static String CLICKHOUSE_TABLENAME;
    public static String CLICKHOUSE_SCHEMA;
    public static String TRANSFORM_SQL;

    public HttpConf(String conf_path) throws Exception {
        try {
            Yaml y = new Yaml();

            if (conf_path!=null && conf_path.contains("hdfs")){
                //读取hdfs上yml文件
                Configuration conf = new Configuration();
                org.apache.hadoop.fs.FileSystem fs = org.apache.hadoop.fs.FileSystem.get(URI.create(conf_path), conf);
                InputStream wrappedStream = fs.open(new Path(conf_path)).getWrappedStream();
                BufferedReader bf = new BufferedReader(new InputStreamReader(wrappedStream, "UTF-8"));
                /* 读取 */
                CONFIG_MAP = (Map) y.load(bf);
            }else {
                //创建file对象
                File file = new File(conf_path);
                //将yaml内容解析成map表
                CONFIG_MAP = (Map) y.load(new FileInputStream(file));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        BASE_MAP = (Map) CONFIG_MAP.get("base");

        //BASE_MAP
        JOB_NAME = (String) BASE_MAP.get("jobName");
        SOURCE_TYPE = (String) BASE_MAP.get("sourceType");
        CHECKPOINT_TIME = (Integer) BASE_MAP.getOrDefault("checkpointTime", 6000L);
        CHECKPOINT_PAUSE = (Integer) BASE_MAP.getOrDefault("checkpointPause", 5000);
        CHECKPOINT_TIMEOUT = (Integer) BASE_MAP.getOrDefault("checkpointTimeout", 120000);
        CHECKPOINT_PATH = (String) BASE_MAP.get("checkpointPath");
        MAX_CONCURRENT_CHECKPOINT = (Integer) BASE_MAP.getOrDefault("maxConcurrentCheckpoint", 1);
        TOLERABLE_CHECKPOINT_FAILURE_NUMBER = (Integer) BASE_MAP.getOrDefault("tolerableCheckpointFailureNumber", 3);
        RESTART_ATTEMPTS = (Integer) BASE_MAP.getOrDefault("restartAttempts", 3);
        RESTART_DELAY_INTERVAL = (Integer) BASE_MAP.getOrDefault("restartdelayInterval", 10000);


        //SOURCE_MAP
        SOURCE_MAP = (Map) CONFIG_MAP.get("source");
        APP_ID = (String) SOURCE_MAP.get("appId");
        APP_SECRET = (String) SOURCE_MAP.get("appSecret");
        HTTP_URL = (String) SOURCE_MAP.get("httpUrl");
        APP_TOKEN = (String) SOURCE_MAP.get("appToken");
        TABLE_ID = (String) SOURCE_MAP.get("tableId");
        VIEW_ID = (String) SOURCE_MAP.get("viewId");
        HTTP_FILTER = (String) SOURCE_MAP.get("httpFilter");

        StringBuilder sourceBuilder = new StringBuilder();
        Map sourceTableSchema = (Map) SOURCE_MAP.get("tableSchema");
        Map sourceSchema = (Map) sourceTableSchema.get("schema");
        Set<Map.Entry<String, String>> sourceSchemaSet = sourceSchema.entrySet();
        for (Map.Entry<String, String> entry : sourceSchemaSet) {
            sourceBuilder.append("`" + entry.getKey() + "` " + entry.getValue() + ",");
        }
        TABLE_SCHEMA = sourceBuilder.substring(0, sourceBuilder.lastIndexOf(","));

        //SINK_CLICKHOUSE_MAP
        SINK_MAP = (Map) CONFIG_MAP.get("sink");
        SINK_CLICKHOUSE_MAP = (Map) SINK_MAP.get("sinkClickhouse");

//        StringBuilder sinkBuilder = new StringBuilder();
//        Map sinkTableSchema = (Map) SINK_CLICKHOUSE_MAP.get("tableSchema");
//        Map sinkschema = (Map) sinkTableSchema.get("schema");
//        Set<Map.Entry<String, String>> sinkSchemaSet = sinkschema.entrySet();
//        for (Map.Entry<String, String> entry : sinkSchemaSet) {
//            sinkBuilder.append("`" + entry.getKey() + "` " + entry.getValue() + ",");
//        }
//        CLICKHOUSE_SCHEMA = sinkBuilder.substring(0, sinkBuilder.lastIndexOf(","));
        CLICKHOUSE_URL = (String) SINK_CLICKHOUSE_MAP.get("JDBCUrl");
        CLICKHOUSE_DATABASE = (String) SINK_CLICKHOUSE_MAP.get("sinkDatabase");
        CLICKHOUSE_TABLENAME = (String) SINK_CLICKHOUSE_MAP.get("sinkTable");
        CLICKHOUSE_USERNAME = (String) SINK_CLICKHOUSE_MAP.get("userName");
        CLICKHOUSE_PASSWORD = (String) SINK_CLICKHOUSE_MAP.get("passWord");
        CLICKHOUSE_SCHEMA = SchemaUtil.getSchema(CLICKHOUSE_URL,CLICKHOUSE_USERNAME,CLICKHOUSE_PASSWORD,CLICKHOUSE_DATABASE,CLICKHOUSE_TABLENAME);
        TRANSFORM_SQL = (String) SINK_MAP.get("transformSql");
    }
}

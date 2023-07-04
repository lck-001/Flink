package app;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

public class HttpSourceTestApp {
    public static void main(String[] args) {
        //TODO 1.获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        StreamTableEnvironment tenv = StreamTableEnvironment.create(env);

        //创建source_table
        tenv.executeSql("create table source_table( " +
                "    `data` ROW<items ARRAY<ROW<fields ROW<" +
                "      `年龄` STRING," +
                "      `性别` STRING," +
                "      `位置` ROW<`address` STRING,`adname` STRING,`cityname` STRING>," +
                "      `人员` ROW<`name` STRING,`email` STRING> ," +
                "      `日期` STRING>>>>  " +
                ") with (" +
                "    'connector' = 'http'" +
                "    ,'http.url' = 'https://open.feishu.cn/open-apis/bitable/v1/apps/'" +
                "    ,'http.interval' = '1000000'" +
                "    ,'http.appId' = 'cli_a24e455d667b500c'" +
                "    ,'http.appSecret' = 'OMBShEjrNTINIphrIZ8mphWOSAdf1ES2'" +
                "    ,'http.appToken' = 'bascnWUJdESV7t41tdWUbypVIwf'" +
//                "    ,'http.httpFilter' = 'CurrentValue.[日期]>=TODAY()'" +
                "    ,'http.httpFilter' = 'null'" +
                "    ,'http.tableId' = 'tblyGnqoFUxAcwYe'" +
                "    ,'http.viewId' = 'vew90i6ABt'" +
                "    ,'format' = 'json'" +
                ")");

        //创建sink_table
        tenv.executeSql("create table sinkTable (" +
                "   age string, " +
                "   sex string, " +
                "   adname string, " +
                "   cityname  string, " +
                "   create_user string, " +
                "   user_email string, " +
                "   event_time  string " +
                ")with(" +
                "   'connector' = 'clickhouse', " +
                "   'url' = 'jdbc:clickhouse://10.11.33.163:8125', " +
                "   'database-name' = 'ceph_meta', " +
                "   'table-name' = 'dwd_feishu_table_data_test', " +
                "   'username' = 'chengkang'," +
                "   'password' = 'chengkang123'" +
//                "   'format' = 'json'" +
                ")");

        //数据转换
        String insert_sql = "select\n" +
                "    `年龄` as age,\n" +
                "    `性别` as sex,\n" +
                "    `位置`.`adname` as adname,\n" +
                "    `位置`.`cityname` as cityname,\n" +
                "    `人员`.`name` as create_user,\n" +
                "    `人员`.`email` as user_email,\n" +
                "    `日期` as  event_time\n" +
                "     from source_table CROSS JOIN UNNEST(items) AS t (fields)";

        tenv.executeSql(" insert into sinkTable " + insert_sql);
        tenv.executeSql( insert_sql).print();
    }
}

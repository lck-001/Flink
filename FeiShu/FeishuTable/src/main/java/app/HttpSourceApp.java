package app;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

public class HttpSourceApp {
    public static void main(String[] args) {
        //TODO 1.获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        StreamTableEnvironment tenv = StreamTableEnvironment.create(env);

        //创建source_table
        tenv.executeSql("create table source_table( " +
                "    `data` ROW<items ARRAY<ROW<fields ROW<" +
                "                                          `Robot ID` STRING," +
                "                                          `姓名` ROW<`name` STRING,`email` STRING>," +
                "                                          `时间` BIGINT," +
                "                                          `TTS及现场声音`  STRING," +
                "                                          `租户` STRING," +
                "                                          `账号名称` STRING," +
                "                                          `Hari状态` STRING," +
                "                                          `前端屏幕显示显示` STRING," +
                "                                          `远程重启操作是否恢复` STRING," +
                "                                          `异常情况著名jira编号` STRING," +
                "                                          `备注` STRING>>>> " +
                ") with (" +
                "    'connector' = 'http'" +
                "    ,'http.url' = 'https://open.feishu.cn/open-apis/bitable/v1/apps/'" +
                "    ,'http.interval' = '1000000'" +
                "    ,'http.appId' = 'cli_a24e455d667b500c'" +
                "    ,'http.appSecret' = 'OMBShEjrNTINIphrIZ8mphWOSAdf1ES2'" +
                "    ,'http.appToken' = 'bascnzY7kmzeDb15yLIEIbGWNud'" +
//                "    ,'http.httpFilter' = 'CurrentValue.[日期]>=TODAY()'" +
                "    ,'http.httpFilter' = 'null'" +
                "    ,'http.tableId' = 'tbl0TT803viYHbBh'" +
                "    ,'http.viewId' = 'vew6W7btMg'" +
                "    ,'format' = 'json'" +
                ")");

        //创建sink_table
//        tenv.executeSql("create table sinkTable (" +
//                "   robot_id string, " +
//                "   create_user string, " +
//                "   user_email string, " +
//                "   event_time  bigint, " +
//                "   tts_status string, " +
//                "   tenant_code string, " +
//                "   user_name string, " +
//                "   harix_status string, " +
//                "   screen_status  string, " +
//                "   recover_status  string, " +
//                "   jira  string, " +
//                "   remark  string, " +
//                "   robot_type  string " +
//                ")with(" +
//                "   'connector' = 'clickhouse', " +
//                "   'url' = 'jdbc:clickhouse://10.11.33.163:8125', " +
//                "   'database-name' = 'ceph_meta', " +
//                "   'table-name' = 'dwd_service_virtual_data_test', " +
//                "   'username' = 'chengkang'," +
//                "   'password' = 'chengkang123'," +
//                "   'format' = 'json'" +
//                ")");


        String insert_sql = "select " +
                "fields.`Robot ID` as robot_id, " +
                "fields.`姓名`.`name` as create_user," +
                "fields.`姓名`.`email` as user_email," +
                "fields.`时间`/1000 as  event_time," +
                "fields.`TTS及现场声音` as tts_status, " +
                "fields.`租户`  as tenant_code, " +
                "fields.`账号名称`  as user_name, " +
                "fields.`Hari状态`   as harix_status," +
                "fields.`前端屏幕显示显示`   as screen_status," +
                "fields.`远程重启操作是否恢复`   as recover_status," +
                "fields.`异常情况著名jira编号`   as jira," +
                "fields.`备注` as remark," +
                "'virtual' as robot_type " +
                //CROSS JOIN UNNEST 处理数组 列转多行
                " from source_table CROSS JOIN UNNEST(items) AS t (fields)";
        //数据转换
//        tenv.executeSql("insert into sinkTable " +insert_sql);;
        tenv.executeSql(insert_sql).print();;
    }
}

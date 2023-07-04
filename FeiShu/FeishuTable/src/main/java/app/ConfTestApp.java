package app;

import conf.HttpConf;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.concurrent.TimeUnit;

import static conf.HttpConf.*;

public class ConfTestApp {
    public static void main(String[] args) throws Exception {
        //TODO 1.获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        ParameterTool propertiesargs = ParameterTool.fromArgs(args);
        String fileName = propertiesargs.get("http_conf_path");
        new HttpConf(fileName);

        env.enableCheckpointing(CHECKPOINT_TIME);// 每 ** ms 开始一次 checkpoint
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);// 设置模式为精确一次
        env.getCheckpointConfig().setCheckpointTimeout(CHECKPOINT_TIMEOUT);// Checkpoint 必须在** ms内完成，否则就会被抛弃
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(MAX_CONCURRENT_CHECKPOINT);// 同一时间只允许一个 checkpoint 进行
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(CHECKPOINT_PAUSE);// 确认 checkpoints 之间的时间会进行 ** ms
        env.getCheckpointConfig().setExternalizedCheckpointCleanup(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(TOLERABLE_CHECKPOINT_FAILURE_NUMBER);
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(RESTART_ATTEMPTS, Time.of(RESTART_DELAY_INTERVAL, TimeUnit.MILLISECONDS)));//重启策略：重启3次，间隔10s
//        env.setStateBackend(new FsStateBackend(CHECKPOINT_PATH));

        StreamTableEnvironment tenv = StreamTableEnvironment.create(env);

        //创建source_table
        String source_sql = "create table source_table( " +
                "    `data` ROW<items ARRAY<ROW<fields ROW< " + TABLE_SCHEMA + " >>>> " +
                ") with (" +
                "    'connector' = '" + SOURCE_TYPE + "'" +
                "    ,'http.url' = '" + HTTP_URL + "'" +
                "    ,'http.appId' = '" + APP_ID + "'" +
                "    ,'http.appSecret' = '" + APP_SECRET + "'" +
                "    ,'http.appToken' = '" + APP_TOKEN + "'" +
                "    ,'http.tableId' = '" + TABLE_ID + "'" +
                "    ,'http.viewId' = '" + VIEW_ID + "'" +
                "    ,'http.httpFilter' = '" + HTTP_FILTER + "'" +
                "    ,'http.interval' = '10'" +
                "    ,'format' = 'json'" +
                ")";
        tenv.executeSql(source_sql);

        //创建sink_table
        String sink_sql =  "create table sinkTable (" + CLICKHOUSE_SCHEMA +
                "      )with(" +
                "      'connector' = 'clickhouse'," +
                "      'url' = '" + CLICKHOUSE_URL + "'," +
                "      'database-name' = '" + CLICKHOUSE_DATABASE + "'," +
                "      'table-name' = '" + CLICKHOUSE_TABLENAME + "'," +
                "      'username' = '" + CLICKHOUSE_USERNAME + "'," +
                "      'password' = '" + CLICKHOUSE_PASSWORD + "'," +
                "      'format' = 'json'" +
                "      )";
        System.out.println("sink_sql: "+sink_sql);
        tenv.executeSql(sink_sql);

        //数据转换
//        String insert_sql= "insert into sinkTable " +  TRANSFORM_SQL;
        String insert_sql=  TRANSFORM_SQL;
        tenv.executeSql(insert_sql).print();
    }
}

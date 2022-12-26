package app;

import bean.EventLog;
import com.alibaba.fastjson.JSON;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import utils.ClickHouseUtil;
import utils.MqttConsumer;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import static conf.MqttConfig.*;

public class MqttApiApp {
    public static void main(String[] args) throws Exception {
        //TODO 1.获取流执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        ParameterTool parameterargs = ParameterTool.fromArgs(args);
        env.getConfig().setGlobalJobParameters(parameterargs);
        ExecutionConfig.GlobalJobParameters parametersargs = env.getConfig().getGlobalJobParameters();
        Map<String, String> map = parametersargs.toMap();
        CLICKHOUSE_URL = map.get("ckhost");
        CLICKHOUSE_USERNAME = map.get("ckusername");
        CLICKHOUSE_PASSWORD = map.get("ckpassword");
        String FSSTATEBACKEND = map.get("fspath");

        //TODO　2.检查点设置
        env.setStateBackend(new FsStateBackend(FSSTATEBACKEND));// checkpoint存储位置
        env.enableCheckpointing(10000);// 每 ** ms 开始一次 checkpoint
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);// 设置模式为精确一次
        env.getCheckpointConfig().setCheckpointTimeout(100000);// Checkpoint 必须在** ms内完成，否则就会被抛弃
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(2);// 同一时间只允许一个 checkpoint 进行
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(3000);// 确认 checkpoints 之间的时间会进行 ** ms
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(5);// 允许chechpoint失败次数
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.of(10, TimeUnit.SECONDS)));//重启策略：重启3次，间隔10s


        String username = "system_bigdata";
        String password = "3881a684835683b2b6c3a52bdb09b49081136d05";
        String msgTopic = "bigdata/sv/inspection_event";
        String clientId = "mqtt";
        String hostUrl231 = "tcp://172.16.31.96:30671";
        String hostUrl251 = "tcp://172.16.31.28:31673";
        String hostUrl85 = "tcp://172.16.23.85:30231";
        //TODO 3.从MQTT多源获取数据
        DataStream<String> stream231 = env.addSource(new MqttConsumer(username,password,hostUrl231,clientId,msgTopic));
        DataStream<String> stream251 = env.addSource(new MqttConsumer(username,password,hostUrl251,clientId,msgTopic));
        DataStream<String> stream85 = env.addSource(new MqttConsumer(username,password,hostUrl85,clientId,msgTopic));

        //TODO　4.JSON-->EventLog，并指定数据来源
        SingleOutputStreamOperator<EventLog> log85DS = stream85.map(line -> {
            EventLog cephNginxLog = JSON.parseObject(line, EventLog.class);
            cephNginxLog.k8s_env_name = "bj-uit-85";
            return cephNginxLog;
        });
        SingleOutputStreamOperator<EventLog> log231DS = stream231.map(line -> {
            EventLog cephNginxLog = JSON.parseObject(line, EventLog.class);
            cephNginxLog.k8s_env_name = "bj-prod-231";
            return cephNginxLog;
        });
        SingleOutputStreamOperator<EventLog> log251DS = stream251.map(line -> {
            EventLog cephNginxLog = JSON.parseObject(line, EventLog.class);
            cephNginxLog.k8s_env_name = "bj-prod-251";
            return cephNginxLog;
        });

        //TODO 5.多流合并
        DataStream<EventLog> unionDS = log85DS.union(log231DS,log251DS);

        //TODO　6.将合并后的流输出到clickhouse
        unionDS.addSink(
                ClickHouseUtil.<EventLog>getJdbcSink("insert into harix.dwd_service_inspection_app_event" +
                        "(event_type_id,event_name,event_time,model_id,tenant_id,option,event_id,rcu_id,robot_id,robot_type,agent_id,asr_type,tts_type,start_time,cost_time,asr_totalcounts,asr_errorcounts,nlp_totalcounts,nlp_errorcounts,asr_info,nlp_info,k8s_env_name)" +
                        " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"))
        .setParallelism(6);

        env.execute();
    }
}

package com.data.http.app;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.data.http.bean.Inspection;
import com.data.http.conf.HttpConf;
import com.data.http.utlis.ClickHouseUtil;
import com.data.http.utlis.HttpUtils;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.data.http.conf.HttpConf.*;

public class HttpInspectionApp {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        ParameterTool propertiesargs = ParameterTool.fromArgs(args);
        String fileName = propertiesargs.get("http_conf_path");
        //从hdfs获取动态参数配置文件
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        org.apache.hadoop.fs.FileSystem fs = org.apache.hadoop.fs.FileSystem.get(URI.create(fileName), conf);
        InputStream wrappedStream = fs.open(new Path(fileName)).getWrappedStream();
        //防止中文乱码
        Properties props = new Properties();
        BufferedReader bf = new BufferedReader(new InputStreamReader(wrappedStream,"UTF-8"));
        props.load(bf);
        ParameterTool parameters = ParameterTool.fromMap((Map) props);
        //提升全局变量
        env.getConfig().setGlobalJobParameters(parameters);
        new HttpConf(parameters);

        //2.设置CK&状态后端
        env.setStateBackend(new FsStateBackend(FSSTATEBACKEND));
        env.enableCheckpointing(10000);// 每 ** ms 开始一次 checkpoint
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);// 设置模式为精确一次
        env.getCheckpointConfig().setCheckpointTimeout(100000);// Checkpoint 必须在** ms内完成，否则就会被抛弃
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(2);// 同一时间只允许一个 checkpoint 进行
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(3000);// 确认 checkpoints 之间的时间会进行 ** ms
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(5);
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.of(10, TimeUnit.SECONDS)));//重启策略：重启3次，间隔10s

        SingleOutputStreamOperator<Inspection> flatMap = env.addSource(new HttpUtils()).map(JSON::parseObject)
                .flatMap(new FlatMapFunction<JSONObject, Inspection>() {
                    @Override
                    public void flatMap(JSONObject jsonObject, Collector<Inspection> collector) throws Exception {
                        Inspection inspection = new Inspection();
                        JSONArray items = jsonObject.getJSONObject("data").getJSONArray("items");
                        for (int i = 0; i < items.toArray().length; i++) {
                            inspection.robot_id = items.getJSONObject(i).getJSONObject("fields").getString("Robot唯一标识").trim();
                            inspection.user_email = items.getJSONObject(i).getJSONObject("fields").getJSONObject("创建人").getString("email").trim();
                            inspection.create_user = items.getJSONObject(i).getJSONObject("fields").getJSONObject("创建人").getString("name").trim();
                            inspection.electric_status = items.getJSONObject(i).getJSONObject("fields").getString("本体电量").trim();
                            inspection.slam_status = items.getJSONObject(i).getJSONObject("fields").getString("检查定位和机器人点位（是否正常）").trim();
                            inspection.video_status = items.getJSONObject(i).getJSONObject("fields").getString("检查语音、视频是否正常").trim();
                            inspection.user_id = items.getJSONObject(i).getJSONObject("fields").getString("账户ID（例如：HF-FCYY-0138）账户ID（例如：HF-FCYY-0138）").trim();
                            inspection.tenant_name = items.getJSONObject(i).getJSONObject("fields").getString("项目名称").trim();
                            inspection.exception_info = items.getJSONObject(i).getJSONObject("fields").getString("是否有任务异常（CROSS系统查看任务记录异常挂起，任务未执行，发起失败，任务进行中）").trim();
                            inspection.user_name = items.getJSONObject(i).getJSONObject("fields").getString("子项目（账户名称：例如：         泉州台商方舱医院-GL-0149-3-1-1 ）").trim();
                            inspection.network_status = items.getJSONObject(i).getJSONObject("fields").getString("检查网络连接是否正常（机器人本身是连接4G网，结果却连的是WIFI，反之亦然）").trim();
                            inspection.ccu_status = items.getJSONObject(i).getJSONObject("fields").getString("检查是否出现CCU断联").trim();
                            inspection.event_time = (items.getJSONObject(i).getJSONObject("fields").getLong("巡检时间"))/1000;
                            inspection.open_status = items.getJSONObject(i).getJSONObject("fields").getString("是否开机").trim();
                            inspection.alarm_info = items.getJSONObject(i).getJSONObject("fields").getString("未处理的告警信息/事件").trim();
                            inspection.remark = items.getJSONObject(i).getJSONObject("fields").getString("备注").trim();
                            collector.collect(inspection);
                        }
                    }
                });

        flatMap.addSink(
                ClickHouseUtil.<Inspection>getJdbcSink("insert into "+CLICKHOUSE_TABLENAME +
                        "(event_time,create_user,tenant_name,user_name,user_id,robot_id,open_status,exception_info,network_status,video_status,ccu_status,slam_status,alarm_info,electric_status,remark,user_email,robot_type)" +
                        " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"));

        env.execute();
    }
}

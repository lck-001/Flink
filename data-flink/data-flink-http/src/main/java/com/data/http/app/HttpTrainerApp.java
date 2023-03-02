package com.data.http.app;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.data.http.bean.Trainer;
import com.data.http.bean.Virtual;
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

import java.io.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.data.http.conf.HttpConf.CLICKHOUSE_TABLENAME;
import static com.data.http.conf.HttpConf.FSSTATEBACKEND;

public class HttpTrainerApp {
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

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String current_date = formatter.format(new Date());
        SingleOutputStreamOperator<Trainer> flatMap = env.addSource(new HttpUtils()).map(JSON::parseObject)
                .flatMap(new FlatMapFunction<JSONObject, Trainer>() {
                    @Override
                    public void flatMap(JSONObject jsonObject, Collector<Trainer> collector) throws Exception {
                        Trainer trainer = new Trainer();
                        JSONArray items = jsonObject.getJSONObject("data").getJSONArray("items");
                        for (int i = 0; i < items.toArray().length; i++) {

                            trainer.event_date = current_date;
                            trainer.project_id =  items.getJSONObject(i).getJSONObject("fields").getString("OA编号");
                            trainer.project_name = items.getJSONObject(i).getJSONObject("fields").getString("项目名称（OA）");
                            trainer.robot_id = items.getJSONObject(i).getJSONObject("fields").getString("ROBOT ID");
                            trainer.tenant_id = items.getJSONObject(i).getJSONObject("fields").getString("租户");
                            trainer.user_id = items.getJSONObject(i).getJSONObject("fields").getString("帐号 ID");
                            trainer.agent_id = items.getJSONObject(i).getJSONObject("fields").getString("Agent ID");
                            trainer.task_type = items.getJSONObject(i).getJSONObject("fields").getString("转运营任务-任务类型");
                            trainer.task_name = items.getJSONObject(i).getJSONObject("fields").getString("\uD83D\uDC2F转运营任务");
                            trainer.address = items.getJSONObject(i).getJSONObject("fields").getString("地址");
                            trainer.sales = items.getJSONObject(i).getJSONObject("fields").getString("销售");
                            trainer.customer_info = items.getJSONObject(i).getJSONObject("fields").getString("客户信息");
                            trainer.is_important_demo = items.getJSONObject(i).getJSONObject("fields").getString("是否重要演示");
                            trainer.update_time = items.getJSONObject(i).getJSONObject("fields").getString("更新日期");
                            if (trainer.update_time !=null) trainer.update_time =  formatter.format(Long.parseLong(trainer.update_time));       ;
                            trainer.support_time = items.getJSONObject(i).getJSONObject("fields").getString("项目支持时间");
                            trainer.project_type = items.getJSONObject(i).getJSONObject("fields").getString("项目类型");
                            trainer.status = items.getJSONObject(i).getJSONObject("fields").getString("项目状态");
                            trainer.robot_num = items.getJSONObject(i).getJSONObject("fields").getString("机器人数量");
                            trainer.robot_type = items.getJSONObject(i).getJSONObject("fields").getString("机器人类型");
                            trainer.robot_type_bi = items.getJSONObject(i).getJSONObject("fields").getString("机器人类型-大数据");
                            trainer.industry = items.getJSONObject(i).getJSONObject("fields").getString("行业");
                            trainer.env = items.getJSONObject(i).getJSONObject("fields").getString("环境");
                            trainer.remark = items.getJSONObject(i).getJSONObject("fields").getString("备注");
                            trainer.video_url = items.getJSONObject(i).getJSONObject("fields").getString("视频链接");

                            collector.collect(trainer);
                        }
                    }
                });

        flatMap.addSink(
                        ClickHouseUtil.<Virtual>getJdbcSink("insert into " + CLICKHOUSE_TABLENAME +
                                "(event_date,project_id,project_name,robot_id,tenant_id,user_id,agent_id,task_type,task_name,address,sales,customer_info,is_important_demo,update_time,support_time,project_type,status,robot_num,robot_type,robot_type_bi,industry,env,remark,video_url)" +
                                " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"));

        env.execute();
    }
}

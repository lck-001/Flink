package com.data.http.app;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.data.http.bean.Seats;
import com.data.http.conf.HttpConf;
import com.data.http.utlis.ClickHouseUtil;
import com.data.http.utlis.DateUtils;
import com.data.http.utlis.HttpTableUtils;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Map;
import java.util.Properties;

import static com.data.http.conf.HttpConf.CLICKHOUSE_TABLENAME;


public class HttpSeatsApp {
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

        env.addSource(new HttpTableUtils())
                .map(JSON::parseObject)
                .flatMap(new FlatMapFunction<JSONObject, Seats>() {
                    @Override
                    public void flatMap(JSONObject jsonObject, Collector<Seats> collector) throws Exception {
                        Seats robotRate = new Seats();
                        JSONArray values = jsonObject.getJSONObject("data").getJSONObject("valueRange").getJSONArray("values");
                        for (int i = 0; i <values.toArray().length ; i++) {
                            //获取到的date为距离1899-12-30的天数
                            String intValue = values.getJSONArray(i).getString(0).trim();
                            robotRate.robot_type = values.getJSONArray(i).getString(1).trim();
                            robotRate.seats_count = values.getJSONArray(i).getFloat(2);
                            robotRate.date = DateUtils.dateutils(Integer.parseInt(intValue));
                            collector.collect(robotRate);
                        }
                    }
                })
        .addSink(ClickHouseUtil.getJdbcSink("insert into "+ CLICKHOUSE_TABLENAME +
                "(date,robot_type,seats_count)" +
                "values(?,?,?)"));

        env.execute();
    }
}

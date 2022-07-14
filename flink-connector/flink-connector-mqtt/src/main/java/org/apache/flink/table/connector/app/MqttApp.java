package org.apache.flink.table.connector.app;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.connector.bean.EventLog;

public class MqttApp {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        tableEnv.executeSql("create table flink_test (event_type_id String,event_name String ) " +
                "WITH ('connector' = 'mqtt','hosturl' = 'tcp://IP:PORT','username' = '','password' = '','topic' = '','format' = 'json')");

        Table table= tableEnv.sqlQuery("SELECT event_type_id,event_name FROM flink_test");

        DataStream<Tuple2<Boolean, EventLog>> stream = tableEnv.toRetractStream(table, EventLog.class);
        SingleOutputStreamOperator<EventLog> returns = stream.map(line -> {
            System.out.println("id:"+line.f1.event_type_id+",name:"+line.f1.event_name);
            return new EventLog(line.f1.event_type_id,line.f1.event_name);
        });
        returns.print("stream====");

        System.out.println(table.getQueryOperation().getResolvedSchema().getColumnCount());
        table.printSchema();

        env.execute("MqttApp");

    }
}

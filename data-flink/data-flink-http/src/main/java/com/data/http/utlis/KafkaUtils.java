package com.data.http.utlis;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.io.Serializable;
import java.util.Properties;

public class KafkaUtils implements Serializable {



    private static Properties properties = new Properties();

    static {
        properties.setProperty("bootstrap.servers", "10.11.33.58:9092");
    }

    /**
     * 获取KafkaSource的方法
     *
     * @param topic   主题
     * @param groupId 消费者组
     */
    public static FlinkKafkaConsumer<String> getKafkaSource(String topic, String groupId) {
        //给配置信息对象添加配置项
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        //获取KafkaSource
        return new FlinkKafkaConsumer<String>(topic, new SimpleStringSchema(), properties);
    }
    public static FlinkKafkaConsumer<String> getKafkaSource(String topic) {
        //获取KafkaSource
        return new FlinkKafkaConsumer<String>(topic, new SimpleStringSchema(), properties);
    }

    /**
     * 获取KafkaSink的方法
     *
     * @param topic   主题
     */
    public static FlinkKafkaProducer<String> getKafkaSink(String topic,String sink_server) {
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, sink_server);
        return new FlinkKafkaProducer<String>(topic, new SimpleStringSchema(), properties);
    }
}

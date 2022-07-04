package com.data.ceph.utils;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.io.Serializable;
import java.util.Properties;

import static com.data.ceph.config.CephConfig.KAFKA_SOURCE_SERVER;

public class KafkaManyUtils implements Serializable {



    private static Properties properties = new Properties();

    /**
     * 获取KafkaSource的方法
     *
     * @param topic   主题
     * @param groupId 消费者组
     */
    public static FlinkKafkaConsumer<String> getKafkaSource(String servers,String topic, String groupId) {
        properties.setProperty("bootstrap.servers", servers);
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
//        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
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
    public static FlinkKafkaProducer<String> getKafkaSink(String topic) {
        return new FlinkKafkaProducer<String>(topic, new SimpleStringSchema(), properties);
    }


}

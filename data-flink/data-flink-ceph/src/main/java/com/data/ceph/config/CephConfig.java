package com.data.ceph.config;

import org.apache.flink.api.common.ExecutionConfig;

import java.io.IOException;
import java.util.Map;

public class CephConfig {

    public static String HBASE_SERVER ;
    public static String KAFKA_SOURCE_SERVER ;
    public static String CLICKHOUSE_URL ;
    public static String CLICKHOUSE_USERNAME ;
    public static String CLICKHOUSE_PASSWORD ;
    public static String CLICKHOUSE_TABLENAME ;
    public static final String CLICKHOUSE_DRIVER = "ru.yandex.clickhouse.ClickHouseDriver";
    public static String IPFILE_PATH ;
    public static String FSSTATEBACKEND ;
    public static String KAFKA_SINK_SERVER ;
    public static String KAFKA_SOURCE_TOPIC ;
    public static String KAFKA_SOURCE_GROUP ;
    public static String KAFKA_SINK_TOPIC ;
    public static String HDFS_FILE_PATH ;

    public CephConfig(ExecutionConfig.GlobalJobParameters parameters) throws IOException {
        Map<String, String> map = parameters.toMap();
        HBASE_SERVER = map.get("hbase_zookeeper_quorum");
        KAFKA_SOURCE_SERVER = map.get("kafka_source_quorum");
        KAFKA_SOURCE_TOPIC = map.get("kafka_source_topic");
        KAFKA_SOURCE_GROUP = map.get("kafka_source_group");
        KAFKA_SINK_SERVER = map.get("kafka_sink_quorum");
        KAFKA_SINK_TOPIC = map.get("kafka_sink_topic");
        CLICKHOUSE_URL = map.get("clickhouse_url");
        CLICKHOUSE_USERNAME = map.get("clickhouse_username");
        CLICKHOUSE_PASSWORD = map.get("clickhouse_password");
        CLICKHOUSE_TABLENAME = map.get("clickhouse_tablename");
        IPFILE_PATH = map.get("ip_file_path");
        FSSTATEBACKEND = map.get("fs_stateBackend");
        HDFS_FILE_PATH = map.get("hdfs_file_path");
    }
}

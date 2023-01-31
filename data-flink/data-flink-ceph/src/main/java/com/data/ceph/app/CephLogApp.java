package com.data.ceph.app;

import com.alibaba.fastjson.JSON;
import com.data.ceph.bean.CephAccessLog;
import com.data.ceph.bean.CephAccessRecord;
import com.data.ceph.bean.CephNginxLog;
import com.data.ceph.config.CephConfig;
import com.data.ceph.function.DimAsyncFunction;
import com.data.ceph.utils.*;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.sink.filesystem.BucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.apache.hadoop.fs.FileSystem;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.data.ceph.config.CephConfig.*;

public class CephLogApp {


    public static void main(String[] args) throws Exception {
        //1.获取流执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        //设置动态参数
        ParameterTool propertiesargs = ParameterTool.fromArgs(args);
        String fileName = propertiesargs.get("CephConfPath");
        //从hdfs获取动态参数配置文件
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        FileSystem fs = FileSystem.get(URI.create(fileName), conf);
        fs.open(new org.apache.hadoop.fs.Path(fileName));
        ParameterTool propertiesFile = ParameterTool.fromPropertiesFile(fs.open(new org.apache.hadoop.fs.Path(fileName)).getWrappedStream());
        // 注册给环境变量(HBASE使用)
        env.getConfig().setGlobalJobParameters(propertiesFile);
        new CephConfig(propertiesFile);

        //2.设置CK&状态后端
        env.setStateBackend(new FsStateBackend(FSSTATEBACKEND));
        env.enableCheckpointing(10000);// 每 ** ms 开始一次 checkpoint
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);// 设置模式为精确一次
        env.getCheckpointConfig().setCheckpointTimeout(100000);// Checkpoint 必须在** ms内完成，否则就会被抛弃
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(2);// 同一时间只允许一个 checkpoint 进行
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(3000);// 确认 checkpoints 之间的时间会进行 ** ms
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(5);
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.of(10,TimeUnit.SECONDS)));//重启策略：重启3次，间隔10s

        //3.从kafka中读取日志信息,将将每行数据转换为JavaBean对象 主流
        DataStreamSource<String> dataStream = env.addSource(KafkaUtils.getKafkaSource(KAFKA_SOURCE_TOPIC, KAFKA_SOURCE_GROUP));

        //4.将json数据格式化成CephNginxLog
        SingleOutputStreamOperator<CephNginxLog> cephNginxLogDS = dataStream.map(line -> {
            //去除json串带的 ‘@’ 符号
            String s = StringSplitUtils.deleteCharString6(line, '@');
            CephNginxLog cephNginxLog = JSON.parseObject(s, CephNginxLog.class);
            return cephNginxLog;
        });

        //5.将CephNginxLog转换成cephAccesslog，
        SingleOutputStreamOperator<CephAccessLog> cephAccesslogDS = cephNginxLogDS.map(new MapFunction<CephNginxLog, CephAccessLog>() {
            @Override
            public CephAccessLog map(CephNginxLog cephNginxLog) throws Exception {
                CephNginxLog.CephRequestHeader request_header_info = JSON.parseObject(cephNginxLog.fields.request_header, CephNginxLog.CephRequestHeader.class);
                CephAccessLog cephAccessLog = new CephAccessLog();
                if (request_header_info != null && "".equals(request_header_info.authorization)) {
                    if (cephNginxLog.fields.request.contains("AWSAccessKeyId=")) {
                        cephAccessLog.access_key = cephNginxLog.fields.request.split("AWSAccessKeyId=", 2)[1].replaceAll("&.*", "");
                    }
                    if (cephNginxLog.fields.request.contains("AWSAccessKeyId%3D")) {
                        cephAccessLog.access_key = cephNginxLog.fields.request.split("AWSAccessKeyId%3D", 2)[1].replaceAll("%.*", "");
                    }
                }
                Pattern pattern = Pattern.compile("^AWS (.+):.*|^AWS4-HMAC-SHA256 Credential=(.*?)/");
                if (request_header_info != null) {
                    if (request_header_info.authorization != null) {
                        Matcher s = pattern.matcher(request_header_info.authorization);
                        //使用正则式匹配字符串
                        if (s.find()) {
                            cephAccessLog.access_key = s.group(1) == null ? s.group(2) : s.group(1);
                        }
                    }
                    cephAccessLog.dit_ip = request_header_info.host;
                    cephAccessLog.file_size = request_header_info.content_length;
                }
                cephAccessLog.event_time = cephNginxLog.timestamp.replace("T", " ").replace("+08:00", "");
                cephAccessLog.body_bytes_sent = cephNginxLog.fields.body_bytes_sent;
                if (cephNginxLog.fields.request.split("/").length >= 3) {
                    cephAccessLog.bucket = cephNginxLog.fields.request.split("/")[1];
                    cephAccessLog.object_id = cephNginxLog.fields.request.split("/", 3)[2].replaceAll("\\s.*|\\?.*", "");
                    if (cephAccessLog.object_id.indexOf(".") > 0) {
                        cephAccessLog.file_type = cephAccessLog.object_id.replaceFirst(".*\\.", "").toLowerCase();
                    } else {
                        cephAccessLog.file_type = "None";
                    }
                }
                if ("GET".equals(cephNginxLog.fields.request_method) || "HEAD".equals(cephNginxLog.fields.request_method)) {
                    cephAccessLog.event_type = "download";
                } else if ("DELETE".equals(cephNginxLog.fields.request_method)) {
                    cephAccessLog.event_type = "delete";
                } else {
                    cephAccessLog.event_type = "upload";
                }
                cephAccessLog.src_ip = cephNginxLog.fields.remote_addr;
                cephAccessLog.src_user = cephNginxLog.fields.remote_user;
                cephAccessLog.request_time = cephNginxLog.fields.request_time;
                cephAccessLog.status = cephNginxLog.fields.status;
                cephAccessLog.request_path = cephNginxLog.fields.request;
                switch (cephNginxLog.fields.request_method) {
                    case "POST":
                        if (cephAccessLog.request_path.contains("?uploads ")) {
                            cephAccessLog.part_status = 0;
                        } else if (cephAccessLog.request_path.contains("?uploadId=")) {
                            cephAccessLog.part_status = 2;
                        }
                        break;
                    case "PUT":
                        if (cephAccessLog.request_path.contains("partNumber=")) {
                            cephAccessLog.part_status = 1;
                            cephAccessLog.part_num = 1;
                        } else {
                            cephAccessLog.part_status = 3;
                        }
                        break;
                    case "GET":
                        cephAccessLog.part_status = 3;
                        break;
                    case "HEAD":
                        cephAccessLog.part_status = 3;
                        break;
                    case "DELETE":
                        cephAccessLog.part_status = 3;
                        break;
                }
                //根据hdfs上的ip解析文件解析IP地址
                if (cephAccessLog.src_ip != null) {
                    IPEntity msg = IPUtils.getIPMsg(cephAccessLog.src_ip);
                    if ("中华民国".equals(msg.getCountryName())) {
                        cephAccessLog.country = "中国";
                        cephAccessLog.city = "台湾";
                    } else {
                        cephAccessLog.country = msg.getCountryName();
                        cephAccessLog.city = msg.getCityName();
                    }
                }
                return cephAccessLog;
            }
        }).filter(cephAccessLog -> cephAccessLog != null && cephAccessLog.event_time.length() > 0);

        //6.转换ceph日志的part_state
        OutputTag<CephAccessLog> uploadingDS = new OutputTag<CephAccessLog>("uploading") {};
        SingleOutputStreamOperator<CephAccessLog> partStateDS = cephAccesslogDS.process(new ProcessFunction<CephAccessLog, CephAccessLog>() {
            @Override
            public void processElement(CephAccessLog value, Context ctx, Collector<CephAccessLog> out) throws Exception {
                if (value.event_type.equals("upload")) {
                    ctx.output(uploadingDS, value);
                } else {
                    out.collect(value);
                }
            }
        });
        //将状态<3的进行合并
        DataStream<CephAccessLog> uploadDS = partStateDS.getSideOutput(uploadingDS);
        SingleOutputStreamOperator<CephAccessLog> uploadedDS = uploadDS
                //添加watermark
                .assignTimestampsAndWatermarks(WatermarkStrategy.<CephAccessLog>forBoundedOutOfOrderness(Duration.ofSeconds(10))
                        .withTimestampAssigner(new SerializableTimestampAssigner<CephAccessLog>() {
                            @Override
                            public long extractTimestamp(CephAccessLog cephAccessLog, long l) {
                                long time = 0L;
                                try {
                                    Date parse = formatter.parse(cephAccessLog.event_time);
                                    time = parse.getTime();
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                return time;
                            }
                        }))
                .keyBy(log -> log.object_id)
                .process(new KeyedProcessFunction<String, CephAccessLog, CephAccessLog>() {
                    private ValueState<Long> timerState; //定时器时间
                    private ValueState<CephAccessLog> cephState; //定时器时间

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        timerState = getRuntimeContext().getState(new ValueStateDescriptor<Long>("timerState", Long.class));
                        cephState = getRuntimeContext().getState(new ValueStateDescriptor<CephAccessLog>("cephState", CephAccessLog.class));
                    }

                    @Override
                    public void processElement(CephAccessLog cephAccessLog, Context ctx, Collector<CephAccessLog> out) throws Exception {
                        if (cephState.value() == null) {
                            cephState.update(cephAccessLog);
                        } else {
                            CephAccessLog log = cephState.value();
                            log.part_num = log.part_num + cephAccessLog.part_num;
                            log.file_size = log.file_size + cephAccessLog.file_size;
                            log.part_status = log.part_status >= cephAccessLog.part_status ? log.part_status : cephAccessLog.part_status;
                            if (log.event_time == null) {
                                log.event_time = cephAccessLog.event_time;
                            } else {
                                log.event_time = cephAccessLog.event_time.compareTo(log.event_time) > 0 ? cephAccessLog.event_time : log.event_time;
                            }
                            cephState.update(log);
                        }
                        //根据状态值添加定时任务
                        long eventTime = formatter.parse(cephAccessLog.event_time).getTime();
//                        long processingTime = ctx.timerService().currentProcessingTime();
                        if (timerState.value()!=null && timerState.value()>0){
//                            ctx.timerService().deleteProcessingTimeTimer(timerState.value());
                            ctx.timerService().deleteEventTimeTimer(timerState.value());
                        }
                        if (cephAccessLog.part_status == 2) {
//                            timerState.update(processingTime + 60000L);
                            timerState.update(eventTime + 60000L);
                        } else if (cephAccessLog.part_status < 2) {
//                            timerState.update(processingTime + 1800000L);
                            timerState.update(eventTime + 1800000L);
                        }else {
                            BeanUtils.copyProperties(cephAccessLog,cephState.value());
                            out.collect(cephAccessLog);
                            cephState.clear();
                            timerState.clear();
                        }
                        if (timerState.value()!=null && timerState.value()>0) {
                            ctx.timerService().registerEventTimeTimer(timerState.value());
//                            ctx.timerService().registerProcessingTimeTimer(timerState.value());
                        }
                    }

                    @Override
                    public void onTimer(long timestamp, OnTimerContext ctx, Collector<CephAccessLog> out) throws Exception {
                        CephAccessLog cephAccessLog = new CephAccessLog();
                        BeanUtils.copyProperties(cephAccessLog,cephState.value());
                        if (cephState.value().part_status == 2) {
                            cephAccessLog.part_status = 3;
                        }
                        out.collect(cephAccessLog);
                        cephState.clear();
                        timerState.clear();
                    }
                });

        //将合并后的流和主流合并进行后续处理
        DataStream<CephAccessLog> unionDS = partStateDS.union(uploadedDS);


        //7.将更新后的数据转换成CephAccessRecord
        SingleOutputStreamOperator<CephAccessRecord> cephRecordDS = unionDS.map(
                new MapFunction<CephAccessLog, CephAccessRecord>() {
                    CephAccessRecord cephAccessRecord = new CephAccessRecord();

                    @Override
                    public CephAccessRecord map(CephAccessLog cephAccessLog) throws Exception {
                        BeanUtils.copyProperties(cephAccessRecord, cephAccessLog);
                        return cephAccessRecord;
                    }
                }
        );


        OutputTag<CephAccessRecord> invalidTag = new OutputTag<CephAccessRecord>("invalid-json"){};
        SingleOutputStreamOperator<CephAccessRecord> validDS = cephRecordDS.process(new ProcessFunction<CephAccessRecord, CephAccessRecord>() {
            @Override
            public void processElement(CephAccessRecord value, Context ctx, Collector<CephAccessRecord> out) throws Exception {
                if (value.access_key == null || "".equals(value.access_key)) {
                    ctx.output(invalidTag, value);
                } else {
                    out.collect(value);
                }
            }
        });

        //8.读取HBase中user表，进行维度关联
        SingleOutputStreamOperator<CephAccessRecord> record = AsyncDataStream.unorderedWait(
                validDS,
                new DimAsyncFunction<CephAccessRecord>() {
                    @Override
                    public String getKey(CephAccessRecord record) {
                        return record.access_key;
                    }
                },
                60, TimeUnit.SECONDS);
//        record.print("record:"+record);


        DataStream<CephAccessRecord> sideOutput = validDS.getSideOutput(invalidTag);
        DataStream<CephAccessRecord> allDataDS = record.union(sideOutput);

        //9.将record发送至clickhouse
        allDataDS.addSink(
                ClickHouseUtil.<CephAccessRecord>getJdbcSink("insert into "+CLICKHOUSE_TABLENAME +
                        "(s3_user_id,display_name,suspended,max_buckets,op_mask,default_placement,default_storage_class,placement_tags,src_ip,src_user,country,city,request_time,body_bytes_sent,status,bucket,object_id,event_time,event_type,request_path,dit_ip,access_key,file_type,file_size,part_num,part_status)" +
                        " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"))
                .setParallelism(10);


        BucketAssigner<String, String> assigner = new DateTimeBucketAssigner<>("yyyy-MM-dd", ZoneId.of("Asia/Shanghai"));
        StreamingFileSink<String> fileSink = StreamingFileSink.<String>forRowFormat(
                new Path(HDFS_FILE_PATH),
                new SimpleStringEncoder<>("UTF-8"))
                .withRollingPolicy(
                        DefaultRollingPolicy.builder()
                                .withRolloverInterval(TimeUnit.DAYS.toMillis(1))//至少包含 20 分钟的数据
                                .withInactivityInterval(TimeUnit.DAYS.toMillis(1 ))//最近 20 分钟没有收到新的数据
                                .withMaxPartSize(1024 * 1024 * 1024)//文件大小已达到 1 GB
                                .build())
                .withBucketAssigner(assigner)
                .build();

        // 将record-->过滤上传数据-->转换成jsonstring-->写入到hdfs
        allDataDS.filter(log->log.event_type.equals("upload")).map(line->JSON.toJSONString(line)).addSink(fileSink);

        //10.流环境执行
        env.execute();
    }
}

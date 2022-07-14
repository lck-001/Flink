package org.apache.flink.table.connector.source;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.table.data.RowData;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.fusesource.mqtt.client.BlockingConnection;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.apache.flink.table.connector.sourceT.MqttDynamicTableSourceFactory.*;
import static org.apache.flink.table.connector.source.MqttDynamicTableSourceFactory.CLIENTID;


public class MqttSourceFunction extends RichSourceFunction<RowData>{
    private BlockingConnection blockingConnection;
    private ReadableConfig confs;
    //阻塞队列存储订阅的消息
    private BlockingQueue<RowData> queue = new ArrayBlockingQueue<RowData>(10);
    //存储服务
    private  MqttClient client;
    //存储订阅主题
    private MqttTopic mqttTopic;
    private DeserializationSchema<RowData> deserializer;

    public MqttSourceFunction(ReadableConfig options, DeserializationSchema<RowData> deserializer) {
        this.confs = options;
        this.deserializer = deserializer;
    }

//    @Override
//    public TypeInformation<String> getProducedType() {
//        return
//    }
    @Override
    public void open(Configuration parameters) throws Exception {
    }



    @Override
    public void cancel() {
        try {
            blockingConnection.disconnect();
        } catch (Throwable t) {
            // ignore
        }
    }


    //包装连接的方法
    private void connect() throws MqttException {
        //连接mqtt服务器
        client = new MqttClient(confs.get(HOSTURL), "mqtt85", new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        options.setUserName(confs.get(USERNAME));
        options.setPassword(confs.get(PASSWORD).toCharArray());
        options.setCleanSession(false);   //是否清除session
        // 设置超时时间
        options.setConnectionTimeout(30);
        // 设置会话心跳时间
        options.setKeepAliveInterval(20);
        try {
            String[] topics = confs.get(TOPIC).split(",");
            //订阅消息
            int[] qos = new int[topics.length];
            for (int i = 0; i < topics.length; i++) {
                qos[i] = 0;
            }
            client.setCallback(new MsgCallback(client, options, topics, qos) {
            });
            client.connect(options);
            client.subscribe(topics, qos);
            System.out.println("MQTT连接成功:" + confs.get(CLIENTID) + ":" + client);
        } catch (Exception e) {
            System.out.println("MQTT连接异常：" + e);
        }
    }

    //实现MqttCallback，内部函数可回调
    class MsgCallback implements MqttCallback {
        private MqttClient client;
        private MqttConnectOptions options;
        private String[] topic;
        private int[] qos;

        public MsgCallback() {
        }

        public MsgCallback(MqttClient client, MqttConnectOptions options, String[] topic, int[] qos) {
            this.client = client;
            this.options = options;
            this.topic = topic;
            this.qos = qos;
        }

        //连接失败回调该函数
        @Override
        public void connectionLost(Throwable throwable) {
            System.out.println("MQTT连接断开，发起重连");
            while (true) {
                try {
                    Thread.sleep(1000);
                    client.connect(options);
                    //订阅消息
                    client.subscribe(topic, qos);
                    System.out.println("MQTT重新连接成功:" + client);
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }

        //收到消息回调该函数
        @Override
        public void messageArrived(String s, MqttMessage message) throws Exception {
            System.out.println();
            //订阅消息字符
            queue.put(deserializer.deserialize(message.getPayload()));

        }

        //对象转化为字节码
        public byte[] getBytesFromObject(Serializable obj) throws Exception {
            if (obj == null) {
                return null;
            }
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);
            return bo.toByteArray();
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

        }
    }

    //flink线程启动函数
    @Override
    public void run(SourceContext<RowData>  ctx) throws Exception {
        connect();
        //利用死循环使得程序一直监控主题是否有新消息
        while (true) {
            //使用阻塞队列的好处是队列空的时候程序会一直阻塞到这里不会浪费CPU资源
            ctx.collect(queue.take());
        }
    }
}

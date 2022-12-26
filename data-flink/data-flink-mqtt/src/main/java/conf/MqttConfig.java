package conf;

import java.io.Serializable;

//该类需要实现序列化所以必须实现Serializable接口
public class MqttConfig implements Serializable {

    public static String CLICKHOUSE_URL ;
    public static String CLICKHOUSE_USERNAME ;
    public static String CLICKHOUSE_PASSWORD ;
    public static final String CLICKHOUSE_DRIVER = "ru.yandex.clickhouse.ClickHouseDriver" ;

}

package com.data.ceph.function;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.security.User;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.security.UserGroupInformation;

import java.util.Collections;
import java.util.Map;

public abstract class DimAsyncFunction<T> extends RichAsyncFunction<T, T> implements DimAsyncJoinFunction<T> {

    private org.apache.hadoop.hbase.client.Connection connection = null;
    private ResultScanner rs = null;
    private Table table = null;

    @Override
    public void open(Configuration parameters) throws Exception {
        //不启用安全认证
        System.setProperty("zookeeper.sasl.client", "false");
        Map<String, String> stringStringMap = getRuntimeContext().getExecutionConfig().getGlobalJobParameters().toMap();
        String hbase = stringStringMap.get("hbase_zookeeper_quorum");
        org.apache.hadoop.conf.Configuration hconf = HBaseConfiguration.create();
        hconf.set(HConstants.ZOOKEEPER_QUORUM, hbase);
        hconf.set(HConstants.ZOOKEEPER_CLIENT_PORT, "2181");
        hconf.set(HConstants.ZOOKEEPER_ZNODE_PARENT, "/hbase");

        //指定用户名为hbase的用户去访问hbase服务
        UserGroupInformation userGroupInformation = UserGroupInformation.createRemoteUser("hive");
        connection = ConnectionFactory.createConnection(hconf, User.create(userGroupInformation));
        table = connection.getTable(TableName.valueOf("cloud:user_info"));
    }


    @Override
    public void asyncInvoke(T input, ResultFuture<T> resultFuture) throws Exception {
        Get get = new Get(Bytes.toBytes(getKey(input)));
        Result rs = table.get(get);
        for (Cell cell : rs.rawCells()) {
            String column = Bytes.toString(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength());
            String value = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
            BeanUtils.setProperty(input, column, value);
        }
        resultFuture.complete(Collections.singletonList(input));
    }
    @Override
    public void close() throws Exception {
        if (rs != null) rs.close();
        if (table != null) table.close();
        if (connection != null) connection.close();
    }
    @Override
    public void timeout(T input, ResultFuture<T> resultFuture) throws Exception {
        System.out.println("TimeOut:" + input);
    }
}

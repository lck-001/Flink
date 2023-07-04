package util;


import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class SchemaUtil {
    public static void main(String[] args) throws Exception {
        // 注册 JDBC 驱动
        Class.forName("ru.yandex.clickhouse.ClickHouseDriver");

        String DB_URL = "jdbc:clickhouse://10.11.33.163:8125/ceph_meta";
//        sinkDatabase: ceph_meta
//        sinkTable: dwd_service_virtual_data_test
        String USER = "chengkang";
        String PASS = "chengkang123";

        // 打开链接
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
//        testTableCol(conn,USER, USER);
    }

    public static String getSchema(String DB_URL, String USER, String PASS, String DATA_BASE, String TABLE_NAME) throws Exception {
        Class.forName("ru.yandex.clickhouse.ClickHouseDriver");
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        return testTableCol(conn,DATA_BASE,TABLE_NAME);
    }


    public static String testTableCol(Connection connection, String DATA_BASE, String TABLE_NAME) throws SQLException {
        List<String> list = new ArrayList<>();

        DatabaseMetaData databaseMetaData = connection.getMetaData();
        //具体能返回哪些字段 可查询注释
        //返回test数据库下所有的列表
        ResultSet columns = databaseMetaData.getColumns(DATA_BASE, null, TABLE_NAME, null);
        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            String typeName = columns.getString("TYPE_NAME");
            if (typeName != null && typeName.equals("DateTime64(3)") || typeName.equals("DateTime"))
                typeName = "bigint";
            if (columnName!=null && "insert_time".equals(columnName)) continue;
            System.out.println(columnName + ": " + typeName.toLowerCase());
            String row = columnName + " " + typeName.toLowerCase();
            list.add(row);
        }
        String cloums = StringUtils.join(list, ",");
        return cloums;
    }
}

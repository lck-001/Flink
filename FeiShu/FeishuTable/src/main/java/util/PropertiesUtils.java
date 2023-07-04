package util;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class PropertiesUtils {

    public static Map CONFIG_MAP;
    public static void  getPropertiesMap(String filePath) throws FileNotFoundException {
        try {
            Yaml y = new Yaml();
            //创建file对象
            //将yaml内容解析成map表
            CONFIG_MAP = (Map) y.load(new FileInputStream(new File(filePath)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void  getHdfsPropertiesMap(String filePath) throws FileNotFoundException {
        try {
            Yaml y = new Yaml();
            //创建file对象
            //将yaml内容解析成map表

            Configuration conf = new Configuration();
            FileSystem fs = FileSystem.get(URI.create(filePath), conf);
            FSDataInputStream open = fs.open(new Path(filePath));
            ParameterTool propertiesFile = ParameterTool.fromPropertiesFile(open.getWrappedStream());

            CONFIG_MAP = (Map) y.load(open);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        getHdfsPropertiesMap("hdfs://nameservice1/tmp/flink/c0004_t_login_seats.yaml");
    }
}
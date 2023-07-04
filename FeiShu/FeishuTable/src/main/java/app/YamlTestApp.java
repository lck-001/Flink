package app;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.yaml.snakeyaml.Yaml;

import java.net.URI;

import java.io.*;
import java.util.Map;
import java.util.Properties;

public class YamlTestApp {

    public static void main(String[] args) throws IOException {
        try {
            Map m1, m2, m3, m4;
            FileWriter fw;


            Configuration conf = new Configuration();
            org.apache.hadoop.fs.FileSystem fs = org.apache.hadoop.fs.FileSystem.get(URI.create("fileName"), conf);
            InputStream wrappedStream = fs.open(new Path("fileName")).getWrappedStream();
            BufferedReader bf = new BufferedReader(new InputStreamReader(wrappedStream, "UTF-8"));
            /* 读取 */
            Yaml y = new Yaml();
            m1 = (Map) y.load(bf);


            //创建file对象
            File file = new File("C:\\workspacebak\\FeiShu\\FeishuTable\\src\\main\\resources\\feishu.yml");
            //将yaml内容解析成map表
            m1 = (Map) y.load(new FileInputStream(file));
            //获取第一级键中的“details”键作为对象，进一步获取下级的键和值
            m2 = (Map) m1.get("details");
            m3 = (Map) m2.get("friends");
            //这里email键属于第三级，对其key进行赋值
            m3.put("email", "asdasd@gamil.com");
            //获取第三级键中“info”键
            m4 = (Map) m3.get("info");
            //将第四级键“tel”赋值2222
            m4.put("tcp", 2222);

            /* 写入 */
            //初始化filewriter对象，用于写入操作
            fw = new FileWriter(file);
            //用snakeyaml的dump方法将map类解析成yaml内容
            fw.write(y.dump(m1));
            //写入到文件中
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

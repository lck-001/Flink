package com.data.ceph.utils;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.util.Map;

import static com.data.ceph.config.CephConfig.IPFILE_PATH;


/**
 * @ClassName IPUtils
 * @Description TODO
 * @Author qgp
 * @Date 2019/9/24 17:51
 * @Version 1.0
 **/
public class IPUtils {
    /**
     * 全局静态变量，DatabaseReader，保证类加载时加载一次
     */
    private static DatabaseReader reader;



    /**
     * 静态代码块，保证项目启动只获取一次文件
     */
    static {
        try {
            //1.读取本地文件
//            File database = new File("C:\\workspace\\data-flink\\data-flink-ceph\\src\\main\\resources\\GeoLite2-City.mmdb");  //绝对路径读取文件方式
            //2.读取hdfs文件，通过 InputStream 流式读取文件，目的解决无法通过File方式读取jar包内的文件的问题
            InputStream database = getFile("hdfs://nameservice1/user/hive/spark_job_lib/cloud/GeoLite2-City.mmdb");
//            InputStream database = getFile(IPFILE_PATH);
            reader = new DatabaseReader.Builder(database).build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 解析IP
     * @param ip
     * @return
     */
    public static IPEntity getIPMsg(String ip){
        IPEntity msg = new IPEntity();

        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = reader.city(ipAddress);
            Country country = response.getCountry();
            Subdivision subdivision = response.getMostSpecificSubdivision();
            City city = response.getCity();
            Postal postal = response.getPostal();
            Location location = response.getLocation();

            msg.setCountryName(country.getNames().get("zh-CN"));
            msg.setCountryCode(country.getIsoCode());
            msg.setProvinceName(subdivision.getNames().get("zh-CN"));
            msg.setProvinceCode(subdivision.getIsoCode());
            msg.setCityName(city.getNames().get("zh-CN"));
            msg.setPostalCode(postal.getCode());
            //经度
            msg.setLongitude(location.getLongitude());
            //纬度
            msg.setLatitude(location.getLatitude());
        } catch (IOException e) {
            e.printStackTrace();
        }catch (AddressNotFoundException e){
            msg.setCountryName("私网地址");
            msg.setCityName("私网地址");
            return msg;
        }
        catch (GeoIp2Exception e) {
            e.printStackTrace();
        }
        return msg;
    }


    /**
     * 读取classpath下的文件
     * @param fileName 原文件全名
     * @return
     * @throws IOException
     */
    public static InputStream getFile(String fileName) throws IOException {
        //读取 ClassPath 路径下指定资源的输入流
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(URI.create(fileName), conf);
        fs.open(new Path(fileName));
        return fs.open(new Path(fileName)).getWrappedStream();
    }
}

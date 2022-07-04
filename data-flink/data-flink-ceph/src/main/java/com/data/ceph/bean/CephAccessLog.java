package com.data.ceph.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CephAccessLog implements Serializable {
    public String event_time=""; //事件时间
    public String src_ip=""; // 源IP
    public String src_user=""; // 访问用户
    public String country=""; // 源地址 所在国家
    public String city="";//源地址所在城市
    public double request_time=0; // 请求响应时间
    public long body_bytes_sent=0; // 请求响应body大小
    public int status=200; // 请求响应状态
    public String bucket=""; // 桶名
    public String object_id=""; // ObjectID
    public String event_type=""; //请求类型
    public String request_path=""; //请求路径
    public String dit_ip=""; // 响应机器ip
    public String user_agent="";
    public String access_key=""; //
    public long file_size=0; //文件大小
    public int part_num=0; //分片数
    public String file_type="";//文件类型
    public int part_status;//分片状态  start 0 /ing 1/end 2 /complete 3
}

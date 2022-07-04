package com.data.ceph.bean;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.Serializable;

@Data
public class CephNginxLog implements Serializable {

    @SerializedName("@timestamp")
    public String timestamp="";
    @SerializedName("@fields")
    public  Fields fields;

    public static class Fields implements Serializable{
        public String remote_addr="";
        public String remote_user="";
        public long body_bytes_sent;
        public double request_time;
        public int status;
        public String request="";
        public String request_method="";
        public String request_header="";
        public CephRequestHeader request_header_info;
    }

    public static class CephRequestHeader implements Serializable{
        public String host="";
        @SerializedName("accept-encoding")
        public String accept_encoding="";
        @SerializedName("content-type")
        public String content_type="";
        @SerializedName("x-amz-storage-class")
        public String x_amz_storage_class="";
        @SerializedName("content-length")
        public long content_length;
        @SerializedName("x-amz-meta-s3cmd-attrs")
        public String x_amz_meta_s3cmd_attrs="";
        public String authorization="";
    }
}

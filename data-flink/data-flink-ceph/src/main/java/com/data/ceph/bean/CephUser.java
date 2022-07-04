package com.data.ceph.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CephUser implements Serializable {
    public String access_key="";
    public String s3_user_id = "";
    public String display_name = "";
    public String suspended = "";
    public String max_buckets = "";
    public String op_mask = "";
    public String default_placement="";
    public String default_storage_class="";
    public String placement_tags="";
}

package com.data.http.bean;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Trainer {
    public String event_date;
    public String project_id;	//'OA编号'
    public String project_name;	//'OA项目名称'
    public String robot_id;	//'ROBOT ID
    public String tenant_id;	//'租户ID'
    public String user_id;	//'账号ID'
    public String agent_id;	//'AGNET ID
    public String task_type;	//'任务类型'
    public String task_name;	//'转运营任务'
    public String address;	//'地址'
    public String sales;	//'销售'
    public String customer_info;	//'客户信息'
    public String is_important_demo;	//'是否重要演示'
    public String update_time;	//'OA更新日期'
    public String support_time;	//'项目支持时间'
    public String project_type;	//'项目类型'
    public String status;	//'项目状态'
    public String robot_num;	//'机器人数量'
    public String robot_type;	//'机器人类型'
    public String robot_type_bi;	//'机器人类型-BI'
    public String industry;	//'行业'
    public String env;	//'环境'
    public String remark;	//'备注'
    public String video_url;	//'视频链接'
}

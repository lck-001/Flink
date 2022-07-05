package org.apache.flink.table.connector.bean;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventLog {
    public String event_type_id ;
    public String event_name ;

    public EventLog(String event_type_id,String event_name){
        this.event_type_id = event_type_id;
        this.event_name = event_name;
    }
}

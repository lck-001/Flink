package bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventLog {
    public String event_type_id ;
    public String event_name ;
    public String event_time ;
    public String model_id ;
    public String tenant_id ;
    public String option ;
    public String event_id ;
    public String rcu_id ;
    public String robot_id ;
    public String robot_type ;
    public int agent_id ;
    public String asr_type ;
    public String tts_type ;
    public String start_time ;
    public int cost_time ;
    public int asr_totalcounts ;
    public int asr_errorcounts ;
    public int nlp_totalcounts ;
    public int nlp_errorcounts ;
    public String asr_info ;
    public String nlp_info ;
    public String k8s_env_name ;
}

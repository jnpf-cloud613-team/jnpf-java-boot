package jnpf.flowable.model.trigger;

import jnpf.flowable.model.message.FlowMsgModel;
import jnpf.flowable.model.util.EventModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TriggerEventModel {
    private List<FlowMsgModel> msgList = new ArrayList<>();
    private List<EventModel> eventList = new ArrayList<>();
}

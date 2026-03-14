package jnpf.flowable.model.xml.process;

import com.alibaba.fastjson.annotation.JSONField;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Process {

    @XmlAttribute
    private String id = "Process_1";

    @XmlAttribute
    private Boolean isExecutable = true;

    @JSONField(name = "bpmn2:startEvent")
    @XmlElement(name = "bpmn2:startEvent")
    private Event startEvent;

    @JSONField(name = "bpmn2:userTask")
    @XmlElement(name = "bpmn2:userTask")
    private List<Task> userTask;

    @JSONField(name = "bpmn2:sequenceFlow")
    @XmlElement(name = "bpmn2:sequenceFlow")
    private List<Sequence> sequenceFlow;

    @JSONField(name = "bpmn2:endEvent")
    @XmlElement(name = "bpmn2:endEvent")
    private Event endEvent;

    @JSONField(name = "bpmn2:group")
    @XmlElement(name = "bpmn2:group")
    private List<Group> group;

    @JSONField(name = "bpmn2:inclusiveGateway")
    @XmlElement(name = "bpmn2:inclusiveGateway")
    private List<GateWay> inclusiveGateway;

}

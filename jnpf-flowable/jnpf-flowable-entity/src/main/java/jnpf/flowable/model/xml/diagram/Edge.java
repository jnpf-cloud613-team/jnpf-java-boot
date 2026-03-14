package jnpf.flowable.model.xml.diagram;

import com.alibaba.fastjson.annotation.JSONField;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Edge {

    @XmlAttribute
    private String id;

    @XmlAttribute
    private String bpmnElement;

    @JSONField(name = "bpmndi:BPMNLabel")
    @XmlElement(name = "bpmndi:BPMNLabel")
    private Label label;

    @JSONField(name = "di:waypoint")
    @XmlElement(name = "di:waypoint")
    private List<Waypoint> waypoint = new ArrayList<>();

}

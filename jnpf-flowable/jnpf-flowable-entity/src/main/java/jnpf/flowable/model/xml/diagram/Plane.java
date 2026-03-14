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
public class Plane {

    @XmlAttribute
    private String id = "BPMNPlane_1";

    @XmlAttribute
    private String bpmnElement = "Process_1";

    @JSONField(name = "bpmndi:BPMNShape")
    @XmlElement(name = "bpmndi:BPMNShape")
    private List<Shape> bpmnShape = new ArrayList<>();

    @JSONField(name = "bpmndi:BPMNEdge")
    @XmlElement(name = "bpmndi:BPMNEdge")
    private List<Edge> bpmnEdge = new ArrayList<>();

}

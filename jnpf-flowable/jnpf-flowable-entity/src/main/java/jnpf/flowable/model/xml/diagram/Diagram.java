package jnpf.flowable.model.xml.diagram;

import com.alibaba.fastjson.annotation.JSONField;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Diagram {

    @XmlAttribute
    private String id = "BPMNDiagram_1";

    @JSONField(name = "bpmndi:BPMNPlane")
    @XmlElement(name = "bpmndi:BPMNPlane")
    private Plane bpmnPlane = new Plane();

}

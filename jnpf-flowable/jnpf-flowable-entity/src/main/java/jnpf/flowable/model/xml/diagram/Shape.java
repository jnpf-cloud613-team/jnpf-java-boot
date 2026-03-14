package jnpf.flowable.model.xml.diagram;

import com.alibaba.fastjson.annotation.JSONField;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;


@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Shape {

    @XmlAttribute
    private String id;

    @XmlAttribute
    private String bpmnElement;

    @JSONField(name = "dc:Bounds")
    @XmlElement(name = "dc:Bounds")
    private Bounds bounds = new Bounds();
}

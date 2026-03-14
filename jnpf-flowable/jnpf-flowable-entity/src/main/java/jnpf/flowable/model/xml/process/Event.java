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
public class Event {

    @XmlAttribute
    private String id;

    @JSONField(name = "bpmn2:incoming")
    @XmlElement(name = "bpmn2:incoming")
    private List<String> incoming;

    @JSONField(name = "bpmn2:outgoing")
    @XmlElement(name = "bpmn2:outgoing")
    private List<String> outgoing;
}

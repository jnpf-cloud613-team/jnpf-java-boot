package jnpf.flowable.model.xml.process;

import com.alibaba.fastjson.annotation.JSONField;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;


@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Sequence {

    @XmlAttribute
    private String id;

    @XmlAttribute
    private String sourceRef;

    @XmlAttribute
    private String targetRef;

    @JSONField(name = "bpmn2:conditionExpression")
    @XmlElement(name = "bpmn2:conditionExpression")
    private Condition conditionExpression;

}

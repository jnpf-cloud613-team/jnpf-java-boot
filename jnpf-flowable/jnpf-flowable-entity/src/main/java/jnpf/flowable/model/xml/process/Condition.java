package jnpf.flowable.model.xml.process;

import com.alibaba.fastjson.annotation.JSONField;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Condition {

    @JSONField(name = "xsi:type")
    @XmlAttribute(name = "xsi:type")
    private String type = "bpmn2:tFormalExpression";

    @XmlValue
    private String content;
}

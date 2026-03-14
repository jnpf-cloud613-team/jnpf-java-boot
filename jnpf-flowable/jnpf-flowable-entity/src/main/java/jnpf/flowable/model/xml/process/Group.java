package jnpf.flowable.model.xml.process;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.Data;


@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Group {

    @XmlAttribute
    private String id;

}

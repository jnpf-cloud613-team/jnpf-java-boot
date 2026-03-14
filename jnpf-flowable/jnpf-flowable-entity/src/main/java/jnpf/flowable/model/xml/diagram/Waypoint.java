package jnpf.flowable.model.xml.diagram;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Waypoint {

    @XmlAttribute
    private String x = "400";

    @XmlAttribute
    private String y = "300";
}

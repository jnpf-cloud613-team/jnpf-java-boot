package jnpf.flowable.model.xml.diagram;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Bounds {

    @XmlAttribute
    private String x = "400";

    @XmlAttribute
    private String y = "300";

    @XmlAttribute
    private String width = "200";

    @XmlAttribute
    private String height = "88";
}

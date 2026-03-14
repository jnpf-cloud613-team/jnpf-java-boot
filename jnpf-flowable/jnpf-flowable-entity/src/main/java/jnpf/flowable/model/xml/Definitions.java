package jnpf.flowable.model.xml;

import com.alibaba.fastjson.annotation.JSONField;
import jnpf.flowable.model.xml.diagram.*;
import jakarta.xml.bind.annotation.*;
import jnpf.flowable.model.xml.process.Process;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "bpmn2:definitions")
public class Definitions {

    @JSONField(name = "xmlns:xsi")
    @XmlAttribute(name = "xmlns:xsi")
    private String xsi = "http://www.w3.org/2001/XMLSchema-instance";

    @JSONField(name = "xmlns:bpmn2")
    @XmlAttribute(name = "xmlns:bpmn2")
    private String bpmn2 = "http://www.omg.org/spec/BPMN/20100524/MODEL";

    @JSONField(name = "xmlns:bpmndi")
    @XmlAttribute(name = "xmlns:bpmndi")
    private String bpmndi = "http://www.omg.org/spec/BPMN/20100524/DI";

    @JSONField(name = "xmlns:dc")
    @XmlAttribute(name = "xmlns:dc")
    private String dc = "http://www.omg.org/spec/DD/20100524/DC";

    @JSONField(name = "xmlns:di")
    @XmlAttribute(name = "xmlns:di")
    private String di = "http://www.omg.org/spec/DD/20100524/DI";

    @XmlAttribute
    private String id = "sample-diagram";

    @XmlAttribute
    private String targetNamespace = "http://www.flowable.org/processdef";

    @JSONField(name = "xsi:schemaLocation")
    @XmlAttribute(name = "xsi:schemaLocation")
    private String schemaLocation = "http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd";

    @JSONField(name = "bpmn2:process")
    @XmlElement(name = "bpmn2:process")
    private Process process = new Process();

    @JSONField(name = "bpmndi:BPMNDiagram")
    @XmlElement(name = "bpmndi:BPMNDiagram")
    private Diagram bpmnDiagram = new Diagram();

}

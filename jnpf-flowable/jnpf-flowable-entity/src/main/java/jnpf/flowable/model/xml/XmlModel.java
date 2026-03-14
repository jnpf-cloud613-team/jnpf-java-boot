package jnpf.flowable.model.xml;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class XmlModel {

    @JSONField(name = "bpmn2:definitions")
    private Definitions definitions ;

    private String newNodeCode;

    private String newLineCode;
}

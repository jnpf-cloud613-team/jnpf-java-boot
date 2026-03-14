package jnpf.flowable.model.templatenode;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.flowable.model.template.TemplateCrForm;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TemplateNodeCrFrom extends TemplateCrForm {

    @Schema(description = "流程节点")
    private Map<String, Map<String, Object>> flowNodes = new HashMap<>();

    @Schema(description = "流程xml")
    private String flowXml;

}

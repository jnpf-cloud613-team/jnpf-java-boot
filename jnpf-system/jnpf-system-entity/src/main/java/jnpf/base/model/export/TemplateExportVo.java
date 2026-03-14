package jnpf.base.model.export;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class TemplateExportVo {
    @Schema(description = "流程定义")
    private Object template;

    @Schema(description = "流程定义版本")
    private Object flowVersion;

    @Schema(description = "流程节点")
    private List<Object> nodeList;
}

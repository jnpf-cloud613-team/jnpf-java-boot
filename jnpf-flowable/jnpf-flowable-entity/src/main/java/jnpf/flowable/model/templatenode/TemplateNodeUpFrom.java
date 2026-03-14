package jnpf.flowable.model.templatenode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TemplateNodeUpFrom extends TemplateNodeCrFrom {

    @Schema(description = "流程模板主键")
    private String id;

    @Schema(description = "流程版本主键")
    private String flowId;

    @Schema(description = "是否加版本")
    private Boolean isAddVersion = true;
}

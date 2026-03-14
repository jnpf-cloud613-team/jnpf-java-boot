package jnpf.flowable.model.templatejson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TemplateJsonSelectVO {
    @Schema(description = "流程状态")
    private Integer state;
    @Schema(description = "流程状态")
    private Integer enabledMark;
    @Schema(description = "流程主键")
    private String id;
    @Schema(description = "流程名称")
    private String fullName;
    @Schema(description = "流程名称")
    private String flowVersion;
}

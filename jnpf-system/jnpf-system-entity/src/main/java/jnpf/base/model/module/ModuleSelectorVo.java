package jnpf.base.model.module;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "菜单选择对象")
public class ModuleSelectorVo {
    @Schema(description = "菜单id")
    private String id;
    @Schema(description = "应用名称")
    private String systemName;
    @Schema(description = "菜单名称")
    private String fullName;
    @Schema(description = "菜单编码")
    private String enCode;
    @Schema(description = "类型")
    private Integer type;
    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "类型属性信息")
    private String propertyJson;
    @Schema(description = "表单id")
    private String formId;
    @Schema(description = "流程id")
    private String flowId;
}

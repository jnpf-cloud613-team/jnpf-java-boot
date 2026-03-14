package jnpf.permission.model.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class RoleCrForm {
    @NotBlank(message = "必填")
    @Schema(description = "角色名称")
    private String fullName;
    @Schema(description = "角色编码")
    private String enCode;
    @NotBlank(message = "必填")
    @Schema(description = "角色类型")
    private String type;
    @Schema(description = "说明")
    private String description;
    @Schema(description = "排序")
    private long sortCode;

    @Schema(description = "岗位约束(0-关闭，1启用)")
    private Integer isCondition;
    @Schema(description = "约束内容")
    private String conditionJson;
}

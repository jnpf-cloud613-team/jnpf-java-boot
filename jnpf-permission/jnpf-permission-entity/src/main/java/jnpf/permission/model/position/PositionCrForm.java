package jnpf.permission.model.position;

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
public class PositionCrForm {
    @NotBlank(message = "必填")
    @Schema(description = "所属组织(id)")
    private String organizeId;
    @Schema(description = "父级岗位")
    private String parentId;
    @NotBlank(message = "必填")
    @Schema(description = "岗位名称")
    private String fullName;
    @Schema(description = "岗位编码")
    private String enCode;
    @Schema(description = "说明")
    private String description;
    @Schema(description = "排序")
    private Long sortCode;

    @Schema(description = "岗位约束(0-关闭，1启用)")
    private Integer isCondition = 0;
    @Schema(description = "约束内容")
    private String conditionJson;
}

package jnpf.permission.model.organize;

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
public class OrganizeCrForm {
    @NotBlank(message = "公司上级不能为空")
    @Schema(description = "父级id")
    private String parentId;
    @NotBlank(message = "公司名称不能为空")
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;


    @Schema(description = "分类")
    private String category;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "排序")
    private Long sortCode;
}

package jnpf.permission.model.organize;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class OrganizeDepartCrForm {

    private String managerId;
    @NotBlank(message = "必填")
    @Schema(description = "上级ID")
    private String parentId;
    @NotBlank(message = "必填")
    @Schema(description = "部门名称")
    private String fullName;
    @NotBlank(message = "必填")
    @Schema(description = "部门编码")
    private String enCode;
    @Schema(description = "状态")
    private int enabledMark;
    private String description;
    @Schema(description = "排序")
    private Long sortCode;
}

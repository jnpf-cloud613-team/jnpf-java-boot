package jnpf.permission.model.usergroup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/11 9:28
 */
@Data
public class GroupForm {

    @Schema(description = "名称")
    @NotBlank(message = "名称不能为空")
    private String fullName;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "排序")
    private Long sortCode;

    @Schema(description = "说明")
    private String description;
}

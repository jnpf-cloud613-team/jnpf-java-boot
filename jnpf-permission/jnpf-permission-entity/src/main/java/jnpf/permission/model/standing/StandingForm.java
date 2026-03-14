package jnpf.permission.model.standing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 身份管理表单
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/4 18:38:01
 */
@Data
public class StandingForm {

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

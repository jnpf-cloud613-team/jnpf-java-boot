package jnpf.permission.model.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 加入企业表单
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/9/22 15:55:55
 */
@Data
@Schema(description = "加入企业")
public class TenantJoinForm {
    @Schema(description = "租户号")
    @NotBlank(message = "租户号不能为空")
    private String tenantId;

    @Schema(description = "企业名称")
    private String companyName;

    @Schema(description = "APP ID")
    @NotBlank(message = "APP ID不能为空")
    private String appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "您在该应用下的账号")
    @NotBlank(message = "您在该应用下的账号不能为空")
    private String account;
    @Schema(description = "您在该应用下的密码")
    @NotBlank(message = "您在该应用下的密码不能为空")
    private String password;
}

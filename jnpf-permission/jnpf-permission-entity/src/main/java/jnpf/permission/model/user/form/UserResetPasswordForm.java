package jnpf.permission.model.user.form;

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
public class UserResetPasswordForm {
    @NotBlank(message = "必填")
    @Schema(description = "用户id")
    private String id;
    @NotBlank(message = "必填")
    @Schema(description = "新密码，需要 MD5 加密后传输")
    private String userPassword;
    @NotBlank(message = "必填")
    @Schema(description = "重复新密码")
    private String validatePassword;
}

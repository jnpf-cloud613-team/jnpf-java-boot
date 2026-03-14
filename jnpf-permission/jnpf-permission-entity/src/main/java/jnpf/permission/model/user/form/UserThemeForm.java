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
public class UserThemeForm {
    @NotBlank(message = "必填")
    @Schema(description = "系统主题")
    private String theme;
}

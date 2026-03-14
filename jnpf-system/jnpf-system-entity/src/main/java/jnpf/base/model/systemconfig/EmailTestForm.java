package jnpf.base.model.systemconfig;

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
public class EmailTestForm {
    @NotBlank(message = "必填")
    @Schema(description = "邮箱地址")
    private String account;
    @NotBlank(message = "必填")
    @Schema(description = "邮箱密码")
    private String password;
    @NotBlank(message = "必填")
    @Schema(description = "POP3服务")
    private String pop3Host;
    @NotBlank(message = "必填")
    @Schema(description = "POP3端口")
    private Integer pop3Port;
    @NotBlank(message = "必填")
    @Schema(description = "SMTP服务")
    private String smtpHost;
    @NotBlank(message = "必填")
    @Schema(description = "SMTP端口")
    private Integer smtpPort;
    @Schema(description = "ssl登录")
    private String ssl	;
}

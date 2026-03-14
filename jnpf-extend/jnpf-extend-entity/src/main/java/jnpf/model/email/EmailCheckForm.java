package jnpf.model.email;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class EmailCheckForm {
    @NotBlank(message = "必填")
    @Schema(description ="POP3端口")
    private String pop3Port;
    @Schema(description ="ssl登录")
    private String emailSsl;
    @NotBlank(message = "必填")
    @Schema(description ="邮箱地址")
    private String account;
    @NotBlank(message = "必填")
    @Schema(description ="POP3服务")
    private String pop3Host;
    @NotBlank(message = "必填")
    @Schema(description ="邮箱密码")
    private String password;
    @NotBlank(message = "必填")
    @Schema(description ="SMTP服务")
    private String smtpHost;
    @NotBlank(message = "必填")
    @Schema(description ="SMTP端口")
    private String smtpPort;
    @NotBlank(message = "必填")
    @Schema(description ="显示名称")
    private String senderName;

}

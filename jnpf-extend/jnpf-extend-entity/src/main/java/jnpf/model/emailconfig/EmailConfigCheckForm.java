package jnpf.model.emailconfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 邮箱账户密码验证
 */
@Data
public class EmailConfigCheckForm {
    @Schema(description ="邮箱密码")
    private String password;

    @Schema(description ="显示名称")
    private String senderName;

    @Schema(description ="SMTP服务")
    private String smtpHost;

    @Schema(description ="POP3端口")
    private Integer pop3Port;

    @Schema(description ="主键id")
    private String id;

    @Schema(description ="SMTP端口")
    private Integer smtpPort;

    @Schema(description ="ssl登录")
    private Integer emailSsl;

    @Schema(description ="邮箱地址")
    private String account;

    @Schema(description ="POP3服务")
    private String pop3Host;
}

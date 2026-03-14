package jnpf.model.email;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EmailCofigInfoVO {

    @Schema(description ="账户")
    private String account;

    @Schema(description ="密码")
    private String password;

    @Schema(description ="POP3服务")
    private String pop3Host;

    @Schema(description ="POP3端口")
    private Integer pop3Port;

    @Schema(description ="发件人名称")
    private String senderName;

    @Schema(description ="SMTP服务")
    private String smtpHost;

    @Schema(description ="SMTP端口")
    private Integer smtpPort;
    @Schema(description ="创建时间")
    private long creatorTime;
    @Schema(description ="是否开户SSL登录(1-是,0否)")
    private Integer emailSsl;
}

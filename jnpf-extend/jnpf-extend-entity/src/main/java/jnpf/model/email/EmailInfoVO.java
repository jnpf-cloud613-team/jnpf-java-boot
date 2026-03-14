package jnpf.model.email;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EmailInfoVO{
    @Schema(description ="主键")
    private String id;
    @Schema(description ="邮件主题")
    private String subject;
    @Schema(description ="抄送人")
    private String cc;
    @Schema(description ="密送人")
    private String bcc;
    @Schema(description ="发件人姓名")
    private String senderName;
    @Schema(description ="发件人邮箱")
    private String sender;
    @Schema(description ="时间")
    private Long fdate;
    @Schema(description ="创建时间")
    private Long creatorTime;
    @Schema(description ="收件箱收件人")
    private String mAccount;
    @Schema(description ="发送收件人")
    private String recipient;
    @Schema(description ="附件对象")
    private String attachment;
    @Schema(description ="邮件内容")
    private String bodyText;
}

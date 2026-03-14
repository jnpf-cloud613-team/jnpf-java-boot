package jnpf.model.email;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EmailSentListVO {
    @Schema(description ="附件")
    private String attachment;
    @Schema(description ="发件人")
    private String id;
    @Schema(description ="主题")
    private String subject;
    @Schema(description ="收件人")
    private String recipient;
    @Schema(description ="创建时间")
    private Long creatorTime;
}

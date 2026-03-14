package jnpf.model.email;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * 存草稿
 */
@Data
public class EmailSendCrForm {
//    @JsonIgnore
    @Schema(description ="id",hidden = true)
    private String id;

    @Schema(description ="抄送人")
    private String cc;

    @Schema(description ="密送人")
    private String bcc;

    @Schema(description ="正文")
    private String bodyText;

    @Schema(description ="附件")
    private String attachment;

    @NotBlank(message = "必填")
    @Schema(description ="主题")
    private String subject;

    @NotBlank(message = "必填")
    @Schema(description ="收件人")
    private String recipient;

}

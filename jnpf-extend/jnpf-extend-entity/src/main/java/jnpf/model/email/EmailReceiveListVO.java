package jnpf.model.email;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class EmailReceiveListVO {
    @Schema(description ="是否已读(1-已读，0-未)")
    private Integer isRead;
    @Schema(description ="附件")
    private String attachment;
    @Schema(description ="时间")
    private Long fdate;
    @Schema(description ="发件人")
    private String id;
    @Schema(description ="是否标星(1-是,0-否)")
    private Integer starred;
    @Schema(description ="发件人")
    private String sender;
    @Schema(description ="主题")
    private String subject;
    @Schema(description ="创建时间")
    private Long creatorTime;
    @Schema(description ="收件人")
    private String recipient;

}

package jnpf.model.worklog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class WorkLogListVO {
    @Schema(description ="主键")
    private String id;
    @Schema(description ="标题")
    private String title;
    @Schema(description ="内容")
    private String question;
    @Schema(description ="创建时间")
    private long creatorTime;
    @Schema(description ="内容")
    private String todayContent;
    @Schema(description ="内容")
    private String tomorrowContent;
    @Schema(description ="用户主键")
    private String toUserId;
}

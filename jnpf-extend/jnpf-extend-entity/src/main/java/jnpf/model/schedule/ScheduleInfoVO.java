package jnpf.model.schedule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ScheduleInfoVO {
    @Schema(description ="日程主键")
    private String id;
    @Schema(description ="开始时间(时间戳)")
    private long startTime;
    @Schema(description ="结束时间(时间戳)")
    private long endTime;
    @Schema(description ="日程内容")
    private String content;
    @Schema(description ="提醒设置",example = "1")
    private Integer early;
    @Schema(description ="APP提醒(1-提醒，0-不提醒)",example = "1")
    private Integer appAlert;
    @Schema(description ="日程颜色")
    private String colour;
    @Schema(description ="颜色样式")
    private String colourCss;
    @Schema(description ="微信提醒(1-提醒，0-不提醒)",example = "1")
    private Integer weChatAlert;
    @Schema(description ="邮件提醒(1-提醒，0-不提醒)",example = "1")
    private Integer mailAlert;
    @Schema(description ="短信提醒(1-提醒，0-不提醒)",example = "1")
    private Integer mobileAlert;
}

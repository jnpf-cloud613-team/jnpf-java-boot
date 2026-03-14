package jnpf.model.schedule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


import jakarta.validation.constraints.NotNull;

@Data
public class ScheduleCrForm {
    @Schema(description ="开始时间")
    private long startTime;
    @NotNull(message = "必填")
    @Schema(description ="结束时间")
    private long endTime;
    @NotBlank(message = "必填")
    @Schema(description ="日程内容")
    private String content;
    @Schema(description ="提醒设置")
    private Integer early;
    @Schema(description ="APP提醒(1-提醒，0-不提醒)")
    private Integer appAlert;
    @Schema(description ="日程颜色")
    private String colour;
    @Schema(description ="颜色样式")
    private String colourCss;
    @Schema(description ="微信提醒(1-提醒，0-不提醒)")
    private Integer weChatAlert;
    @Schema(description ="邮件提醒(1-提醒，0-不提醒)")
    private Integer mailAlert;
    @Schema(description ="短信提醒(1-提醒，0-不提醒)")
    private Integer mobileAlert;

}

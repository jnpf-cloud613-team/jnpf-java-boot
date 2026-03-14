package jnpf.base.model.schedule;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 */
@Data
public class ScheduleNewListVO {
    @Schema(description ="主键")
    private String id;

    @Schema(description ="工作类型")
    private String category;

    @Schema(description ="紧急程度")
    private String urgent;

    @Schema(description ="标题")
    private String title;

    @Schema(description ="内容")
    private String content;

    @Schema(description ="是否全天")
    private Integer allDay;

    @Schema(description ="开始时间")
    private Long startDay;

    @Schema(description ="开始时间")
    private String startTime;

    @Schema(description ="结束时间")
    private Long endDay;

    @Schema(description ="结束时间")
    private String endTime;

    @Schema(description ="时长")
    private Integer duration;

    @Schema(description ="颜色")
    private String color;

    @Schema(description ="提醒时间")
    private Integer reminderTime;

    @Schema(description ="提醒方式")
    private Integer reminderType;

    @Schema(description ="发送配置")
    private String send;

    @Schema(description ="发送名称")
    private String sendName;

    @Schema(description ="重复提醒")
    private Integer repetition;

    @Schema(description ="结束重复")
    private Long repeatTime;

    @Schema(description ="创建人")
    private String creatorUserId;

}

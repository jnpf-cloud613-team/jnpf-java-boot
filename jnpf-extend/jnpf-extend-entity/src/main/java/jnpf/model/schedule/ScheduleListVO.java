package jnpf.model.schedule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ScheduleListVO{
    @Schema(description ="日程主键")
    private String id;
    @Schema(description ="开始时间")
    private long startTime;
    @Schema(description ="开始时间")
    private long endTime;
    @Schema(description ="颜色")
    private String colour;
    @Schema(description ="日程内容")
    private String content;
}

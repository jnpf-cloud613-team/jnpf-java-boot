package jnpf.base.model.schedule;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ScheduleNewTime {
    @Schema(description = "开始时间")
    private String startTime;
    @Schema(description = "结束时间")
    private String endTime;
    @Schema(description = "当前时间")
    private String dateTime;
}

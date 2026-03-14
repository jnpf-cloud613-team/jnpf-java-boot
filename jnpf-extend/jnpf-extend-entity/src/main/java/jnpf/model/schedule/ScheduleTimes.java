package jnpf.model.schedule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ScheduleTimes extends ScheduleTime {
    @Schema(description ="日期")
    private String dateTime;
}

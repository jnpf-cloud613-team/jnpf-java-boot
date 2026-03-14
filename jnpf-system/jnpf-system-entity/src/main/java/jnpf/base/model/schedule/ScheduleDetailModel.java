package jnpf.base.model.schedule;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ScheduleDetailModel {
    @Schema(description ="分组主键")
    private String groupId;
    @Schema(description ="主键")
    private String id;
    @Schema(description = "类型")
    private String type;
}

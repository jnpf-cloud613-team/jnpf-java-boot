package jnpf.base.model.flow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "流程状态参数")
public class FlowStateModel {
    @Schema(description = "表单ids")
    private List<String> formIds = new ArrayList<>();
    @Schema(description = "任务id")
    private String flowTaskId;
    @Schema(description = "流程状态")
    private Integer flowState;
}

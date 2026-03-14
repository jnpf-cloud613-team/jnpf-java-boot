package jnpf.onlinedev.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "错误信息对象")
public class VisualErrInfo {
    @Schema(description = "错误信息")
    private String errMsg;
    @Schema(description = "主键id")
    private String id;

    @Schema(description = "流程任务id")
    private String flowTaskId;
}

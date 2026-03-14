package jnpf.base.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "表单和流程id参数")
public class PrintDevParam {

    @NotBlank(message = "必填")
    @Schema(description = "表单id")
    private String formId;

    @Schema(description = "流程任务id")
    private String flowTaskId;
}

package jnpf.base.model.form;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "流程表单草稿模型")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DraftJsonModel {
    @Builder.Default
    @Schema(description = "是否必填")
    private Boolean required = false;
    @Schema(description = "字段id")
    private String fieldId;
    @Schema(description = "字段名称")
    private String fieldName;
    @Schema(description = "jnpfkey")
    private String jnpfKey;
    @Builder.Default
    @Schema(description = "是否多选")
    private boolean multiple = false;
    @Schema(description = "表名")
    private String tableName;
}

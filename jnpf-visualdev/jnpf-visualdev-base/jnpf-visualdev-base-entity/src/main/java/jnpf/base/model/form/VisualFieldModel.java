package jnpf.base.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(description = "字段模型")
@AllArgsConstructor
@NoArgsConstructor
public class VisualFieldModel {

    @Schema(description = "字段id")
    private String fieldId;

    @Schema(description = "字段名称")
    private String fieldName;

    @Schema(description = "字段jnpfkey")
    private String jnpfKey;

    @Schema(description = "表名")
    private String tableName;

    @Schema(description = "表名")
    private Boolean required = false;

    @Schema(description = "表名")
    private Boolean multiple = false;
}

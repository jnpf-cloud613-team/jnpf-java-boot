package jnpf.base.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "名称编码模型")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NameCodeModel {
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "名称")
    private String fullName;
}

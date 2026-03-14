package jnpf.model.document;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


import jakarta.validation.constraints.NotNull;

@Data
public class DocumentCrForm {
    @NotBlank(message = "必填")
    @Schema(description ="文件夹名称")
    private String fullName;
    @NotNull(message = "必填")
    @Schema(description ="文档分类")
    private Integer type;
    @NotBlank(message = "必填")
    @Schema(description ="文档父级")
    private String parentId;
}

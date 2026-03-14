package jnpf.model.tableexample;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TableExampleCityListVO {
    @Schema(description ="父级主键")
    private String id;
    @Schema(description ="名称")
    private String fullName;
    @Schema(description ="图标")
    private String enCode;
    @Schema(description ="父级主键")
    private String parentId;
}

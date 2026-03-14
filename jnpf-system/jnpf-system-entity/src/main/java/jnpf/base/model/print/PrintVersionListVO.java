package jnpf.base.model.print;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "打印版本")
public class PrintVersionListVO {
    @Schema(description = "状态")
    private Integer state;
    @Schema(description = "主键")
    private String id;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "版本")
    private String version;
}

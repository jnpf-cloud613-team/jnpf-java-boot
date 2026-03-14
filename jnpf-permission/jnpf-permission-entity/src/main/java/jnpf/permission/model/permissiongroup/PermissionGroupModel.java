package jnpf.permission.model.permissiongroup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "模型")
public class PermissionGroupModel implements Serializable {
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "排序")
    private Long sortCode;
    @Schema(description = "状态")
    private Integer enabledMark;
    @Schema(description = "说明")
    private String description;
    @Schema(description = "类型")
    private Integer type;
    @Schema(description = "主键")
    private String id;
}

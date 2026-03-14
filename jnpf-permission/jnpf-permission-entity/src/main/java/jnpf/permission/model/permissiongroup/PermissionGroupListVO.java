package jnpf.permission.model.permissiongroup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class PermissionGroupListVO implements Serializable {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "排序")
    private Long sortCode;
    @Schema(description = "说明")
    private String description;
    @Schema(description = "状态")
    private Integer enabledMark;
    @Schema(description = "权限成员")
    private String permissionMember;
    @Schema(description = "类型")
    private Integer type;
    @Schema(description = "创建时间")
    private Long creatorTime;
}

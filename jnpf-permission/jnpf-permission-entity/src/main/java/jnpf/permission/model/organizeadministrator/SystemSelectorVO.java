package jnpf.permission.model.organizeadministrator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class SystemSelectorVO implements Serializable {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "是否有权限")
    private Integer isPermission;


    private boolean disabled;

    private long sortCode;

    private long creatorTime;

}

package jnpf.base.model.module;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class ModulePermissionVO implements Serializable {

    @Schema(description = "按钮权限")
    private ModulePermissionModel buttonAuthorize;
    @Schema(description = "列表权限")
    private ModulePermissionModel columnAuthorize;
    @Schema(description = "表单权限")
    private ModulePermissionModel formAuthorize;
    @Schema(description = "数据权限")
    private ModulePermissionModel dataAuthorize;

    @Schema(description = "权限成员")
    private ModulePermissionModel permissionMember;

}

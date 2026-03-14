package jnpf.permission.model.permissiongroup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class ViewPermissionsModel implements Serializable {
    @NotNull(message = "对象主键不能为空")
    @Schema(description = "对象id")
    private String id;
    @Schema(description = "权限组id")
    private String permissionId;
    @NotNull(message = "对象类型不能为空")
    @Schema(description = "对象类型")
    private String objectType;
    @Schema(description = "权限类型")
    private String itemType;
}

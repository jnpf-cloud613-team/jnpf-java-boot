package jnpf.permission.model.permissiongroup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ViewPermissionsVO implements Serializable {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "下级菜单列表")
    private List<ViewPermissionsVO> children;
}

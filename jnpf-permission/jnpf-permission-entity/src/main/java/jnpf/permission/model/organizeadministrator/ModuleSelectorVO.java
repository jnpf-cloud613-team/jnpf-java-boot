package jnpf.permission.model.organizeadministrator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ModuleSelectorVO implements Serializable {

    private String id;
    private String fullName;
    private String enCode;
    private String parentId;
    private String icon;
    private Integer type;
    private Long sortCode;
    private String category;
    private String propertyJson;

    private String systemId;
    private Boolean hasModule;

    @Schema(description ="是否有下级菜单")
    private Boolean hasChildren;
    @Schema(description ="下级菜单列表")
    private List<ModuleSelectorVO> children;

    @Schema(description = "是否有权限")
    private Integer isPermission;

    private boolean disabled;
}

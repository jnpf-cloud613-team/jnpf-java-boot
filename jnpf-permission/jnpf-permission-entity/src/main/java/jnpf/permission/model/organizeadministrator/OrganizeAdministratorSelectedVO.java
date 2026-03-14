package jnpf.permission.model.organizeadministrator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrganizeAdministratorSelectedVO implements Serializable {

    @Schema(description = "组织权限集合")
    private List<OrganizeAdministratorSelectorVO> orgAdminList = new ArrayList<>();

    @Schema(description = "应用权限集合")
    private List<SystemSelectorVO> systemPermissionList = new ArrayList<>();

    @Schema(description = "菜单权限集合")
    private List<ModuleSelectorVO> modulePermissionList = new ArrayList<>();

    @Schema(description = "有菜单权限集合")
    private List<String> moduleIds = new ArrayList<>();

    @Schema(description = "有应用权限集合")
    private List<String> systemIds = new ArrayList<>();

    @Schema(description = "管理组")
    private String managerGroup;

}

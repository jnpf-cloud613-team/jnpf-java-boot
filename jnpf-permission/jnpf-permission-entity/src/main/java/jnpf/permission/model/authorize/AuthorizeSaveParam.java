package jnpf.permission.model.authorize;

import jnpf.permission.entity.AuthorizeEntity;
import jnpf.permission.entity.OrganizeEntity;
import jnpf.permission.entity.PositionEntity;
import jnpf.permission.entity.RoleRelationEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AuthorizeSaveParam {
    private String objectId;
    private String objectType;
    //全部组织
    private List<OrganizeEntity> allOrgList;
    //全部岗位
    private List<PositionEntity> allPosList;
    //全部权限
    private Map<String, List<AuthorizeEntity>> allAuthMap;
    //角色关系
    private List<RoleRelationEntity> roleRealationList;

    private List<String> systemSave;
    private List<String> moduleSave;
    private List<String> buttonSave;
    private List<String> columnSave;
    private List<String> resourceSave;
    private List<String> formSave;

    //移除角色相关权限时，需要跳过当前权限
    private String thisRole;
}

package jnpf.permission.model.authorize;

import jnpf.base.entity.*;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
public class AuthorizeParam {
    /**
     * 应用编码
     */
    private String appCode;
    /**
     * 可见系统列表
     */
    @Builder.Default
    List<SystemEntity> systemEntityList = new ArrayList<>();
    /**
     * 可见菜单列表
     */
    @Builder.Default
    List<ModuleEntity> menuEntityList = new ArrayList<>();
    /**
     * 可见按钮列表
     */
    @Builder.Default
    List<ModuleButtonEntity> buttonEntityList = new ArrayList<>();
    /**
     * 可见列表字段列表
     */
    @Builder.Default
    List<ModuleColumnEntity> columnEntityList = new ArrayList<>();
    /**
     * 可见数据权限列表
     */
    @Builder.Default
    List<ModuleDataAuthorizeSchemeEntity> resEntityList = new ArrayList<>();
    /**
     * 可见表单字段列表
     */
    @Builder.Default
    List<ModuleFormEntity> formEntityList = new ArrayList<>();
    /**
     * 权限id
     */
    String objectId;
    /**
     * 权限类型:position,organize,role
     */
    String objectType;
    /**
     * 分配的类型：module,button,column,resource,form
     */
    String itemType;
    /**
     * 选择中的菜单id
     */
    String moduleIds;
}

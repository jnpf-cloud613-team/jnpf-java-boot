package jnpf.permission.service;

import jnpf.base.ActionResult;
import jnpf.base.service.SuperService;
import jnpf.permission.entity.RoleRelationEntity;
import jnpf.permission.model.rolerelaiton.*;

import java.util.List;

/**
 * 角色关系
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/2/26 18:13:43
 */
public interface RoleRelationService extends SuperService<RoleRelationEntity> {

    /**
     * 根据对象类型和主键查询列表
     *
     * @return
     */
    List<RoleRelationEntity> getListPage(RoleListPage pagination);

    /**
     * 查询关联用户列表
     *
     * @return
     */
    List<RoleRelationUserVo> getUserPage(RoleRelationPage pagination);

    /**
     * 查询关联组织列表
     *
     * @return
     */
    List<RoleRelationOrgVo> getOrgPage(RoleRelationPage pagination);

    /**
     * 查询关联岗位列表
     *
     * @return
     */
    List<RoleRelationOrgVo> getPosPage(RoleRelationPage pagination);

    /**
     * 根据对象类型和主键查询列表
     *
     * @param objectId
     * @param objectType:posistion,organize
     * @return
     */
    List<RoleRelationEntity> getListByObjectId(String objectId, String objectType);

    /**
     * 根据对象类型和主键查询列表
     *
     * @param objectId
     * @param objectType
     * @return
     */
    List<RoleRelationEntity> getListByObjectId(List<String> objectId, String objectType);

    /**
     * 根据对象类型和角色主键查询列表
     *
     * @param roleId
     * @param objectType
     * @return
     */
    List<RoleRelationEntity> getListByRoleId(String roleId, String objectType);

    /**
     * 根据对象类型和角色主键查询列表
     *
     * @param roleId
     * @param objectType
     * @return
     */
    List<RoleRelationEntity> getListByRoleId(List<String> roleId, String objectType);

    /**
     * 角色添加岗位或组织
     *
     * @param form
     */
    ActionResult<Object> roleAddObjectIds(RoleRelationForm form);

    /**
     * 角色移除绑定数据
     *
     * @param ids
     */
    void delete(RoleRelationForm ids);

    /**
     * 组织或岗位添加角色
     *
     * @param form
     */
    void objectAddRoles(AddRolesForm form);

    /**
     * 组织或岗位移除角色
     *
     * @param form
     */
    void objectDeleteRoles(AddRolesForm form);
}

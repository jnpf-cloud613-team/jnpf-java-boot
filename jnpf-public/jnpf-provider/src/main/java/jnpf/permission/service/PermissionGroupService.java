package jnpf.permission.service;

import jnpf.base.service.SuperService;
import jnpf.permission.entity.PermissionGroupEntity;
import jnpf.permission.model.permissiongroup.PaginationPermissionGroup;

import java.util.List;

public interface PermissionGroupService extends SuperService<PermissionGroupEntity> {

    /**
     * 列表
     *
     * @param pagination
     * @return
     */
    List<PermissionGroupEntity> list(PaginationPermissionGroup pagination);

    /**
     * 列表
     *
     * @param filterEnabledMark
     * @param ids
     * @return
     */
    List<PermissionGroupEntity> list(boolean filterEnabledMark, List<String> ids);

    /**
     * 详情
     *
     * @param id
     * @return
     */
    PermissionGroupEntity info(String id);

    /**
     * 新建
     *
     * @param entity
     * @return
     */
    boolean create(PermissionGroupEntity entity);

    /**
     * 修改
     *
     * @param id     主键
     * @param entity 实体
     * @return
     */
    boolean update(String id, PermissionGroupEntity entity);

    /**
     * 删除
     *
     * @param entity 实体
     * @return
     */
    boolean delete(PermissionGroupEntity entity);

    /**
     * 验证名称是否重复
     *
     * @param id
     * @param entity
     */
    boolean isExistByFullName(String id, PermissionGroupEntity entity);

    /**
     * 验证编码是否重复
     *
     * @param id
     * @param enCode
     */
    boolean isExistByEnCode(String enCode, String id);

    /**
     * 通过ids获取权限组列表
     *
     * @param ids
     * @return
     */
    List<PermissionGroupEntity> list(List<String> ids);

    /**
     * 获取权限成员
     *
     * @param id 主键
     * @return
     */
    PermissionGroupEntity permissionMember(String id);

    /**
     * 通过对象id获取当前权限组
     *
     * @param objectId   对象主键
     * @param objectType 对象类型
     * @return
     */
    List<PermissionGroupEntity> getPermissionGroupByObjectId(String objectId, String objectType);

    /**
     * 权限集合授权
     *
     * @param id  权限集合id
     * @param ids 授权对象数组
     * @return List<String> 返回影响的用户列表
     */
    List<String> setAuthByIds(String id, List<String> ids);
}

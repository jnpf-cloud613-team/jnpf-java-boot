package jnpf.permission.service;

import jnpf.base.Pagination;
import jnpf.base.service.SuperService;
import jnpf.permission.entity.RoleEntity;
import jnpf.permission.model.position.PosConModel;
import jnpf.permission.model.role.RolePagination;

import java.util.List;
import java.util.Map;

/**
 * 系统角色
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface RoleService extends SuperService<RoleEntity> {
    /**
     * 列表
     *
     * @param page 条件
     */
    List<RoleEntity> getList(RolePagination page);

    /**
     * 验证名称
     *
     * @param fullName 名称
     * @param id       主键值
     */
    Boolean isExistByFullName(String fullName, String id, String type);

    /**
     * 验证编码
     *
     * @param enCode 名称
     * @param id     主键值
     */
    Boolean isExistByEnCode(String enCode, String id);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(RoleEntity entity);

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     */
    Boolean update(String id, RoleEntity entity);

    /**
     * 信息
     *
     * @param roleId 角色ID
     * @return 角色对象
     */
    RoleEntity getInfo(String roleId);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(RoleEntity entity);

    RoleEntity getByEnCode(String enCode);

    /**
     * 列表
     *
     * @param filterEnabledMark
     * @param type              用户、岗位、组织
     * @param isSystem          null-全部，1-系统，2-自定义
     * @return 角色对象集合
     */
    List<RoleEntity> getList(boolean filterEnabledMark, String type, Integer isSystem);

    /**
     * 根据id集合返回角色对象集合
     *
     * @param roleIds           角色ID集合
     * @param keyword
     * @param filterEnabledMark
     * @return 角色对象集合
     */
    List<RoleEntity> getListByIds(List<String> roleIds, String keyword, boolean filterEnabledMark);

    /**
     * 根据id查询角色列表
     *
     * @param idList
     * @return
     */
    List<RoleEntity> getListByIds(List<String> idList);

    /**
     * 根据id查询角色列表
     *
     * @param idList
     * @return
     */
    List<RoleEntity> getListByIds(Pagination pagination, List<String> idList);


    Map<String, Object> getRoleMap();

    /**
     * 角色编码/name.id
     *
     * @return
     */
    Map<String, Object> getRoleNameAndIdMap();

    Map<String, Object> getRoleNameAndIdMap(boolean enabledMark);

    /**
     * 获取角色实体
     *
     * @param fullName 角色名称
     * @return 角色对象
     */
    RoleEntity getInfoByFullName(String fullName);

    /**
     * 获取当前用户的默认组织下的所有角色集合
     *
     * @return 角色对象集合
     */
    List<RoleEntity> getCurRolesByOrgId();

    /**
     * 根据id集合
     *
     * @param idList            ID集合
     * @param filterEnabledMark
     */
    List<RoleEntity> getList(List<String> idList, Pagination pagination, boolean filterEnabledMark);

    /**
     * 统计用户角色的用户数
     *
     * @return
     */
    Map<String, Integer> roleUserCount();

    /**
     * 获取用户角色列表
     *
     * @param userId
     * @return
     */
    List<RoleEntity> getUserRoles(String userId);

    /**
     * 联动修改角色约束
     *
     * @param id
     * @param posConModel
     */
    void linkUpdate(String id, PosConModel posConModel);
}

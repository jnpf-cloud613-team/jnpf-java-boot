package jnpf.permission.service;

import jnpf.base.model.portalmanage.SavePortalAuthModel;
import jnpf.base.service.SuperService;
import jnpf.database.model.query.SuperJsonModel;
import jnpf.permission.entity.AuthorizeEntity;
import jnpf.permission.model.authorize.AuthorizeDataUpForm;
import jnpf.permission.model.authorize.AuthorizeVO;

import java.util.List;

/**
 * 操作权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface AuthorizeService extends SuperService<AuthorizeEntity> {

    /**
     * 获取当前系统权限列表
     *
     * @param singletonOrg      是否启用
     * @param currentSystemCode 当前系统编码
     * @param isBackend         是否后台（当前用户信息用）
     * @return
     */
    AuthorizeVO getAuthorize(boolean singletonOrg, String currentSystemCode, Integer isBackend);

    /**
     * 全部系统权限
     *
     * @param singletonOrg
     * @return
     */
    AuthorizeVO getAuthorizeByUser(boolean singletonOrg);

    /**
     * 获取全部系统的权限
     *
     * @param singletonOrg      是否启用
     * @param currentSystemCode 当前系统编码
     * @param isBackend         是否后台（当前用户信息用）
     * @return
     */
    AuthorizeVO getAuthorize(boolean singletonOrg, String currentSystemCode, Integer isBackend, boolean allSystem);

    /**
     * 创建
     *
     * @param authorizeList 实体对象
     */
    String save(AuthorizeDataUpForm authorizeList);

    /**
     * 根据用户id获取列表
     *
     * @param isAdmin        是否管理员
     * @param userId         用户主键
     * @param standingfilter 是否根据身份过滤（个人权限部分不需要过滤）
     * @return
     */
    List<AuthorizeEntity> getListByUserId(boolean isAdmin, String userId, boolean standingfilter);

    /**
     * 根据岗位或者角色获取全部权限
     *
     * @param objectId   岗位/角色id
     * @param objectType 类型:岗位-position/角色-role
     * @return
     */
    List<AuthorizeEntity> getListByPosOrRoleId(String objectId, String objectType);

    void saveItemAuth(SavePortalAuthModel portalAuthModel);

    List<SuperJsonModel> getConditionSql(String moduleId, String systemCode);

    void removeAuthByUserOrMenu(List<String> userIds, List<String> menuIds);

    void saveObjectAuth(SavePortalAuthModel portalAuthModel);

    /**
     * 通过Item获取权限列表
     *
     * @param itemType
     * @param itemId
     * @return
     */
    List<AuthorizeEntity> getAuthorizeByItem(String itemType, String itemId);

    List<AuthorizeEntity> getListByRoleIdsAndItemType(List<String> roleIds, String itemType);

    /**
     * 根据对象Id获取列表
     *
     * @param objectId 对象主键
     * @return
     */
    List<AuthorizeEntity> getListByObjectId(List<String> objectId);

    /**
     * 判断当前角色是否有权限
     *
     * @param roleId
     * @return
     */
    List<AuthorizeEntity> getListByRoleId(String roleId);

    /**
     * 根据对象Id获取列表
     *
     * @param objectId 对象主键
     * @param itemType 对象主键
     * @return
     */
    List<AuthorizeEntity> getListByObjectId(String objectId, String itemType);

    /**
     * 根据对象Id获取列表
     *
     * @param objectType 对象主键
     * @return
     */
    List<AuthorizeEntity> getListByObjectAndItem(String itemId, String objectType);

    /**
     * 根据对象Id获取列表
     *
     * @param itemId   对象主键
     * @param itemType 对象类型
     * @return
     */
    List<AuthorizeEntity> getListByObjectAndItemIdAndType(String itemId, String itemType);

    /**
     * 配置权限集合权限
     *
     * @param objectId
     * @param objectType
     */
    void setPermissionGroup(String objectId, String objectType);
}

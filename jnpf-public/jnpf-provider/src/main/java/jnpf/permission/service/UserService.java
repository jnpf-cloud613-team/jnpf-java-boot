package jnpf.permission.service;

import jnpf.base.Pagination;
import jnpf.base.service.SuperService;
import jnpf.model.SystemParamModel;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.rolerelaiton.RoleRelationPage;
import jnpf.permission.model.user.UserRelationIds;
import jnpf.permission.model.user.UserSystemCountModel;
import jnpf.permission.model.user.mod.UserConditionModel;
import jnpf.permission.model.user.page.PageUser;
import jnpf.permission.model.user.page.PaginationUser;
import jnpf.permission.model.user.page.UserPagination;
import jnpf.permission.model.user.vo.BaseInfoVo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public interface UserService extends SuperService<UserEntity> {

    List<UserEntity> getList(UserPagination pagination);

    /*======================get接口========================*/

    List<UserEntity> getAdminList();

    /**
     * 列表
     *
     * @param pagination  条件
     * @param enabledMark
     * @param gender
     * @return
     */
    List<UserEntity> getList(PaginationUser pagination, String organizeId, boolean flag, boolean filter, Integer enabledMark, String gender);

    /**
     * 列表
     *
     * @param pagination        条件
     * @param filterCurrentUser
     * @return
     */
    List<UserEntity> getList(PageUser pagination, Boolean filterCurrentUser);

    /**
     * 通过关键字查询
     *
     * @param pagination
     * @return
     */
    List<UserEntity> getUserPage(Pagination pagination);

    /**
     * 通过组织id获取用户列表
     *
     * @param organizeId 组织id
     * @param keyword    关键字
     * @return
     */
    List<UserEntity> getListByOrganizeId(String organizeId, String keyword);

    /**
     * 列表
     *
     * @param enabledMark
     * @return
     */
    List<UserEntity> getList(boolean enabledMark);

    /**
     * 用户名列表（在线开发）
     *
     * @param idList
     * @return
     */
    List<UserEntity> getUserNameList(List<String> idList);

    /**
     * 用户名列表（在线开发）
     *
     * @param idList
     * @return
     */
    List<UserEntity> getUserNameList(Set<String> idList);


    /**
     * （id : name/account）
     *
     * @return
     */
    Map<String, Object> getUserMap();


    /**
     * （ name/account: id）
     *
     * @return
     */
    Map<String, Object> getUserNameAndIdMap();

    Map<String, Object> getUserNameAndIdMap(boolean enabledMark);

    /**
     * 通过名称查询id
     *
     * @return
     */
    UserEntity getByRealName(String realName);


    /**
     * 通过名称查询id
     *
     * @return
     */
    UserEntity getByRealName(String realName, String account);

    /**
     * 列表
     *
     * @param managerId 主管Id
     * @param keyword   关键字
     * @return
     */
    List<UserEntity> getListByManagerId(String managerId, String keyword);

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    UserEntity getInfo(String id);

    /**
     * 信息
     *
     * @param account 账户
     * @return
     */
    UserEntity getUserByAccount(String account);

    /**
     * 信息
     *
     * @param mobile 手机号码
     * @return
     */
    UserEntity getUserByMobile(String mobile);

    /*==============================================*/

    Boolean setAdminListByIds(List<String> adminIds);

    /**
     * 验证账户
     *
     * @param account 账户
     * @return
     */
    boolean isExistByAccount(String account);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    Boolean create(UserEntity entity);

    /**
     * 判断用户额度
     */
    void beforeCheck();

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     */
    Boolean update(String id, UserEntity entity);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(UserEntity entity);

    /**
     * 批量删除用户信息
     *
     * @param userIdList 用户id列表
     */
    void batchDelete(List<String> userIdList);

    /**
     * 修改密码
     *
     * @param entity 实体对象
     */
    void updatePassword(UserEntity entity);

    /**
     * 查询用户名称
     *
     * @param id 主键值
     * @return
     */
    List<UserEntity> getUserName(List<String> id);

    /**
     * 查询用户名称
     *
     * @param id 主键值
     * @return
     */
    List<UserEntity> getUserName(List<String> id, boolean filterEnabledMark);

    /**
     * 查询用户名称
     *
     * @param id 主键值
     * @return
     */
    List<UserEntity> getListByUserIds(List<String> id);

    /**
     * 查询出分页被禁用的账号
     *
     * @param id 主键值
     * @return
     */
    List<UserEntity> getUserList(List<String> id);

    /**
     * 通过account返回user实体
     *
     * @param account 账户
     * @return
     */
    UserEntity getUserEntity(String account);

    /**
     * 获取用户id
     *
     * @return
     */
    List<String> getListId();

    /**
     * 添加岗位或角色成员
     *
     * @param entity
     */
    void update(UserEntity entity, String type);

    /**
     * 通过组织id获取上级id集合
     *
     * @param organizeId
     * @param organizeParentIdList
     */
    void getOrganizeIdTree(String organizeId, StringBuilder organizeParentIdList);

    /**
     * 候选人分页查询
     *
     * @param id
     * @param pagination
     * @return
     */
    List<UserEntity> getUserName(List<String> id, Pagination pagination);

    /**
     * 候选人分页查询
     *
     * @param id
     * @param pagination
     * @param flag       是否过滤自己
     * @return
     */
    List<UserEntity> getUserNames(List<String> id, PaginationUser pagination, Boolean flag, Boolean enabledMark);


    /**
     * 删除在线的角色用户
     */
    Boolean delCurRoleUser(String message, List<String> objectIdAll);


    /**
     * 获取用户信息
     *
     * @param orgIdList
     * @param keyword
     * @return
     */
    List<UserEntity> getList(List<String> orgIdList, String keyword);

    public List<UserEntity> getListBySyn(List<String> orgIdList, String keyword);

    /**
     * 得到用户关系
     *
     * @param userIds
     * @return
     */
    List<String> getUserIdList(List<String> userIds);

    /**
     * 根据用户关系获取所有用户数据
     *
     * @param userIds
     * @return
     */
    List<String> getRelUserEnable(List<String> userIds);

    /**
     * 得到用户关系
     *
     * @param userIds
     * @return
     */
    List<BaseInfoVo> getObjList(List<String> userIds, PaginationUser pagination);

    /**
     * 查询给定的条件是否有默认当前登录者的默认用户值
     *
     * @param userConditionModel
     * @return
     */
    String getDefaultCurrentValueUserId(UserConditionModel userConditionModel);

    /**
     * 通过ids转换数据
     *
     * @param ids
     * @return
     */
    List<String> getFullNameByIds(List<String> ids);

    /**
     * 通过ids返回相应的数据
     *
     * @param ids
     * @return
     */
    List<BaseInfoVo> selectedByIds(List<String> ids);


    List<UserEntity> getUserAccount(List<String> ids);

    void updateStand(List<String> ids, int standing);

    /**
     * 删除在线用户（仅提示刷新）
     *
     * @param message
     * @param userIds 用户IDs
     * @return 执行结果
     */
    void delCurUser(String message, List<String> userIds);

    /**
     * 切换身份刷新其他在线用户（仅提示刷新）
     *
     * @return 执行结果
     */
    void majorStandFreshUser();

    /**
     * 删除在线用户(强制下线)
     * 密码修改时用
     *
     * @param message
     * @param userIds 用户IDs
     * @return 执行结果
     */
    Boolean logoutUser(String message, List<String> userIds);

    /**
     * 根据用户id列表查询用户分页
     *
     * @return 执行结果
     */
    List<UserEntity> getPageByIds(RoleRelationPage pagination);

    /**
     * 获取用户组织角色等数据列表
     *
     * @param userId
     * @return
     */
    UserRelationIds getUserObjectIdList(String userId);

    Map<String, String> getSystemFieldValue(SystemParamModel model);

    /**
     * 关键词及ids范围查询用户信息
     *
     * @param model
     * @return
     */
    List<UserEntity> pageUser(UserSystemCountModel model);
}

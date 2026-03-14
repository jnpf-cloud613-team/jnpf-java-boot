package jnpf.base.service;

import jnpf.base.entity.SystemEntity;
import jnpf.base.model.AppAuthorizationModel;
import jnpf.base.model.base.SystemPageVO;
import jnpf.permission.model.user.WorkHandoverModel;

import java.util.List;

/**
 * 系统
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface SystemService extends SuperService<SystemEntity> {

    /**
     * 获取列表
     *
     * @return
     */
    List<SystemEntity> getList();

    /**
     * 获取系统列表
     *
     * @param keyword
     * @param filterMain
     * @return
     */
    List<SystemEntity> getList(String keyword, boolean verifyAuth, boolean filterMain);

    /**
     * 获取系统列表
     *
     * @param pageVO
     * @return
     */
    List<SystemEntity> getList(SystemPageVO pageVO);

    /**
     * 获取列表
     *
     * @return
     */
    List<SystemEntity> getListByIdsKey(List<String> ids, String keyword);

    /**
     * 获取详情
     *
     * @param id
     * @return
     */
    SystemEntity getInfo(String id);

    /**
     * 判断系统名称是否重复
     *
     * @param id
     * @param fullName
     * @return
     */
    Boolean isExistFullName(String id, String fullName);

    /**
     * 判断系统编码是否重复
     *
     * @param id
     * @param enCode
     * @return
     */
    Boolean isExistEnCode(String id, String enCode);

    /**
     * 新建
     *
     * @param entity
     * @return
     */
    Boolean create(SystemEntity entity);

    /**
     * 新建
     *
     * @param entity
     * @return
     */
    Boolean update(String id, SystemEntity entity);

    /**
     * 删除
     *
     * @param id
     * @return
     */
    Boolean delete(String id);

    /**
     * 通过id获取系统列表
     *
     * @param list
     * @param moduleAuthorize
     * @return
     */
    List<SystemEntity> getListByIds(List<String> list, List<String> moduleAuthorize);

    /**
     * 通过编码获取系统信息
     *
     * @param enCode
     * @return
     */
    SystemEntity getInfoByEnCode(String enCode);

    /**
     * 获取
     *
     * @param moduleAuthorize
     * @return
     */
    List<SystemEntity> findSystemAdmin(List<String> moduleAuthorize);

    boolean saveSystemAuthorizion(AppAuthorizationModel model);

    /**
     * 获取当前用户有编辑权限的应用
     *
     * @param userId
     * @param isStand 是否判断当前身份
     * @return
     */
    List<SystemEntity> getAuthListByUser(String userId, Boolean isStand);

    /**
     * 工作交接-交接应用
     *
     * @param workHandoverModel
     */
    void workHandover(WorkHandoverModel workHandoverModel);

    void changeSystemAuthorizion(AppAuthorizationModel model);

    List<SystemEntity> getListByIds(List<String> systemId, List<String> moduleAuthorize, int type);

    /**
     * 获取当前用户创建应用所有菜单id
     *
     * @param userId
     * @return
     */
    List<SystemEntity> getListByCreUser(String userId);

    void setAutoEnCode(SystemEntity entity);

    void importCopy(SystemEntity entity, boolean isImport);

    List<SystemEntity> getMainList();
}

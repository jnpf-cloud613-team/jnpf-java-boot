package jnpf.base.service;

import jnpf.base.Page;
import jnpf.base.Pagination;
import jnpf.base.entity.ModuleDataEntity;
import jnpf.base.model.module.ModuleModel;
import jnpf.model.login.AllMenuSelectVO;

import java.util.List;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/5/6 上午10:48
 */
public interface ModuleDataService extends SuperService<ModuleDataEntity> {

    /**
     * 列表
     *
     * @return
     */
    List<ModuleDataEntity> getList(String category, Page page);

    /**
     * 创建
     */
    void create(String moduleId);

    /**
     * 信息
     *
     * @param objectId 对象主键
     * @return
     */
    ModuleDataEntity getInfo(String objectId);

    /**
     * 验证名称
     *
     * @param objectId 对象主键
     * @return
     */
    boolean isExistByObjectId(String objectId);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(ModuleDataEntity entity);

    /**
     * 删除
     */
    void delete(String objectId);

    /**
     * app菜单
     *
     * @return
     */
    List<AllMenuSelectVO> getDataList(Page page);

    /**
     * app常用菜单详情
     *
     * @return
     */
    List<AllMenuSelectVO> getAppDataList(Pagination pagination);

    /**
     * 获取全部收藏菜单
     *
     * @return
     */
    List<ModuleModel> getFavoritesList(List<ModuleModel> moduleList );
}

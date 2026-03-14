package jnpf.base.service;

import jnpf.base.Pagination;
import jnpf.base.entity.ModuleColumnEntity;

import java.util.List;

/**
 * 列表权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */

public interface ModuleColumnService extends SuperService<ModuleColumnEntity> {

    /**
     * 列表
     *
     * @return ignore
     */
    List<ModuleColumnEntity> getList();

    /**
     * 列表
     *
     * @return ignore
     */
    List<ModuleColumnEntity> getEnabledMarkList(String enabledMark);

    /**
     * 列表
     *
     * @param moduleId   功能主键
     * @param pagination 分页参数
     * @return ignore
     */
    List<ModuleColumnEntity> getList(String moduleId, Pagination pagination);

    /**
     * 列表
     *
     * @param moduleId 功能主键
     * @return ignore
     */
    List<ModuleColumnEntity> getList(String moduleId);


    /**
     * 信息
     *
     * @param id 主键值
     * @return ignore
     */
    ModuleColumnEntity getInfo(String id);

    /**
     * 通过id和菜单id获取详情
     *
     * @param id       主键值
     * @param moduleId
     * @return
     */
    ModuleColumnEntity getInfo(String id, String moduleId);

    /**
     * 验证名称
     *
     * @param moduleId 功能主键
     * @param fullName 名称
     * @param id       主键值
     * @return ignore
     */
    boolean isExistByFullName(String moduleId, String fullName, String id);

    /**
     * 验证编码
     *
     * @param moduleId 功能主键
     * @param enCode   编码
     * @param id       主键值
     * @return ignore
     */
    boolean isExistByEnCode(String moduleId, String enCode, String id);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(ModuleColumnEntity entity);

    /**
     * 创建
     *
     * @param entitys 实体对象
     */
    void create(List<ModuleColumnEntity> entitys);

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     */
    boolean update(String id, ModuleColumnEntity entity);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(ModuleColumnEntity entity);

    /**
     * 通过moduleIds获取权限
     *
     * @param ids
     * @return
     */
    List<ModuleColumnEntity> getListByModuleId(List<String> ids,Integer typr);

    /**
     * 通过moduleIds获取权限
     *
     * @param ids
     * @return
     */
    List<ModuleColumnEntity> getListByIds(List<String> ids);
}

package jnpf.base.service;

import jnpf.base.entity.VisualAliasEntity;
import jnpf.base.model.VisualAliasForm;
import jnpf.base.util.common.AliasModel;
import jnpf.model.visualjson.TableModel;

import java.util.List;
import java.util.Map;

/**
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/4/13 14:05:19
 */
public interface VisualAliasService extends SuperService<VisualAliasEntity> {

    List<VisualAliasEntity> getList(String visualId);

    /**
     * 获取表 别名列表
     *
     * @param id
     * @return
     */
    List<TableModel> getAliasInfo(String id);

    /**
     * 保存或者修改表别名列表
     *
     * @param id
     * @param form
     */
    void aliasSave(String id, VisualAliasForm form);

    /**
     * 获取全字段别名，系统字段自动驼峰
     *
     * @param id
     * @return
     */
    Map<String, AliasModel> getAllFiledsAlias(String id);

    /**
     * 复制命名规范
     * @param visualId
     * @param uuid
     * @return
     */
    void copy(String visualId,String uuid);

    /**
     * 复制单个对象
     * @param visualId
     * @param copy
     * @return
     */
    void copyEntity(VisualAliasEntity copy,String visualId);

    /**
     *
     * @param visualId
     */
    void removeByVisualId(String visualId);
}

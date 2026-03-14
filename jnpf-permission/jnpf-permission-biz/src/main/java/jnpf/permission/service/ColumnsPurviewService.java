package jnpf.permission.service;

import jnpf.base.service.SuperService;
import jnpf.permission.entity.ColumnsPurviewEntity;

/**
 * 模块列表权限业务类
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/15 9:39
 */
public interface ColumnsPurviewService extends SuperService<ColumnsPurviewEntity> {

    /**
     * 通过moduleId获取列表权限
     *
     * @param moduleId
     * @return
     */
    ColumnsPurviewEntity getInfo(String moduleId);

    /**
     * 判断是保存还是编辑
     *
     * @param moduleId
     * @param entity
     * @return
     */
    boolean update(String moduleId, ColumnsPurviewEntity entity);
}

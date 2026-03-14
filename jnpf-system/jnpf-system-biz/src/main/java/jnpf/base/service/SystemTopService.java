package jnpf.base.service;

import jnpf.base.entity.SystemTopEntity;

import java.util.List;

/**
 * 应用置顶Service
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025-09-05
 */
public interface SystemTopService extends SuperService<SystemTopEntity> {
    /**
     * 保存置顶
     *
     * @param entity
     * @param hasSysIds 列表里有的系统id
     * @return
     */
    void saveTop(SystemTopEntity entity, List<String> hasSysIds);

    /**
     * 取消置顶
     *
     * @param entity
     * @return
     */
    void canleTop(SystemTopEntity entity);

    List<String> getObjectIdList(String type,String standId);
}

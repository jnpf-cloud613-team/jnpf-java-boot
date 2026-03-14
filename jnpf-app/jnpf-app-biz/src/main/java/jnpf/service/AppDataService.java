package jnpf.service;

import jnpf.base.service.SuperService;
import jnpf.entity.AppDataEntity;

import java.util.List;

/**
 * app常用数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021-08-08
 */
public interface AppDataService extends SuperService<AppDataEntity> {


    /**
     * 列表
     *
     * @return
     */
    List<AppDataEntity> getList();

    /**
     * 信息
     *
     * @param objectId 对象主键
     * @return
     */
    AppDataEntity getInfo(String objectId);

    /**
     * 验证名称
     *
     * @param objectId 对象主键
     * @return
     */
    boolean isExistByObjectId(String objectId, String systemId);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(AppDataEntity entity);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(AppDataEntity entity);

}

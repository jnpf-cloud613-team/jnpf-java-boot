package jnpf.base.service;

import jnpf.base.model.ai.AiPagination;
import jnpf.base.entity.AiEntity;

import java.util.List;

/**
 * 个人签名
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022年9月2日 上午9:18
 */
public interface AiService extends SuperService<AiEntity> {


    /**
     * 列表
     *
     * @param pagination 条件
     */
    List<AiEntity> getList(AiPagination pagination);

    /**
     * 验证名称
     *
     * @param fullName 名称
     * @param id       主键值
     * @return ignore
     */
    boolean isExistByFullName(String fullName, String id);

    /**
     * 信息
     *
     * @param id 主键值
     */
    AiEntity getInfo(String id);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    boolean create(AiEntity entity);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    boolean update(String id, AiEntity entity);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(AiEntity entity);

    /**
     * 获取默认
     */
    AiEntity getDefault();
}

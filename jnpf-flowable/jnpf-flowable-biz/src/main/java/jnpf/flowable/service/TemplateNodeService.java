package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TemplateNodeEntity;

import java.util.List;

public interface TemplateNodeService extends SuperService<TemplateNodeEntity> {

    /**
     * 列表
     *
     * @return
     */
    List<TemplateNodeEntity> getList(String flowId);

    /**
     * 获取节点
     *
     * @param flowIds 版本主键集合
     */
    List<TemplateNodeEntity> getList(List<String> flowIds, String nodeType);

    /**
     * 根据用户主键 获取节点
     *
     * @param userId 用户主键
     */
    List<TemplateNodeEntity> getListLikeUserId(String userId);

    /**
     * 信息
     *
     * @param id 主键值
     * @return ignore
     */
    TemplateNodeEntity getInfo(String id) throws WorkFlowException;

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(TemplateNodeEntity entity);

    /**
     * 更新
     *
     * @param id     主键值
     * @param entity 实体对象
     * @return ignore
     */
    boolean update(String id, TemplateNodeEntity entity);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(TemplateNodeEntity entity);

    /**
     * 删除
     *
     * @param idList
     */
    void deleteList(List<String> idList);

    /**
     * 删除
     *
     * @param idList
     */
    void delete(List<String> idList);

    /**
     * 获取开始节点表单列表
     */
    List<TemplateNodeEntity> getListStart();
}

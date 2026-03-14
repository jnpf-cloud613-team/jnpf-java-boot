package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TriggerTaskEntity;
import jnpf.flowable.model.trigger.TriggerInfoListModel;
import jnpf.flowable.model.trigger.TriggerInfoModel;
import jnpf.flowable.model.trigger.TriggerPagination;
import jnpf.flowable.model.trigger.TriggerTaskModel;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/10 17:12
 */
public interface TriggerTaskService extends SuperService<TriggerTaskEntity> {
    /**
     * 获取任务下的触发记录
     *
     * @param taskId   任务主键
     * @param nodeCode 节点编码
     */
    List<TriggerInfoListModel> getListByTaskId(String taskId, String nodeCode);

    /**
     * 判断是否有任务流程
     *
     * @param taskId   任务主键
     * @param nodeCode 节点编码
     */
    boolean existTriggerTask(String taskId, String nodeCode);

    /**
     * 列表
     *
     * @param pagination 分页参数
     */
    List<TriggerTaskModel> getList(TriggerPagination pagination);

    /**
     * 详情
     *
     * @param id 主键
     */
    TriggerInfoModel getInfo(String id) throws WorkFlowException;

    /**
     * 重试
     *
     * @param id 主键
     */
    void retry(String id) throws WorkFlowException;

    /**
     * 保存
     *
     * @param entity 实体
     */
    void saveTriggerTask(TriggerTaskEntity entity);

    /**
     * 更新
     *
     * @param entity 实体
     */
    void updateTriggerTask(TriggerTaskEntity entity);

    /**
     * 批量删除
     *
     * @param ids 任务流程实例主键
     */
    void batchDelete(List<String> ids);

    /**
     * 判断流程版本下是否存在任务流程
     *
     * @param flowIds 流程版本主键集合
     */
    boolean checkByFlowIds(List<String> flowIds);
}

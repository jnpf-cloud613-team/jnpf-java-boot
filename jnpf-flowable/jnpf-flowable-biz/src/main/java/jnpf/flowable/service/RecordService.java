package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.RecordEntity;
import jnpf.flowable.model.operator.OperatorVo;
import jnpf.flowable.model.record.RecordVo;
import jnpf.flowable.model.task.TaskPagination;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/23 9:21
 */
public interface RecordService extends SuperService<RecordEntity> {

    /**
     * 列表
     *
     * @param taskId 任务id
     */
    List<RecordEntity> getList(String taskId);

    /**
     * 分页列表
     *
     * @param pagination 分页
     */
    List<OperatorVo> getList(TaskPagination pagination);

    /**
     * 消息汇总列表
     *
     * @param taskId     任务id
     * @param statusList 状态
     */
    List<RecordEntity> getRecordList(String taskId, List<Integer> statusList);

    /**
     * 详情
     *
     * @param id 记录主键
     */
    RecordEntity getInfo(String id);

    /**
     * 保存
     *
     * @param entity 记录实体
     */
    void create(RecordEntity entity);

    /**
     * 修改
     *
     * @param id     主键
     * @param entity 实体
     */
    void update(String id, RecordEntity entity);

    /**
     * 变更状态为作废
     *
     * @param taskId       任务主键
     * @param nodeCodeList 节点编码
     */
    void updateStatusToInvalid(String taskId, List<String> nodeCodeList);

    /**
     * 记录列表
     *
     * @param taskId 任务主键
     * @param nodeId 节点id
     */
    List<RecordVo> getList(String taskId, String nodeId);
}

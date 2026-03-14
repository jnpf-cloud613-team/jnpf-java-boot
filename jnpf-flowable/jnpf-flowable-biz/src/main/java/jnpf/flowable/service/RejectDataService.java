package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.EventLogEntity;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.RejectDataEntity;
import jnpf.flowable.entity.TaskEntity;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/8 18:07
 */
public interface RejectDataService extends SuperService<RejectDataEntity> {
    /**
     * 详情
     *
     * @param id 主键
     */
    RejectDataEntity getInfo(String id) throws WorkFlowException;

    /**
     * 新增
     *
     * @param taskEntity         任务
     * @param operatorEntityList 经办集合
     * @param nodeCode           节点编码
     */
    RejectDataEntity create(TaskEntity taskEntity, List<OperatorEntity> operatorEntityList, List<EventLogEntity> eventLogList, String nodeCode);
}

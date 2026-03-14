package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.TriggerRecordEntity;
import jnpf.flowable.entity.TriggerTaskEntity;
import jnpf.flowable.model.flowable.FlowableTaskModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/10 17:15
 */
public interface TriggerRecordService extends SuperService<TriggerRecordEntity> {

    List<TriggerRecordEntity> getList(String triggerTaskId);

    List<TriggerRecordEntity> getListByTaskId(String taskId);

    void create(TriggerRecordEntity entity);

    void create(TriggerTaskEntity triggerTask, NodeModel nodeModel, FlowableTaskModel taskModel);

    void createStart(String triggerId);

    void createEnd(String triggerId);

    void delete(List<String> triggerTaskIds);
}

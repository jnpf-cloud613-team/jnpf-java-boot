package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.TriggerRecordEntity;
import jnpf.flowable.entity.TriggerTaskEntity;
import jnpf.flowable.mapper.TriggerRecordMapper;
import jnpf.flowable.model.flowable.FlowableTaskModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.service.TriggerRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/10 17:18
 */
@Service
public class TriggerRecordServiceImpl extends SuperServiceImpl<TriggerRecordMapper, TriggerRecordEntity> implements TriggerRecordService {

    @Override
    public List<TriggerRecordEntity> getList(String triggerTaskId) {
        return this.baseMapper.getList(triggerTaskId);
    }

    @Override
    public List<TriggerRecordEntity> getListByTaskId(String taskId) {
        return this.baseMapper.getListByTaskId(taskId);
    }

    @Override
    public void create(TriggerRecordEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public void create(TriggerTaskEntity triggerTask, NodeModel nodeModel, FlowableTaskModel taskModel) {
        this.baseMapper.create(triggerTask, nodeModel, taskModel);
    }

    @Override
    public void createStart(String triggerId) {
        this.baseMapper.createStart(triggerId);
    }

    @Override
    public void createEnd(String triggerId) {
        this.baseMapper.createEnd(triggerId);
    }

    @Override
    public void delete(List<String> triggerTaskIds) {
        this.baseMapper.delete(triggerTaskIds);
    }
}

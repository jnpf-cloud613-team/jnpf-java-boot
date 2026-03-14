package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.TriggerLaunchflowEntity;
import jnpf.flowable.mapper.TriggerLaunchflowMapper;
import jnpf.flowable.service.TriggerLaunchflowService;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/4/29 下午5:21
 */
@Service
public class TriggerLaunchflowServiceImpl extends SuperServiceImpl<TriggerLaunchflowMapper, TriggerLaunchflowEntity> implements TriggerLaunchflowService {


    @Override
    public void create(TriggerLaunchflowEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public List<TriggerLaunchflowEntity> getTaskList(List<String> taskId) {
        return this.baseMapper.getTaskList(taskId, null);
    }

    @Override
    public List<TriggerLaunchflowEntity> getTaskIds(String taskId) {
        return this.baseMapper.getTaskIds(taskId);
    }

    @Override
    public void delete(TriggerLaunchflowEntity entity) {
        this.baseMapper.delete(entity);
    }
}

package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.EventLogEntity;
import jnpf.flowable.mapper.EventLogMapper;
import jnpf.flowable.service.EventLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventLogServiceImpl extends SuperServiceImpl<EventLogMapper, EventLogEntity> implements EventLogService {

    @Override
    public List<EventLogEntity> getList(String taskId) {
        return this.baseMapper.getList(taskId);
    }

    @Override
    public List<EventLogEntity> getList(String taskId, List<String> nodeCode) {
        return this.baseMapper.getList(taskId, nodeCode);
    }

    @Override
    public void delete(String taskId, List<String> nodeCode) {
        this.baseMapper.delete(taskId, nodeCode);
    }

    @Override
    public void create(EventLogEntity entity) {
        this.baseMapper.create(entity);
    }

}

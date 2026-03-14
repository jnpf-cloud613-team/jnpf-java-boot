package jnpf.base.service.impl;

import jnpf.base.entity.ScheduleLogEntity;
import jnpf.base.mapper.ScheduleLogMapper;
import jnpf.base.service.ScheduleLogService;
import jnpf.base.service.SuperServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 日程
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 */
@Service
public class ScheduleLogServiceImpl extends SuperServiceImpl<ScheduleLogMapper, ScheduleLogEntity> implements ScheduleLogService {

    @Override
    public List<ScheduleLogEntity> getListAll(List<String> scheduleIdList) {
        return this.baseMapper.getListAll(scheduleIdList);
    }

    @Override
    public ScheduleLogEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public void create(ScheduleLogEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public void delete(List<String> scheduleIdList, String operationType) {
        this.baseMapper.delete(scheduleIdList, operationType);
    }

    @Override
    public boolean update(String id, ScheduleLogEntity entity) {
        return this.baseMapper.update(id, entity);
    }
}

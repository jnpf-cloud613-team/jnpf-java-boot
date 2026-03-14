package jnpf.base.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jnpf.base.entity.ScheduleNewUserEntity;
import jnpf.base.mapper.ScheduleNewUserMapper;
import jnpf.base.service.ScheduleNewUserService;
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
public class ScheduleNewUserServiceImpl extends SuperServiceImpl<ScheduleNewUserMapper, ScheduleNewUserEntity> implements ScheduleNewUserService {

    @Override
    public List<ScheduleNewUserEntity> getList(String scheduleId, Integer type) {
        return this.baseMapper.getList(scheduleId, type);
    }

    @Override
    public List<ScheduleNewUserEntity> getList() {
        return this.baseMapper.getList();
    }

    @Override
    public void create(ScheduleNewUserEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    @DSTransactional
    public void deleteByScheduleId(List<String> scheduleIdList) {
        this.baseMapper.deleteByScheduleId(scheduleIdList);
    }

    @Override
    public void deleteByUserId(List<String> scheduleIdList) {
        this.baseMapper.deleteByUserId(scheduleIdList);
    }
}

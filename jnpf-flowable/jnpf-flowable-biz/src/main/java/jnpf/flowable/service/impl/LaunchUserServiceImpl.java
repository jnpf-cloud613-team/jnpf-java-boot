package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.LaunchUserEntity;
import jnpf.flowable.mapper.LaunchUserMapper;
import jnpf.flowable.service.LaunchUserService;
import jnpf.flowable.util.FlowUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 9:46
 */
@Service
@RequiredArgsConstructor
public class LaunchUserServiceImpl extends SuperServiceImpl<LaunchUserMapper, LaunchUserEntity> implements LaunchUserService {


    private final FlowUtil flowUtil;

    @Override
    public LaunchUserEntity getInfoByTask(String taskId) {
        return this.baseMapper.getInfoByTask(taskId);
    }

    @Override
    public List<LaunchUserEntity> getTaskList(String taskId) {
        return this.baseMapper.getTaskList(taskId);
    }

    @Override
    public void createLaunchUser(String taskId, String userId) {
        flowUtil.createLaunchUser(taskId, userId);
    }

    @Override
    public void delete(String taskId) {
        this.baseMapper.delete(taskId);
    }

    @Override
    public void delete(String taskId, List<String> nodeCode) {
        this.baseMapper.delete(taskId, nodeCode);
    }

    @Override
    public void deleteStepUser(String taskId) {
        this.baseMapper.deleteStepUser(taskId);
    }
}

package jnpf.flowable.service.impl;

import jnpf.base.service.SuperServiceImpl;
import jnpf.flowable.entity.RevokeEntity;
import jnpf.flowable.entity.TaskLineEntity;
import jnpf.flowable.mapper.RevokeMapper;
import jnpf.flowable.mapper.TaskLineMapper;
import jnpf.flowable.service.TaskLineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/23 17:37
 */
@Service
@RequiredArgsConstructor
public class TaskLineServiceImpl extends SuperServiceImpl<TaskLineMapper, TaskLineEntity> implements TaskLineService {

    private final RevokeMapper revokeMapper;

    @Override
    public List<TaskLineEntity> getList(String taskId) {
        return this.getList(taskId);
    }

    @Override
    public void create(String taskId, Map<String, Boolean> conditionResMap) {
        this.baseMapper.create(taskId, conditionResMap);
    }

    @Override
    public List<String> getLineKeyList(String taskId) {
        RevokeEntity revokeEntity = revokeMapper.getRevokeTask(taskId);
        if (null != revokeEntity) {
            taskId = revokeEntity.getTaskId();
        }
        return this.baseMapper.getLineKeyList(taskId);
    }
}

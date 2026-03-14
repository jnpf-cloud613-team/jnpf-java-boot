package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.TriggerLaunchflowEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 16:00
 */
public interface TriggerLaunchflowMapper extends SuperMapper<TriggerLaunchflowEntity> {

    default List<TriggerLaunchflowEntity> getTriggerList(List<String> triggerId) {
        if (CollUtil.isEmpty(triggerId)) {
            return new ArrayList<>();
        }
        QueryWrapper<TriggerLaunchflowEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(TriggerLaunchflowEntity::getTriggerId, triggerId);
        queryWrapper.lambda().select(TriggerLaunchflowEntity::getId, TriggerLaunchflowEntity::getTriggerId, TriggerLaunchflowEntity::getTaskId, TriggerLaunchflowEntity::getNodeCode);
        return this.selectList(queryWrapper);
    }

    default void create(TriggerLaunchflowEntity entity) {
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(RandomUtil.uuId());
        }
        this.insert(entity);
    }

    default List<TriggerLaunchflowEntity> getTaskList(List<String> taskId, List<String> nodeCode) {
        if (CollUtil.isEmpty(taskId)) {
            return new ArrayList<>();
        }
        QueryWrapper<TriggerLaunchflowEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(TriggerLaunchflowEntity::getTaskId, taskId);
        if (CollUtil.isNotEmpty(nodeCode)) {
            queryWrapper.lambda().in(TriggerLaunchflowEntity::getNodeCode, nodeCode);
        }
        queryWrapper.lambda().select(TriggerLaunchflowEntity::getId, TriggerLaunchflowEntity::getTriggerId, TriggerLaunchflowEntity::getTaskId, TriggerLaunchflowEntity::getNodeCode);
        return this.selectList(queryWrapper);
    }

    default List<TriggerLaunchflowEntity> getTaskIds(String taskId) {
        if (StringUtil.isEmpty(taskId)) {
            return new ArrayList<>();
        }
        QueryWrapper<TriggerLaunchflowEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().like(TriggerLaunchflowEntity::getTaskIds, taskId);
        return this.selectList(queryWrapper);
    }

    default void delete(TriggerLaunchflowEntity entity) {
        if (entity != null) {
            this.deleteById(entity.getId());
        }
    }

}

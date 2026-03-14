package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.LaunchUserEntity;
import jnpf.flowable.model.util.FlowNature;

import java.util.ArrayList;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 9:44
 */
public interface LaunchUserMapper extends SuperMapper<LaunchUserEntity> {

    default LaunchUserEntity getInfoByTask(String taskId) {
        QueryWrapper<LaunchUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(LaunchUserEntity::getTaskId, taskId);
        queryWrapper.lambda().eq(LaunchUserEntity::getType, FlowNature.TASK_INITIATION);
        return this.selectOne(queryWrapper);
    }

    
    default List<LaunchUserEntity> getTaskList(String taskId) {
        QueryWrapper<LaunchUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(LaunchUserEntity::getTaskId, taskId);
        queryWrapper.lambda().eq(LaunchUserEntity::getType, FlowNature.STEP_INITIATION);
        return this.selectList(queryWrapper);
    }

    default void delete(String taskId) {
        delete(taskId, new ArrayList<>());
    }

    default void delete(String taskId, List<String> nodeCode) {
        QueryWrapper<LaunchUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(LaunchUserEntity::getTaskId, taskId);
        if (ObjectUtil.isNotEmpty(nodeCode)) {
            queryWrapper.lambda().in(LaunchUserEntity::getNodeCode, nodeCode);
        }
        List<LaunchUserEntity> list = this.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            this.deleteByIds(list);
        }
    }

    default void deleteStepUser(String taskId) {
        List<LaunchUserEntity> list = getTaskList(taskId);
        if (CollUtil.isNotEmpty(list)) {
            this.deleteByIds(list);
        }
    }
}

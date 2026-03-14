package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.ImmutableList;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.TriggerRecordEntity;
import jnpf.flowable.entity.TriggerTaskEntity;
import jnpf.flowable.enums.TriggerRecordEnum;
import jnpf.flowable.model.flowable.FlowableTaskModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.util.FlowNature;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/10 17:14
 */
public interface TriggerRecordMapper extends SuperMapper<TriggerRecordEntity> {

    default List<TriggerRecordEntity> getList(String triggerTaskId) {
        if (StringUtils.isBlank(triggerTaskId)) {
            return new ArrayList<>();
        }
        return getList(ImmutableList.of(triggerTaskId));
    }

    default List<TriggerRecordEntity> getList(List<String> triggerTaskIdList) {
        if (CollUtil.isEmpty(triggerTaskIdList)) {
            return new ArrayList<>();
        }
        QueryWrapper<TriggerRecordEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(TriggerRecordEntity::getTriggerId, triggerTaskIdList).orderByAsc(TriggerRecordEntity::getStartTime);
        return this.selectList(queryWrapper);
    }

    default List<TriggerRecordEntity> getListByTaskId(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return new ArrayList<>();
        }
        QueryWrapper<TriggerRecordEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TriggerRecordEntity::getTaskId, taskId).orderByDesc(TriggerRecordEntity::getStartTime);
        return this.selectList(queryWrapper);
    }

    default void create(TriggerRecordEntity entity) {
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(RandomUtil.uuId());
        }
        entity.setCreatorTime(new Date());
        this.insert(entity);
    }

    default void create(TriggerTaskEntity triggerTask, NodeModel nodeModel, FlowableTaskModel taskModel) {
        TriggerRecordEntity entity = new TriggerRecordEntity();
        entity.setId(RandomUtil.uuId());
        entity.setStatus(TriggerRecordEnum.PASSED.getCode());
        Date date = new Date();
        entity.setStartTime(date);
        entity.setEndTime(date);

        entity.setNodeCode(nodeModel.getNodeId());
        entity.setNodeName(nodeModel.getNodeName());
        entity.setNodeId(taskModel.getTaskId());

        entity.setTriggerId(triggerTask.getId());
        if (StringUtil.isNotEmpty(triggerTask.getTaskId())) {
            entity.setTaskId(triggerTask.getTaskId());
        }

        this.insert(entity);
    }

    default void createStart(String triggerId) {
        TriggerRecordEntity entity = new TriggerRecordEntity();
        entity.setId(RandomUtil.uuId());
        entity.setStatus(TriggerRecordEnum.PASSED.getCode());
        Date date = new Date();
        entity.setStartTime(date);
        entity.setEndTime(date);
        entity.setTriggerId(triggerId);
        entity.setNodeCode(FlowNature.START_CODE);
        entity.setNodeName(FlowNature.START_NAME);
        this.insert(entity);
    }

    default void createEnd(String triggerId) {
        TriggerRecordEntity entity = new TriggerRecordEntity();
        entity.setId(RandomUtil.uuId());
        entity.setStatus(TriggerRecordEnum.PASSED.getCode());
        Date date = new Date();
        entity.setStartTime(date);
        entity.setEndTime(date);
        entity.setTriggerId(triggerId);
        entity.setNodeCode(FlowNature.END_CODE);
        entity.setNodeName(FlowNature.END_NAME);
        this.insert(entity);
    }

    default void delete(List<String> triggerTaskIds) {
        if (CollUtil.isNotEmpty(triggerTaskIds)) {
            QueryWrapper<TriggerRecordEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(TriggerRecordEntity::getTriggerId, triggerTaskIds);
            this.delete(queryWrapper);
        }
    }
}

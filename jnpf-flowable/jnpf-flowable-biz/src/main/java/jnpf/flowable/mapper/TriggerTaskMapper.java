package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.entity.TemplateJsonEntity;
import jnpf.flowable.entity.TriggerTaskEntity;
import jnpf.flowable.model.trigger.TriggerPagination;
import jnpf.flowable.model.trigger.TriggerTaskModel;
import jnpf.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/10 17:11
 */
public interface TriggerTaskMapper extends SuperMapper<TriggerTaskEntity> {

    default void saveTriggerTask(TriggerTaskEntity entity) {
        this.insert(entity);
    }

    default void updateTriggerTask(TriggerTaskEntity entity) {
        this.updateById(entity);
    }

    default List<TriggerTaskEntity> getListByTaskId(String taskId, String nodeCode) {
        QueryWrapper<TriggerTaskEntity> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(taskId)) {
            queryWrapper.lambda().eq(TriggerTaskEntity::getTaskId, taskId);
        }
        if (StringUtils.isNotBlank(nodeCode)) {
            queryWrapper.lambda().eq(TriggerTaskEntity::getNodeId, nodeCode);
        }
        queryWrapper.lambda().orderByDesc(TriggerTaskEntity::getStartTime);
        return this.selectList(queryWrapper);
    }

    default boolean existTriggerTask(String taskId, String nodeId) {
        if (StringUtils.isBlank(taskId) || StringUtils.isBlank(nodeId)) {
            return false;
        }
        QueryWrapper<TriggerTaskEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TriggerTaskEntity::getTaskId, taskId)
                .eq(TriggerTaskEntity::getNodeId, nodeId);
        return this.selectCount(queryWrapper) > 0;
    }

    default List<TriggerTaskModel> getList(TriggerPagination pagination) {

        MPJLambdaWrapper<TriggerTaskEntity> wrapper = JoinWrappers.lambda(TriggerTaskEntity.class)
                .select(TriggerTaskEntity::getId, TriggerTaskEntity::getFullName,
                        TriggerTaskEntity::getParentId, TriggerTaskEntity::getParentTime,
                        TriggerTaskEntity::getStatus, TriggerTaskEntity::getStartTime
                )
                .selectAs(TemplateEntity::getStatus, TriggerTaskModel::getTemplateStatus)
                .selectAs(TemplateEntity::getSystemId, TriggerTaskModel::getSystemName)
                .leftJoin(TemplateJsonEntity.class, TemplateJsonEntity::getId, TriggerTaskEntity::getFlowId)
                .leftJoin(TemplateEntity.class, TemplateEntity::getId, TemplateJsonEntity::getTemplateId)
                .isNull(TriggerTaskEntity::getTaskId);

        String keyword = pagination.getKeyword();
        if (StringUtil.isNotEmpty(keyword)) {
            wrapper.and(e -> e.like(TriggerTaskEntity::getFullName, keyword)
                    .or().like(TriggerTaskEntity::getId, keyword));
        }
        if (ObjectUtil.isNotEmpty(pagination.getStartTime()) && ObjectUtil.isNotEmpty(pagination.getEndTime())) {
            wrapper.between(TriggerTaskEntity::getStartTime, new Date(pagination.getStartTime()), new Date(pagination.getEndTime()));
        }
        String systemId = pagination.getSystemId();
        if (ObjectUtil.isNotEmpty(systemId)) {
            wrapper.eq(TemplateEntity::getSystemId, systemId);
        }
        wrapper.orderByDesc(TriggerTaskEntity::getCreatorTime);

        Page<TriggerTaskModel> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        Page<TriggerTaskModel> iPage = this.selectJoinPage(page, TriggerTaskModel.class, wrapper);
        return pagination.setData(iPage.getRecords(), page.getTotal());
    }

    default boolean checkByFlowIds(List<String> flowIds) {
        if (CollUtil.isNotEmpty(flowIds)) {
            QueryWrapper<TriggerTaskEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(TriggerTaskEntity::getFlowId, flowIds);
            return this.selectCount(queryWrapper) > 0;
        }
        return false;
    }


}

package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.google.common.collect.ImmutableList;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.DelegateEntity;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.RecordEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.enums.RecordEnum;
import jnpf.flowable.model.task.FlowMethod;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.task.TaskPagination;
import jnpf.flowable.model.util.FlowNature;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/23 9:20
 */
public interface RecordMapper extends SuperMapper<RecordEntity> {


    // 根据经办主键获取转审/转办记录
    default RecordEntity getTransferRecord(String operatorId) {
        QueryWrapper<RecordEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RecordEntity::getOperatorId, operatorId)
                .in(RecordEntity::getHandleType, RecordEnum.TRANSFER.getCode(), RecordEnum.TRANSFER_PROCESSING.getCode())
                .orderByDesc(RecordEntity::getHandleTime);
        queryWrapper.lambda().select(RecordEntity::getId, RecordEntity::getHandleId);
        List<RecordEntity> list = this.selectPage(new Page<>(1, 1, false), queryWrapper).getRecords();
        if (CollUtil.isNotEmpty(list)) {
            return list.get(0);
        }
        return new RecordEntity();
    }


    default List<RecordEntity> getList(String taskId) {
        QueryWrapper<RecordEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(RecordEntity::getTaskId, taskId).orderByAsc(RecordEntity::getHandleTime);
        return this.selectList(wrapper);
    }

    default void updateRecords(List<RecordEntity> recordList) {
        if (CollUtil.isNotEmpty(recordList)) {
            this.updateById(recordList);
        }
    }

    // 将经办相关的记录作废
    default void invalid(List<OperatorEntity> operatorList) {
        List<String> opIds = operatorList.stream().map(OperatorEntity::getId).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(opIds)) {
            QueryWrapper<RecordEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(RecordEntity::getOperatorId, opIds);
            List<RecordEntity> list = this.selectList(queryWrapper);
            for (RecordEntity recordEntity : list) {
                recordEntity.setStatus(FlowNature.INVALID);
            }
            this.updateById(list);
        }
    }

    // 作废加签记录
    default void invalidAddSignRecord(OperatorEntity operator) {
        if (null != operator && null != operator.getId()) {
            QueryWrapper<RecordEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(RecordEntity::getOperatorId, operator.getId()).eq(RecordEntity::getHandleType, RecordEnum.ADD_SIGN.getCode());
            List<RecordEntity> list = this.selectList(queryWrapper);
            if (CollUtil.isNotEmpty(list)) {
                for (RecordEntity recordEntity : list) {
                    recordEntity.setStatus(FlowNature.INVALID);
                }
                this.updateById(list);
            }
        }
    }

    /**
     * 创建记录
     */
    default void createRecord(FlowMethod flowMethod) {
        FlowModel flowModel = flowMethod.getFlowModel();
        String userId = flowMethod.getUserId();
        OperatorEntity operator = flowMethod.getOperatorEntity();
        Integer type = flowMethod.getType();
        String handId = flowMethod.getHandId();

        // 已存在同意操作的记录（说明是后加签默认同意，无需再次保存）
        if (ObjectUtil.equals(RecordEnum.AUDIT.getCode(), type)) {
            QueryWrapper<RecordEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(RecordEntity::getOperatorId, operator.getId()).eq(RecordEntity::getHandleType, type)
                    .ne(RecordEntity::getStatus, FlowNature.INVALID);
            if (this.selectCount(queryWrapper) > 0) return;
        }

        RecordEntity entity = new RecordEntity();
        entity.setId(RandomUtil.uuId());
        entity.setHandleId(handId);
        if (null != operator) {
            if (StringUtils.isBlank(handId)) {
                if (ObjectUtil.equals(operator.getHandleId(), FlowNature.SYSTEM_CODE)) {
                    entity.setHandleId(FlowNature.SYSTEM_CODE);
                } else {
                    entity.setHandleId(operator.getHandleId());
                }
            }
            if (StringUtils.isBlank(entity.getHandleId())) {
                String loginUserId = UserProvider.getLoginUserId();
                entity.setHandleId(loginUserId);
            }
            entity.setNodeId(operator.getNodeId());
            entity.setNodeCode(operator.getNodeCode());
            entity.setNodeName(operator.getNodeName());
            entity.setTaskId(operator.getTaskId());
            entity.setOperatorId(operator.getId());
            entity.setHandleType(operator.getHandleStatus());
            entity.setHandleTime(operator.getHandleTime() == null ? new Date() : operator.getHandleTime());
            entity.setCreatorTime(operator.getCreatorTime() == null ? new Date() : operator.getCreatorTime());
        }
        entity.setHandleOpinion(flowModel.getHandleOpinion());
        entity.setHandleUserId(userId);
        entity.setSignImg(flowModel.getSignImg());
        if (CollUtil.isNotEmpty(flowModel.getFileList())) {
            entity.setFileList(JsonUtil.getObjectToString(flowModel.getFileList()));
        }
        // 拓展字段
        List<Map<String, Object>> expandField = flowModel.getApprovalField();
        if (CollUtil.isNotEmpty(expandField)) {
            String str = JsonUtil.getObjectToString(expandField);
            entity.setExpandField(str);
        }
        entity.setHandleType(type);
        entity.setStatus(FlowNature.NORMAL);

        this.insert(entity);
    }

    default List<RecordEntity> getList(TaskPagination pagination, List<DelegateEntity> delegateList) {
        String userId = StringUtil.isNotEmpty(pagination.getUserId()) ? pagination.getUserId() : UserProvider.getLoginUserId();
        List<Integer> handleStatus = ImmutableList.of(
                RecordEnum.REJECT.getCode(), RecordEnum.AUDIT.getCode(),
                RecordEnum.BACK.getCode(), RecordEnum.ADD_SIGN.getCode(),
                RecordEnum.TRANSFER.getCode(), RecordEnum.TRANSFER_PROCESSING.getCode()
        );
        MPJLambdaWrapper<RecordEntity> recordWrapper = JoinWrappers.lambda(RecordEntity.class)
                .leftJoin(TaskEntity.class, TaskEntity::getId, RecordEntity::getTaskId)
                .select(RecordEntity::getHandleId)
                .select(RecordEntity::getNodeCode)
                .select(RecordEntity::getTaskId)
                .selectMax(RecordEntity::getHandleTime)
                .in(RecordEntity::getHandleType, handleStatus)
                .isNotNull(RecordEntity::getOperatorId)
                .groupBy(RecordEntity::getTaskId, RecordEntity::getNodeCode, RecordEntity::getHandleId);

        recordWrapper.and(t -> {
            t.eq(RecordEntity::getHandleId, userId);
            for (DelegateEntity delegate : delegateList) {
                if (StringUtil.isNotEmpty(delegate.getFlowId())) {
                    String[] flowIds = delegate.getFlowId().split(",");
                    t.or(tw -> tw.in(TaskEntity::getTemplateId, flowIds)
                            .eq(RecordEntity::getHandleId, delegate.getUserId()).eq(RecordEntity::getCreatorUserId, userId)
                            .between(RecordEntity::getHandleTime, delegate.getStartTime(), delegate.getEndTime()));
                } else {
                    t.or(t1 -> t1.eq(RecordEntity::getHandleId, delegate.getUserId()).eq(RecordEntity::getCreatorUserId, userId)
                            .between(RecordEntity::getHandleTime, delegate.getStartTime(), delegate.getEndTime()));
                }
            }
        });
        return this.selectJoinList(RecordEntity.class, recordWrapper);
    }

    default List<RecordEntity> getRecordList(String taskId, List<Integer> statusList) {
        QueryWrapper<RecordEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(RecordEntity::getTaskId, taskId);
        if (CollUtil.isNotEmpty(statusList)) {
            queryWrapper.lambda().in(RecordEntity::getHandleType, statusList);
        }
        queryWrapper.lambda().orderByAsc(RecordEntity::getHandleTime);
        return this.selectList(queryWrapper);
    }

    default RecordEntity getInfo(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        return this.selectById(id);
    }

    default void create(RecordEntity entity) {
        entity.setId(RandomUtil.uuId());
        this.insert(entity);
    }

    default void update(String id, RecordEntity entity) {
        entity.setId(id);
        this.updateById(entity);
    }

    default void updateStatusToInvalid(String taskId, List<String> nodeCodeList) {
        UpdateWrapper<RecordEntity> wrapper = new UpdateWrapper<>();
        wrapper.lambda().eq(RecordEntity::getTaskId, taskId);
        if (CollUtil.isNotEmpty(nodeCodeList)) {
            wrapper.lambda().in(RecordEntity::getNodeCode, nodeCodeList);
        }
        wrapper.lambda().set(RecordEntity::getStatus, FlowNature.INVALID);
        this.update(wrapper);
    }
}

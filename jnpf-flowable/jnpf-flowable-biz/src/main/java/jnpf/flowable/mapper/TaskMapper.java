package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import jnpf.base.mapper.SuperMapper;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.RecordEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.enums.RecordEnum;
import jnpf.flowable.enums.TaskStatusEnum;
import jnpf.flowable.model.monitor.MonitorVo;
import jnpf.flowable.model.operator.OperatorVo;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.task.TaskPagination;
import jnpf.flowable.model.util.FlowNature;
import jnpf.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/17 15:07
 */
public interface TaskMapper extends SuperMapper<TaskEntity> {


    default boolean checkAsync(String id) {
        List<TaskEntity> childList = this.getChildList(id, TaskEntity::getId, TaskEntity::getIsAsync);
        return childList.stream().anyMatch(e -> FlowNature.CHILD_ASYNC.equals(e.getIsAsync()));
    }

    default List<OperatorVo> getlist(TaskPagination pagination, List<RecordEntity> recordList) {
        if (recordList.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> taskId = recordList.stream().map(RecordEntity::getTaskId).collect(Collectors.toList());
        List<String> nodeCode = recordList.stream().map(RecordEntity::getNodeCode).collect(Collectors.toList());
        List<Date> handleTime = recordList.stream().map(RecordEntity::getHandleTime).collect(Collectors.toList());
        MPJLambdaWrapper<TaskEntity> wrapper = JoinWrappers.lambda(TaskEntity.class)
                .leftJoin(RecordEntity.class, RecordEntity::getTaskId, TaskEntity::getId)
                .leftJoin(OperatorEntity.class, OperatorEntity::getId, RecordEntity::getOperatorId)
                .leftJoin(TemplateEntity.class, TemplateEntity::getId, TaskEntity::getTemplateId)
                .selectAll(TaskEntity.class)
                .selectAs(TaskEntity::getUrgent, OperatorVo::getFlowUrgent)
                .selectAs(RecordEntity::getId, OperatorVo::getId)
                .selectAs(RecordEntity::getNodeName, OperatorVo::getCurrentNodeName)
                .selectAs(RecordEntity::getNodeCode, OperatorVo::getNodeCode)
                .selectAs(RecordEntity::getHandleType, OperatorVo::getStatus)
                .selectAs(RecordEntity::getHandleId, OperatorVo::getHandleId)
                .selectAs(RecordEntity::getHandleTime, OperatorVo::getCreatorTime)
                .selectAs(OperatorEntity::getHandleId, OperatorVo::getOperatorHandleId)
                .selectAs(RecordEntity::getTaskId, OperatorVo::getTaskId)
                .selectAs(TemplateEntity::getSystemId, OperatorVo::getSystemName)
                .selectAs(RecordEntity::getCreatorUserId, OperatorVo::getRecordCreatorUserId)
                .selectAs(TaskEntity::getCreatorUserId, OperatorVo::getCreatorUserId)
                .in(RecordEntity::getTaskId, taskId)
                .in(RecordEntity::getNodeCode, nodeCode)
                .ne(TaskEntity::getStatus, TaskStatusEnum.CANCEL.getCode());

        if (CollUtil.isNotEmpty(handleTime)) {
            List<List<Date>> dateList = Lists.partition(handleTime, 1000);
            wrapper.and(e -> {
                for (List<Date> list : dateList) {
                    e.or().in(RecordEntity::getHandleTime, list);
                }
            });

        }

        Map<Integer, List<Integer>> statusMap = ImmutableMap.of(
                1, ImmutableList.of(RecordEnum.AUDIT.getCode()),// 同意
                2, ImmutableList.of(RecordEnum.REJECT.getCode()),// 拒绝
                3, ImmutableList.of(RecordEnum.TRANSFER.getCode(), RecordEnum.TRANSFER_PROCESSING.getCode()),// 转审
                4, ImmutableList.of(RecordEnum.ADD_SIGN.getCode()),// 加签
                5, ImmutableList.of(RecordEnum.BACK.getCode())// 退回
        );

        Integer status = pagination.getStatus();
        List<Integer> handleType = statusMap.get(status);
        if (ObjectUtil.isNotEmpty(handleType)) {
            wrapper.in(RecordEntity::getHandleType, handleType);
        }
        String keyWord = pagination.getKeyword();
        if (ObjectUtil.isNotEmpty(keyWord)) {
            wrapper.and(t -> t.like(TaskEntity::getEnCode, keyWord).or().like(TaskEntity::getFullName, keyWord));
        }
        //所属分类
        String category = pagination.getFlowCategory();
        if (ObjectUtil.isNotEmpty(category)) {
            List<String> categoryList = Arrays.stream(category.split(",")).collect(Collectors.toList());
            wrapper.in(TaskEntity::getFlowCategory, categoryList);
        }
        // 所属流程
        String templateId = pagination.getTemplateId();
        if (ObjectUtil.isNotEmpty(templateId)) {
            wrapper.eq(TaskEntity::getTemplateId, templateId);
        }
        // 紧急程度
        Integer flowUrgent = pagination.getFlowUrgent();
        if (ObjectUtil.isNotEmpty(flowUrgent)) {
            wrapper.eq(TaskEntity::getUrgent, flowUrgent);
        }
        // 发起人员
        String creatorUserId = pagination.getCreatorUserId();
        if (ObjectUtil.isNotEmpty(creatorUserId)) {
            wrapper.eq(TaskEntity::getCreatorUserId, creatorUserId);
        }
        // 日期范围（近7天、近1月、近3月、自定义）
        if (ObjectUtil.isNotEmpty(pagination.getStartTime()) && ObjectUtil.isNotEmpty(pagination.getEndTime())) {
            wrapper.between(TaskEntity::getStartTime, new Date(pagination.getStartTime()), new Date(pagination.getEndTime()));
        }
        //应用主建
        String systemId = pagination.getSystemId();
        if (ObjectUtil.isNotEmpty(systemId)) {
            wrapper.eq(TemplateEntity::getSystemId, systemId);
        }
        wrapper.orderByDesc(RecordEntity::getHandleTime);

        Page<OperatorVo> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        Page<OperatorVo> data = this.selectJoinPage(page, OperatorVo.class, wrapper);
        for (OperatorVo operatorVo : data.getRecords()) {
            if (StringUtil.isEmpty(operatorVo.getOperatorHandleId())
                    || ObjectUtil.equals(operatorVo.getStatus(), RecordEnum.TRANSFER.getCode())) {
                continue;
            }

            boolean isUser = ObjectUtil.equals(operatorVo.getHandleId(), operatorVo.getRecordCreatorUserId());
            operatorVo.setDelegateUser(!isUser ? operatorVo.getRecordCreatorUserId() : null);
        }
        return pagination.setData(data.getRecords(), data.getTotal());
    }

    default List<TaskEntity> subFlowInfo(FlowModel flowModel) {
        String taskId = flowModel.getTaskId();
        String nodeCode = flowModel.getNodeCode();
        QueryWrapper<TaskEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(TaskEntity::getParentId, taskId).eq(TaskEntity::getSubCode, nodeCode);
        return this.selectList(wrapper);
    }

    default List<TaskEntity> getChildList(String id, SFunction<TaskEntity, ?>... columns) {
        return getChildList(ImmutableList.of(id), columns);
    }

    default List<TaskEntity> getChildList(List<String> id, SFunction<TaskEntity, ?>... columns) {
        QueryWrapper<TaskEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(columns).in(TaskEntity::getParentId, id);
        return this.selectList(queryWrapper);
    }

    default List<String> getChildAllList(String id) {
        List<String> idAll = new ArrayList<>();
        List<String> idList = ImmutableList.of(id);
        this.deleTaskAll(idList, idAll);
        return idAll;
    }

    default void getChildList(String id, boolean suspend, List<String> list) {
        List<TaskEntity> taskAll = this.getChildList(id, TaskEntity::getId, TaskEntity::getIsAsync);
        if (suspend) {
            taskAll = taskAll.stream().filter(t -> FlowNature.CHILD_SYNC.equals(t.getIsAsync())).collect(Collectors.toList());
        }
        for (TaskEntity entity : taskAll) {
            list.add(entity.getId());
            this.getChildList(entity.getId(), suspend, list);
        }
    }

    default void deleTaskAll(List<String> idList, List<String> idAll) {
        idAll.addAll(idList);
        for (String id : idList) {
            List<TaskEntity> taskAll = this.getChildList(id, TaskEntity::getId);
            List<String> list = taskAll.stream().map(TaskEntity::getId).collect(Collectors.toList());
            this.deleTaskAll(list, idAll);
        }
    }

    default List<TaskEntity> getOrderStaList(List<String> ids) {
        List<TaskEntity> list = new ArrayList<>();
        if (CollUtil.isNotEmpty(ids)) {
            QueryWrapper<TaskEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(TaskEntity::getId, ids);
            list = this.selectList(queryWrapper);
        }
        return list;
    }

    default List<TaskEntity> getTaskByTemplate(String templateId) {
        QueryWrapper<TaskEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TaskEntity::getTemplateId, templateId);
        return this.selectList(queryWrapper);
    }

    default TaskEntity getInfoSubmit(String id, SFunction<TaskEntity, ?>... columns) {
        List<TaskEntity> list = getInfosSubmit(new String[]{id}, columns);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    default List<TaskEntity> getInfosSubmit(String[] ids, SFunction<TaskEntity, ?>... columns) {
        List<TaskEntity> resultList = Collections.emptyList();
        if (ids == null || ids.length == 0) {
            return resultList;
        }
        LambdaQueryWrapper<TaskEntity> queryWrapper = new LambdaQueryWrapper<>();
        if (ids.length == 1) {
            queryWrapper.select(columns).and(
                    t -> t.eq(TaskEntity::getId, ids[0])
            );
            resultList = this.selectList(queryWrapper);
            if (resultList.isEmpty()) {
                queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.select(columns).and(
                        t -> t.eq(TaskEntity::getFlowId, ids[0])
                );
                resultList = this.selectList(queryWrapper);
            }
        } else {
            queryWrapper.select(TaskEntity::getId).and(t ->
                t.in(TaskEntity::getId, Arrays.asList(ids)).or().in(TaskEntity::getId,  Arrays.asList(ids))
           );
            List<String> resultIds = this.selectList(queryWrapper).stream().map(TaskEntity::getId).collect(Collectors.toList());
            if (!resultIds.isEmpty()) {
                queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.select(columns).in(TaskEntity::getId, resultIds);
                resultList = this.selectList(queryWrapper);
            }
        }
        return resultList;
    }


    default List<MonitorVo> getMonitorList(TaskPagination pagination) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;

        MPJLambdaWrapper<TaskEntity> wrapper = JoinWrappers.lambda(TaskEntity.class)
                .selectAll(TaskEntity.class)
                .selectAs(TaskEntity::getCreatorUserId, MonitorVo::getCreatorUser)
                .selectAs(TaskEntity::getUrgent, MonitorVo::getFlowUrgent)
                .selectAs(TaskEntity::getFlowName, MonitorVo::getFlowName)
                .selectAs(TemplateEntity::getSystemId, MonitorVo::getSystemName)
                .leftJoin(TemplateEntity.class, TemplateEntity::getId, TaskEntity::getTemplateId)
                .isNotNull(TaskEntity::getStartTime)
                .gt(TaskEntity::getStatus, TaskStatusEnum.TO_BE_SUBMIT.getCode());

        //关键字（流程名称、流程编码）
        String keyWord = pagination.getKeyword();
        if (ObjectUtil.isNotEmpty(keyWord)) {
            flag = true;
            wrapper.and(t -> t.like(TaskEntity::getEnCode, keyWord).or().like(TaskEntity::getFullName, keyWord));
        }
        //日期范围（近7天、近1月、近3月、自定义）
        if (ObjectUtil.isNotEmpty(pagination.getStartTime()) && ObjectUtil.isNotEmpty(pagination.getEndTime())) {
            wrapper.between(TaskEntity::getStartTime, new Date(pagination.getStartTime()), new Date(pagination.getEndTime()));
        }
        //所属流程
        String templateId = pagination.getTemplateId();
        if (ObjectUtil.isNotEmpty(templateId)) {
            flag = true;
            wrapper.eq(TaskEntity::getTemplateId, templateId);
        }
        //流程状态
        Integer status = pagination.getStatus();
        if (ObjectUtil.isNotEmpty(status)) {
            flag = true;
            wrapper.eq(TaskEntity::getStatus, status);
        }
        //紧急程度
        Integer flowUrgent = pagination.getFlowUrgent();
        if (ObjectUtil.isNotEmpty(flowUrgent)) {
            flag = true;
            wrapper.eq(TaskEntity::getUrgent, flowUrgent);
        }
        //所属分类
        String flowCategory = pagination.getFlowCategory();
        if (ObjectUtil.isNotEmpty(flowCategory)) {
            flag = true;
            wrapper.eq(TaskEntity::getFlowCategory, flowCategory);
        }
        // 发起人员
        String creatorUserId = pagination.getCreatorUserId();
        if (StringUtils.isNotBlank(creatorUserId)) {
            flag = true;
            wrapper.eq(TaskEntity::getCreatorUserId, creatorUserId);
        }
        //应用主建
        String systemId = pagination.getSystemId();
        if (ObjectUtil.isNotEmpty(systemId)) {
            wrapper.eq(TemplateEntity::getSystemId, systemId);
        }
        wrapper.orderByDesc(TaskEntity::getStartTime);
        if (flag) {
            wrapper.orderByDesc(TaskEntity::getLastModifyTime);
        }

        Page<MonitorVo> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        Page<MonitorVo> iPage = this.selectJoinPage(page, MonitorVo.class, wrapper);
        return pagination.setData(iPage.getRecords(), page.getTotal());
    }

    default TaskEntity getInfo(String id) throws WorkFlowException {
        QueryWrapper<TaskEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TaskEntity::getId, id == null ? "" : id);
        TaskEntity entity = this.selectOne(queryWrapper);
        if (null == entity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        return entity;
    }

    default List<TaskEntity> getSubTask(String taskId, List<String> nodeCodes) {
        if (StringUtil.isEmpty(taskId)) {
            return new ArrayList<>();
        }
        QueryWrapper<TaskEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TaskEntity::getParentId, taskId);
        if (CollUtil.isNotEmpty(nodeCodes)) {
            queryWrapper.lambda().in(TaskEntity::getSubCode, nodeCodes);
        }
        return this.selectList(queryWrapper);
    }
}

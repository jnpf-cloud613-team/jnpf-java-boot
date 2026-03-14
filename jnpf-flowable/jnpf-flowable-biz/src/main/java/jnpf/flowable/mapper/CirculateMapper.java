package jnpf.flowable.mapper;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import jnpf.base.mapper.SuperMapper;
import jnpf.flowable.entity.CirculateEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.enums.TaskStatusEnum;
import jnpf.flowable.model.operator.OperatorVo;
import jnpf.flowable.model.task.TaskPagination;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 16:00
 */
public interface CirculateMapper extends SuperMapper<CirculateEntity> {

    default List<CirculateEntity> getList(String taskId) {
        return getList(taskId, null);
    }

    default List<CirculateEntity> getList(String taskId, String nodeCode) {
        QueryWrapper<CirculateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CirculateEntity::getTaskId, taskId);
        if (StringUtil.isNotEmpty(nodeCode)) {
            queryWrapper.lambda().eq(CirculateEntity::getNodeCode, nodeCode);
        }
        return this.selectList(queryWrapper);
    }


    default List<OperatorVo> getList(TaskPagination pagination) {
        String userId = StringUtil.isNotEmpty(pagination.getUserId()) ? pagination.getUserId() : UserProvider.getLoginUserId();
        MPJLambdaWrapper<CirculateEntity> wrapper = JoinWrappers.lambda(CirculateEntity.class)
                .selectAll(TaskEntity.class)
                .leftJoin(TaskEntity.class, TaskEntity::getId, CirculateEntity::getTaskId)
                .leftJoin(TemplateEntity.class, TemplateEntity::getId, TaskEntity::getTemplateId)
                .selectAs(TemplateEntity::getSystemId, OperatorVo::getSystemName)
                .selectAs(CirculateEntity::getNodeName, OperatorVo::getNodeName)
                .selectAs(CirculateEntity::getNodeCode, OperatorVo::getNodeCode)
                .selectAs(CirculateEntity::getId, OperatorVo::getId)
                .selectAs(TaskEntity::getId, OperatorVo::getTaskId)
                .selectAs(TaskEntity::getUrgent, OperatorVo::getFlowUrgent)
                .selectAs(CirculateEntity::getCreatorTime, OperatorVo::getCreatorTime)
                .ne(TaskEntity::getStatus, TaskStatusEnum.CANCEL.getCode())
                .eq(CirculateEntity::getUserId, userId);
        //关键字（流程名称、流程编码）
        String keyWord = pagination.getKeyword();
        if (ObjectUtil.isNotEmpty(keyWord)) {
            wrapper.and(t -> t.like(TaskEntity::getEnCode, keyWord).or().like(TaskEntity::getFullName, keyWord));
        }
        //日期范围（近7天、近1月、近3月、自定义）
        if (ObjectUtil.isNotEmpty(pagination.getStartTime()) && ObjectUtil.isNotEmpty(pagination.getEndTime())) {
            wrapper.between(TaskEntity::getStartTime, new Date(pagination.getStartTime()), new Date(pagination.getEndTime()));
        }
        //所属流程
        String templateId = pagination.getTemplateId();
        if (ObjectUtil.isNotEmpty(templateId)) {
            wrapper.eq(TaskEntity::getTemplateId, templateId);
        }
        //所属分类
        String category = pagination.getFlowCategory();
        if (ObjectUtil.isNotEmpty(category)) {
            List<String> categoryList = Arrays.stream(category.split(",")).collect(Collectors.toList());
            wrapper.in(TaskEntity::getFlowCategory, categoryList);
        }
        //是否已读
        Integer status = pagination.getStatus();
        if (ObjectUtil.isNotEmpty(status)) {
            wrapper.in(CirculateEntity::getCirculateRead, status);
        }
        //发起人员
        String creatorUserId = pagination.getCreatorUserId();
        if (ObjectUtil.isNotEmpty(creatorUserId)) {
            wrapper.in(TaskEntity::getCreatorUserId, creatorUserId);
        }
        //紧急程度
        Integer flowUrgent = pagination.getFlowUrgent();
        if (ObjectUtil.isNotEmpty(flowUrgent)) {
            wrapper.in(TaskEntity::getUrgent, flowUrgent);
        }
        //应用主建
        String systemId = pagination.getSystemId();
        if (ObjectUtil.isNotEmpty(systemId)) {
            wrapper.eq(TemplateEntity::getSystemId, systemId);
        }
        wrapper.orderByDesc(CirculateEntity::getCreatorTime);
        Page<OperatorVo> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        Page<OperatorVo> data = this.selectJoinPage(page, OperatorVo.class, wrapper);
        return pagination.setData(data.getRecords(), page.getTotal());
    }

    default List<CirculateEntity> getNodeList(String taskId, String nodeId) {
        QueryWrapper<CirculateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CirculateEntity::getTaskId, taskId).like(CirculateEntity::getNodeId, nodeId)
                .orderByDesc(CirculateEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }
}

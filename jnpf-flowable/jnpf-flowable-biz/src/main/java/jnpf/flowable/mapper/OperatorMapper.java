package jnpf.flowable.mapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.google.common.collect.ImmutableList;
import jnpf.base.mapper.SuperMapper;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.DelegateEntity;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.enums.CategoryEnum;
import jnpf.flowable.enums.OperatorStateEnum;
import jnpf.flowable.enums.TaskStatusEnum;
import jnpf.flowable.enums.TemplateStatueEnum;
import jnpf.flowable.model.operator.OperatorVo;
import jnpf.flowable.model.task.FlowMethod;
import jnpf.flowable.model.task.TaskPagination;
import jnpf.flowable.model.util.FlowNature;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 15:28
 */
public interface OperatorMapper extends SuperMapper<OperatorEntity> {


    default List<OperatorVo> getList(TaskPagination pagination, List<DelegateEntity> delegateList) {
        String loginUserId = StringUtil.isNotEmpty(pagination.getUserId()) ? pagination.getUserId() : UserProvider.getLoginUserId();
        List<Integer> statusList = ImmutableList.of(TemplateStatueEnum.UP.getCode(), TemplateStatueEnum.DOWN_CONTINUE.getCode());
        MPJLambdaWrapper<OperatorEntity> wrapper = JoinWrappers.lambda(OperatorEntity.class)
                .selectAll(OperatorEntity.class)
                .selectAs(OperatorEntity::getStatus, OperatorVo::getStatus)
                .selectAs(OperatorEntity::getNodeName, OperatorVo::getCurrentNodeName)
                .selectAs(TaskEntity::getCreatorUserId, OperatorVo::getCreatorUserId)
                .selectAs(TaskEntity::getUrgent, OperatorVo::getFlowUrgent)
                .selectAs(TaskEntity::getFullName, OperatorVo::getFullName)
                .selectAs(TaskEntity::getFlowName, OperatorVo::getFlowName)
                .selectAs(TaskEntity::getStartTime, OperatorVo::getStartTime)
                .selectAs(TaskEntity::getFlowId, OperatorVo::getFlowId)
                .selectAs(TaskEntity::getFlowVersion, OperatorVo::getFlowVersion)
                .selectAs(TaskEntity::getFlowCategory, OperatorVo::getFlowCategory)
                .selectAs(TemplateEntity::getSystemId, OperatorVo::getSystemName)
                .leftJoin(TaskEntity.class, TaskEntity::getId, OperatorEntity::getTaskId)
                .leftJoin(TemplateEntity.class, TemplateEntity::getId, TaskEntity::getTemplateId)
                .in(TemplateEntity::getStatus, statusList)
                .eq(OperatorEntity::getCompletion, FlowNature.NORMAL)
                .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode())
                .ne(OperatorEntity::getStatus, OperatorStateEnum.WAITING.getCode())
                .ne(TaskEntity::getStatus, TaskStatusEnum.CANCEL.getCode())
                .ne(TaskEntity::getStatus, TaskStatusEnum.REVOKED.getCode());

        CategoryEnum categoryEnum = CategoryEnum.getType(pagination.getCategory());
        switch (categoryEnum) {
            case SIGN: // 待签
                wrapper.isNull(OperatorEntity::getSignTime).isNull(OperatorEntity::getStartHandleTime)
                        .isNull(OperatorEntity::getHandleStatus);
                break;
            case TODO: // 待办
                wrapper.isNotNull(OperatorEntity::getSignTime).isNull(OperatorEntity::getStartHandleTime)
                        .isNull(OperatorEntity::getHandleStatus);
                break;
            case DOING: // 在办
                wrapper.isNotNull(OperatorEntity::getSignTime).isNotNull(OperatorEntity::getStartHandleTime)
                        .isNull(OperatorEntity::getHandleStatus);
                break;
            case BATCH_DOING: // 批量在办
                wrapper.isNotNull(OperatorEntity::getSignTime).isNotNull(OperatorEntity::getStartHandleTime)
                        .isNull(OperatorEntity::getHandleStatus)
                        .ne(OperatorEntity::getStatus, OperatorStateEnum.ASSIST.getCode())
                        .ne(OperatorEntity::getStatus, OperatorStateEnum.ADD_SIGN.getCode());
                break;
            default:
                break;
        }
        Integer status = pagination.getStatus();
        if (null != status) {
            if (status == -2) {// 超时
                wrapper.isNotNull(OperatorEntity::getDuedate);
            } else {// 7.协办  5.退回
                wrapper.eq(OperatorEntity::getStatus, status);
            }
        }
        // 版本
        String flowId = pagination.getFlowId();
        if (ObjectUtil.isNotEmpty(flowId)) {
            wrapper.eq(TaskEntity::getFlowId, flowId);
        }
        String nodeCode = pagination.getNodeCode();
        if (StringUtils.isNotBlank(nodeCode)) {
            wrapper.eq(OperatorEntity::getNodeCode, nodeCode);
        }

        Boolean delegateType = pagination.getDelegateType();
        Map<String, String[]> delegateListAll = new HashMap<>();
        List<String> handleId = new ArrayList<>();
        // 是否委托
        if (Boolean.TRUE.equals(delegateType)) {
            for (DelegateEntity delegate : delegateList) {
                if (StringUtil.isNotEmpty(delegate.getFlowId())) {
                    String[] flowIds = delegate.getFlowId().split(",");
                    delegateListAll.put(delegate.getUserId(), flowIds);
                } else {
                    handleId.add(delegate.getUserId());
                }
            }
        }
        wrapper.and(t -> {
            t.eq(OperatorEntity::getHandleId, loginUserId);
            if (!handleId.isEmpty()) {
                t.or(t1 -> t1.in(OperatorEntity::getHandleId, handleId).ne(OperatorEntity::getStatus, OperatorStateEnum.ASSIST.getCode()));
            }
            if (Boolean.TRUE.equals(delegateType)) {
                for (Map.Entry<String, String[]> stringEntry : delegateListAll.entrySet()) {
                    String key = stringEntry.getKey();
                    t.or(tw -> tw.in(TaskEntity::getTemplateId, (Object) delegateListAll.get(key)).eq(OperatorEntity::getHandleId, key)
                            .ne(OperatorEntity::getStatus, OperatorStateEnum.ASSIST.getCode()));
                }

            }
        });
        //关键字
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
        //应用主键
        String systemId = pagination.getSystemId();
        if (ObjectUtil.isNotEmpty(systemId)) {
            wrapper.eq(TemplateEntity::getSystemId, systemId);
        }
        wrapper.orderByDesc(OperatorEntity::getCreatorTime);

        Page<OperatorVo> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<OperatorVo> operatorVoPage = this.selectJoinPage(page, OperatorVo.class, wrapper);

        for (OperatorVo operatorVo : operatorVoPage.getRecords()) {
            boolean isUser = operatorVo.getHandleId().equals(loginUserId);
            operatorVo.setDelegateUser(!isUser ? operatorVo.getCreatorUserId() : null);
            // 待签状态
            if (Objects.equals(categoryEnum, CategoryEnum.SIGN)) {
                operatorVo.setStatus(OperatorStateEnum.WAIT_SIGN.getCode());
            }
        }
        return pagination.setData(operatorVoPage.getRecords(), page.getTotal());
    }

    // 结束经办
    default void endOperator(FlowMethod flowMethod) {
        String taskId = flowMethod.getTaskId();
        String nodeCode = flowMethod.getNodeCode();
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskId)
                .isNull(OperatorEntity::getHandleStatus);
        if (StringUtil.isNotEmpty(nodeCode)) {
            queryWrapper.lambda().eq(OperatorEntity::getNodeCode, nodeCode);
        }
        List<OperatorEntity> list = this.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            for (OperatorEntity entity : list) {
                entity.setCompletion(FlowNature.ACTION);
            }
            this.updateById(list);
        }
    }

    default List<OperatorEntity> getByNodeCode(String taskId, String nodeCode) {
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskId)
                .eq(OperatorEntity::getNodeCode, nodeCode)
                .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode());
        List<OperatorEntity> list = this.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            return list;
        }
        return new ArrayList<>();
    }

    default List<OperatorEntity> getList(String taskId, List<String> nodeCodes) {
        if (StringUtil.isEmpty(taskId)) {
            return new ArrayList<>();
        }
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskId).ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode());
        if (CollUtil.isNotEmpty(nodeCodes)) {
            queryWrapper.lambda().in(OperatorEntity::getNodeCode, nodeCodes);
        }
        queryWrapper.lambda().orderByDesc(OperatorEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<OperatorEntity> getChildList(String id) {
        QueryWrapper<OperatorEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(OperatorEntity::getParentId, id)
                .ne(OperatorEntity::getStatus, OperatorStateEnum.ASSIST.getCode());
        return this.selectList(wrapper);
    }

    default OperatorEntity getInfo(String id) throws WorkFlowException {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getId, id);
        OperatorEntity operator = this.selectOne(queryWrapper);
        if (null == operator) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        return operator;
    }

    default List<OperatorEntity> getList(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return new ArrayList<>();
        }
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskId).orderByDesc(OperatorEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }
}

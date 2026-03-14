package jnpf.flowable.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.ImmutableList;
import jnpf.base.UserInfo;
import jnpf.base.model.base.SystemBaeModel;
import jnpf.base.model.systemconfig.SysConfigModel;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.*;
import jnpf.flowable.mapper.*;
import jnpf.flowable.model.flowable.FlowAbleUrl;
import jnpf.flowable.model.flowable.FlowableNodeModel;
import jnpf.flowable.model.flowable.NextOrPrevFo;
import jnpf.flowable.model.operator.AddSignModel;
import jnpf.flowable.model.task.FlowMethod;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.templatenode.ButtonModel;
import jnpf.flowable.model.templatenode.TaskNodeModel;
import jnpf.flowable.model.templatenode.nodejson.FileConfig;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.templatenode.nodejson.PrintConfig;
import jnpf.flowable.model.templatenode.nodejson.ProperCond;
import jnpf.flowable.model.util.FlowNature;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/16 17:35
 */
@Component
@RequiredArgsConstructor
public class ButtonUtil {


    private final FlowAbleUrl flowAbleUrl;

    private final ServiceUtil serviceUtil;

    private final FlowUtil flowUtil;

    private final OperatorMapper operatorMapper;

    private final TaskMapper taskMapper;

    private final RevokeMapper revokeMapper;

    private final EventLogMapper eventLogMapper;

    private final RecordMapper recordMapper;

    private final TemplateMapper templateMapper;

    private final TriggerRecordMapper triggerRecordMapper;

    /**
     * 按钮控制
     *
     * @param flowModel 参数
     */
    public ButtonModel handleButton(FlowModel flowModel) throws WorkFlowException {
        String opType = flowModel.getOpType();
        TaskEntity taskEntity = flowModel.getTaskEntity();
        OperatorEntity operatorEntity = flowModel.getOperatorEntity();
        RecordEntity recordEntity = flowModel.getRecordEntity();
        Map<String, Object> formData = flowModel.getFormData();
        List<TaskNodeModel> nodeList = flowModel.getNodeList();
        TemplateEntity template = flowModel.getTemplateEntity();
        UserInfo userInfo = flowModel.getUserInfo() == null ? UserProvider.getUser() : flowModel.getUserInfo();
        String userId = userInfo.getUserId();

        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        Map<String, NodeModel> nodes = new HashMap<>();
        for (TemplateNodeEntity nodeEntity : nodeEntityList) {
            nodes.put(nodeEntity.getNodeCode(), JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class));
        }
        flowModel.setNodes(nodes);
        RevokeEntity revokeEntity = revokeMapper.getRevokeTask(taskEntity.getId());

        // 全局节点
        NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());

        ButtonModel model = new ButtonModel();
        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setNodes(nodes);
        flowMethod.setTaskEntity(taskEntity);
        flowMethod.setFormData(formData);
        flowMethod.setPrintNodeList(nodeList);
        OpTypeEnum type = OpTypeEnum.getType(opType);
        switch (type) {
            case LAUNCH_CREATE: // 我发起的新建/编辑
                // 是否是退回撤回的任务
                List<Integer> taskStatus = ImmutableList.of(TaskStatusEnum.BACKED.getCode(), TaskStatusEnum.RECALL.getCode());
                boolean isBack = taskStatus.contains(taskEntity.getStatus());
                if (isBack) {
                    // 是否是委托的任务
                    boolean isDelegate = StringUtils.isNotBlank(taskEntity.getDelegateUserId());
                    if (isDelegate && ObjectUtil.equals(taskEntity.getParentId(), FlowNature.PARENT_ID)) {

                        model.setHasDelegateSubmitBtn(this.checkDelegateSubmit(userId, template));

                    }
                } else {
                    // 委托发起
                    if (null == taskEntity.getId()) {
                        model.setHasDelegateSubmitBtn(this.checkDelegateSubmit(userId, template));
                    } else {
                        if (ObjectUtil.equals(taskEntity.getParentId(), FlowNature.PARENT_ID)) {
                            model.setHasDelegateSubmitBtn(this.checkDelegateSubmit(userId, template));
                        }
                    }
                }

                //暂存和发起按钮
                boolean commonUser = serviceUtil.isCommonUser(userId);
                if (commonUser) {
                    if (null == taskEntity.getId() || ObjectUtil.equals(taskEntity.getParentId(), FlowNature.PARENT_ID)) {
                        AuthorizeVO authorizeByUser = serviceUtil.getAuthorizeByUser();
                        List<String> systemIdList = authorizeByUser.getSystemList().stream().filter(t -> !Objects.equals(t.getIsMain(), 1)).map(SystemBaeModel::getId).collect(Collectors.toList());
                        if (!systemIdList.isEmpty()) {
                            QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
                            queryWrapper.lambda().in(TemplateEntity::getSystemId, systemIdList)
                                    .eq(TemplateEntity::getVisibleType, FlowNature.ALL);
                            queryWrapper.lambda().select(TemplateEntity::getId);
                            List<TemplateEntity> list = templateMapper.selectList(queryWrapper);
                            List<String> permissionList = list.stream().map(TemplateEntity::getId).collect(Collectors.toList());
                            permissionList.addAll(authorizeByUser.getFlowIdList());
                            if (permissionList.contains(template.getId())) {
                                model.setHasSubmitBtn(true);
                                model.setHasSaveBtn(true);
                            }
                        }
                    } else {
                        model.setHasSubmitBtn(true);
                        model.setHasSaveBtn(true);
                    }
                } else {
                    model.setHasSubmitBtn(true);
                    model.setHasSaveBtn(true);
                }
                break;
            case LAUNCH_DETAIL: // 我发起的详情
                // 打印
                TemplateNodeEntity start = nodeEntityList.stream().filter(e -> e.getNodeType().equals(NodeEnum.START.getType())).findFirst().orElse(null);
                if (null != start) {
                    NodeModel startNode = JsonUtil.getJsonToBean(start.getNodeJson(), NodeModel.class);
                    flowMethod.setNodeModel(startNode);
                    model.setHasPrintBtn(this.checkPrint(flowMethod));
                }
                // 催办
                List<Integer> statusList = ImmutableList.of(TaskStatusEnum.RUNNING.getCode(), TaskStatusEnum.REVOKING.getCode());
                if (Boolean.TRUE.equals(global.getHasInitiatorPressOverdueNode()) && statusList.contains(taskEntity.getStatus())) {
                    QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
                    queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskEntity.getId()).eq(OperatorEntity::getCompletion, FlowNature.NORMAL)
                            .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode())
                            .isNull(OperatorEntity::getHandleStatus).isNotNull(OperatorEntity::getDuedate);
                    List<OperatorEntity> operatorList = operatorMapper.selectList(queryWrapper);
                    if (!operatorList.isEmpty()) {
                        model.setHasPressBtn(true);
                    }
                }
                // 撤回
                if (null == revokeEntity) {
                    flowModel.setFlag(FlowNature.INITIATE_FLAG);
                    if (this.checkRecall(flowModel)) {
                        model.setHasRecallLaunchBtn(true);
                    }
                }
                // 撤销，全局属性开启 且 同意状态的任务才能撤销 且不是子流程
                if (Boolean.TRUE.equals(global.getHasRevoke()
                        && Objects.equals(taskEntity.getStatus(), TaskStatusEnum.PASSED.getCode()))
                        && Objects.equals(taskEntity.getParentId(), FlowNature.PARENT_ID)) {
                    Boolean isExist = revokeMapper.checkExist(taskEntity.getId());
                    if (null == revokeEntity && isExist) {
                        model.setHasRevokeBtn(true);
                    }
                }
                break;
            case SIGN: // 待签事宜
                if (Objects.equals(taskEntity.getStatus(), TaskStatusEnum.PAUSED.getCode())) {
                    break;
                }
                model.setHasSignBtn(true);
                if (null != operatorEntity) {
                    model.setProxyMark(!ObjectUtil.equals(operatorEntity.getHandleId(), userId));
                    NodeModel nodeModel = nodes.get(operatorEntity.getNodeCode());
                    if (this.checkViewStartForm(global, nodeModel)) {
                        model.setHasViewStartFormBtn(true);
                    }
                }
                break;
            case TODO: // 待办事宜
                if (Objects.equals(taskEntity.getStatus(), TaskStatusEnum.PAUSED.getCode())) {
                    break;
                }
                if (Boolean.TRUE.equals(global.getHasSignFor())) {
                    model.setHasReduceSignBtn(true);
                }
                model.setHasTransactBtn(true);
                if (null != operatorEntity) {
                    NodeModel nodeModel = nodes.get(operatorEntity.getNodeCode());
                    if (this.checkViewStartForm(global, nodeModel)) {
                        model.setHasViewStartFormBtn(true);
                    }
                }
                break;
            case DOING: // 在办事宜
                if (Objects.equals(taskEntity.getStatus(), TaskStatusEnum.PAUSED.getCode())) {
                    break;
                }
                if (null == operatorEntity) {
                    break;
                }
                // 待办的opType传的也是3，所以待办的代理标识需要再在办判断
                model.setProxyMark(!ObjectUtil.equals(operatorEntity.getHandleId(), userId));
                if (null != revokeEntity) {
                    model.setHasAuditBtn(true);
                    model.setHasRejectBtn(true);
                    break;
                }
                NodeModel nodeModel = nodes.get(operatorEntity.getNodeCode());
                if (null == nodeModel) {
                    break;
                }
                if (this.checkViewStartForm(global, nodeModel)) {
                    model.setHasViewStartFormBtn(true);
                }
                // 协办状态的经办 只有协办保存按钮
                if (OperatorStateEnum.ASSIST.getCode().equals(operatorEntity.getStatus())) {
                    model.setHasAssistSaveBtn(true);
                    break;
                }
                // 打印
                flowMethod.setNodeModel(nodeModel);
                model.setHasPrintBtn(this.checkPrint(flowMethod));
                if (Boolean.TRUE.equals(nodeModel.getHasAuditBtn())) {
                    model.setHasAuditBtn(true);
                }
                if (Boolean.TRUE.equals(nodeModel.getHasRejectBtn()) && ObjectUtil.equals(operatorEntity.getIsProcessing(), FlowNature.NOT_PROCESSING)) {
                    model.setHasRejectBtn(true);
                }
                if (Boolean.TRUE.equals(nodeModel.getHasBackBtn()) && taskEntity.getRejectDataId() == null) {
                    model.setHasBackBtn(true);
                }
                if (Boolean.TRUE.equals(nodeModel.getHasSaveAuditBtn())) {
                    model.setHasSaveAuditBtn(true);
                }
                List<Integer> operatorState = ImmutableList.of(OperatorStateEnum.ADD_SIGN.getCode(), OperatorStateEnum.TRANSFER.getCode());
                if (Boolean.TRUE.equals(nodeModel.getHasFreeApproverBtn()) && !ObjectUtil.equals(operatorEntity.getStatus(), OperatorStateEnum.TRANSFER.getCode())) {

                    model.setHasFreeApproverBtn(this.checkAddSign(operatorEntity, null, false));

                }
                if (Boolean.TRUE.equals(nodeModel.getHasTransferBtn()
                        && !operatorState.contains(operatorEntity.getStatus()))
                        && ObjectUtil.equals(operatorEntity.getParentId(), FlowNature.PARENT_ID)) {

                    model.setHasTransferBtn(true);

                }
                if (Boolean.TRUE.equals(nodeModel.getHasAssistBtn())) {
                    model.setHasAssistBtn(true);
                }
                break;
            case DONE: // 已办事宜
                if (null == operatorEntity || operatorEntity.getId() == null) {
                    break;
                }
                // 减签
                NodeModel node = nodes.get(operatorEntity.getNodeCode());
                // 节点属性开启、经办存在加签信息、记录是加签操作
                if (node != null) {
                    if (null != recordEntity) {
                        if (Boolean.TRUE.equals(node.getHasReduceApproverBtn() && null != operatorEntity.getHandleParameter())
                                && recordEntity.getHandleType().equals(RecordEnum.ADD_SIGN.getCode())) {
                            QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
                            queryWrapper.lambda().eq(OperatorEntity::getTaskId, operatorEntity.getTaskId())
                                    .eq(OperatorEntity::getParentId, operatorEntity.getId())
                                    .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode())
                                    .ne(OperatorEntity::getStatus, OperatorStateEnum.ASSIST.getCode())
                                    .eq(OperatorEntity::getCompletion, FlowNature.NORMAL).isNull(OperatorEntity::getHandleStatus);
                            List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
                            if (CollUtil.isNotEmpty(list)) {
                                model.setHasReduceApproverBtn(true);
                            }
                        }
                        // 审批撤回
                        List<Integer> handleTypes = ImmutableList.of(RecordEnum.AUDIT.getCode(), RecordEnum.REJECT.getCode());
                        if (handleTypes.contains(recordEntity.getHandleType())) {
                            flowModel.setFlag(FlowNature.APPROVAL_FLAG);
                            if (this.checkRecall(flowModel)) {
                                model.setHasRecallAuditBtn(true);
                            }
                        }
                    }
                    flowMethod.setNodeModel(node);
                    // 打印
                    model.setHasPrintBtn(this.checkPrint(flowMethod));
                    if (this.checkViewStartForm(global, node)) {
                        model.setHasViewStartFormBtn(true);
                    }
                }
                break;
            case CIRCULATE: // 抄送事宜
                if (null != operatorEntity) {
                    NodeModel node1 = nodes.get(operatorEntity.getNodeCode());
                    if (this.checkViewStartForm(global, node1)) {
                        model.setHasViewStartFormBtn(true);
                    }
                }
                break;
            case MONITOR: // 流程监控
                // 终止状态下只有复活
                if (ObjectUtil.equals(taskEntity.getStatus(), TaskStatusEnum.CANCEL.getCode())
                        && ObjectUtil.equals(taskEntity.getParentId(), FlowNature.PARENT_ID) && ObjectUtil.equals(template.getStatus(), TemplateStatueEnum.UP.getCode())) {
                    List<TriggerRecordEntity> triggerRecordList = triggerRecordMapper.getListByTaskId(taskEntity.getId());
                    long count = triggerRecordList.stream().filter(e -> ObjectUtil.equals(e.getStatus(), TriggerRecordEnum.EXCEPTION.getCode())).count();
                    if (count == 0) {
                        model.setHasActivateBtn(true);
                    }
                }
                // 挂起状态下只有恢复
                if (ObjectUtil.equals(taskEntity.getStatus(), TaskStatusEnum.PAUSED.getCode())
                        && !ObjectUtil.equals(taskEntity.getIsRestore(), FlowNature.NOT_RESTORE)) {

                    model.setHasRebootBtn(true);

                }
                // 运行状态下，终止、挂起
                if (ObjectUtil.equals(taskEntity.getStatus(), TaskStatusEnum.RUNNING.getCode())) {
                    // 子流程不允许终止
                    if (ObjectUtil.equals(taskEntity.getParentId(), FlowNature.PARENT_ID)) {
                        model.setHasCancelBtn(true);
                    }
                    model.setHasPauseBtn(true);
                    if (this.checkAssign(taskEntity, nodes)) {
                        model.setHasAssignBtn(true);
                    }
                }
                // 归档按钮
                FileConfig config = global.getFileConfig();
                if ((config.getOn() && StringUtils.isNotBlank(config.getTemplateId()))
                        && (ObjectUtil.isNotEmpty(taskEntity.getEndTime()) || ObjectUtil.equals(taskEntity.getStatus(), TaskStatusEnum.CANCEL.getCode()))
                        && Boolean.TRUE.equals((serviceUtil.checkFlowFile(taskEntity.getId())))) {

                    model.setHasFileBtn(true);

                }
                break;
        }
        return model;
    }

    // 判断指派，当前节点全是子流程，就隐藏
    public boolean checkAssign(TaskEntity taskEntity, Map<String, NodeModel> nodes) {
        String currentNodeCode = taskEntity.getCurrentNodeCode();
        if (StringUtils.isNotBlank(currentNodeCode)) {
            List<String> currentNodeList = Arrays.stream(currentNodeCode.split(",")).collect(Collectors.toList());
            for (String nodeCode : currentNodeList) {
                NodeModel nodeModel = nodes.get(nodeCode);
                if (null != nodeModel && !Objects.equals(nodeModel.getType(), NodeEnum.SUB_FLOW.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    // 委托发起按钮
    public boolean checkDelegateSubmit(String userId, TemplateEntity template) {
        List<DelegateEntity> delegateList = flowUtil.getByToUserId(userId, 0);
        if (CollUtil.isNotEmpty(delegateList) && ObjectUtil.equals(template.getStatus(), TemplateStatueEnum.UP.getCode())) {
            for (DelegateEntity delegateEntity : delegateList) {
                String flowId = delegateEntity.getFlowId();
                if (StringUtils.isNotBlank(flowId)) {
                    if (flowId.contains(template.getId())) {
                        if (ObjectUtil.equals(template.getVisibleType(), FlowNature.ALL)) {
                            return true;
                        }
                        List<String> launchPermission = serviceUtil.getPermission(delegateEntity.getUserId());
                        if (launchPermission.contains(template.getId())) {
                            return true;
                        }
                    }
                } else {
                    // 全部流程
                    if (ObjectUtil.equals(template.getVisibleType(), FlowNature.ALL)) {
                        return true;
                    }
                    List<String> launchPermission = serviceUtil.getPermission(delegateEntity.getUserId());
                    if (launchPermission.contains(template.getId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 查看发起表单
     */
    public boolean checkViewStartForm(NodeModel global, NodeModel nodeModel) {
       return null != nodeModel && global.getHasAloneConfigureForms()
               && StringUtils.isNotBlank(nodeModel.getFormId());
    }

    /**
     * 判断撤回
     *
     * @param flowModel 参数
     */
    public boolean checkRecall(FlowModel flowModel) throws WorkFlowException {
        String deploymentId = flowModel.getDeploymentId();
        String currentNodeCode = flowModel.getNodeEntity().getNodeCode();
        TaskEntity taskEntity = flowModel.getTaskEntity();
        RecordEntity recordEntity = flowModel.getRecordEntity();
        Map<String, NodeModel> nodes = flowModel.getNodes();
        // 判断撤回的标识  1.发起撤回  2.审批撤回
        int flag = flowModel.getFlag();
        boolean isException = flowModel.getIsException();

        NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
        Integer recallRule = global.getRecallRule();

        // 全局撤回配置为1 直接不允许撤回 (流程撤回规则  1: 不允许撤回  2: 发起节点允许撤回  3:所有节点允许撤回)
        if (recallRule == null || Objects.equals(recallRule, FlowNature.NOT_ALLOWED)) {
            return false;
        }

        // 不是进行中的任务、存在退回冻结数据
        if (!Objects.equals(TaskStatusEnum.RUNNING.getCode(), taskEntity.getStatus()) || null != taskEntity.getRejectDataId()) {
            if (isException) {
                throw new WorkFlowException(MsgCode.WF036.get());
            }
            return false;
        }
        // 经办未审批 说明是已撤回的，不允许撤回
        OperatorEntity operator = operatorMapper.getInfo(recordEntity.getOperatorId());
        if (null != operator) {
            if (!ObjectUtil.equals(operator.getParentId(), FlowNature.PARENT_ID)) {
                if (isException) {
                    throw new WorkFlowException(MsgCode.WF077.get());
                }
                return false;
            }
            // 判断依次审批
            boolean inTurn = this.checkInTurn(operator, isException);
            if (!inTurn && !isException) {
                return false;
            }
            //判断逐级审批
            boolean inStep = this.checkStep(operator, isException);
            if (!inStep && !isException) {
                return false;
            }
            if (operator.getHandleStatus() == null) {
                if (isException) {
                    throw new WorkFlowException(MsgCode.WF006.get());
                }
                return false;
            } else if (null == operator.getHandleTime()) {
                // 后加签的同意未真正审批，不允许撤回
                List<OperatorEntity> childList = operatorMapper.getChildList(operator.getId());
                if (CollUtil.isNotEmpty(childList)) {
                    if (isException) {
                        throw new WorkFlowException(MsgCode.WF006.get());
                    }
                    return false;
                }
            }
        } else {
            // 发起撤回的校验
            QueryWrapper<RecordEntity> recordWrapper = new QueryWrapper<>();
            recordWrapper.lambda().eq(RecordEntity::getTaskId, taskEntity.getId()).ne(RecordEntity::getStatus, FlowNature.INVALID);
            List<RecordEntity> recordList = recordMapper.selectList(recordWrapper);
            if (CollUtil.isNotEmpty(recordList)) {
                RecordEntity submit = recordList.stream().filter(e -> ObjectUtil.equals(e.getHandleType(), RecordEnum.SUBMIT.getCode())).findFirst().orElse(null);
                if (null != submit) {
                    recordList = recordList.stream().filter(e -> !ObjectUtil.equals(e.getNodeCode(), submit.getNodeCode())).collect(Collectors.toList());
                }
                if (!recordList.isEmpty()) {
                    if (isException) {
                        throw new WorkFlowException(MsgCode.WF006.get());
                    }
                    return false;
                }
            }

            //外部节点
            List<EventLogEntity> eventLogList = eventLogMapper.getList(taskEntity.getId()).stream().filter(e -> Objects.equals(FlowNature.SUCCESS, e.getStatus())).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(eventLogList) && !eventLogList.isEmpty()) {
                if (isException) {
                    throw new WorkFlowException(MsgCode.WF006.get());
                }
                return false;

            }
        }
        // 获取节点的下一级节点,判断撤回
        NextOrPrevFo fo = new NextOrPrevFo();
        fo.setDeploymentId(deploymentId);
        fo.setTaskKey(currentNodeCode);
        List<FlowableNodeModel> nextModels = flowAbleUrl.getNext(fo);
        List<String> nextCodes = nextModels.stream().map(FlowableNodeModel::getId).collect(Collectors.toList());
        // 审批撤回需要用到下一级节点
        flowModel.setNextCodes(nextCodes);

        if (CollUtil.isNotEmpty(nextCodes)) {
            QueryWrapper<TaskEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(TaskEntity::getParentId, taskEntity.getId())
                    .in(TaskEntity::getSubCode, nextCodes);
            List<TaskEntity> list = taskMapper.selectList(queryWrapper);
            if (CollUtil.isNotEmpty(list)) {
                // 异步子流程，不允许撤回
                long asyncCount = list.stream().filter(e -> Objects.equals(e.getIsAsync(), FlowNature.CHILD_ASYNC)).count();
                if (asyncCount > 0) {
                    return false;
                }
                long count = list.stream().filter(e -> !Objects.equals(e.getStatus(), TaskStatusEnum.TO_BE_SUBMIT.getCode())).count();
                if (count > 0) {
                    if (isException) {
                        throw new WorkFlowException(MsgCode.WF036.get());
                    }
                    return false;
                }
            }

            //外部节点只要有成功数据，不允许撤回
            List<EventLogEntity> eventLogList = eventLogMapper.getList(taskEntity.getId(), nextCodes).stream().filter(e -> Objects.equals(FlowNature.SUCCESS, e.getStatus())).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(eventLogList) && !eventLogList.isEmpty()) {
                if (isException) {
                    throw new WorkFlowException(MsgCode.WF006.get());
                }
                return false;

            }
        }

        if (Objects.equals(flag, FlowNature.APPROVAL_FLAG) && Objects.equals(recallRule, FlowNature.START_ALLOWED)) {
            return false;
        }
        if (CollUtil.isEmpty(nextCodes)) {
            return true;
        }
        // 存在已办理的经办，撤回后可以重新提交（下个审批节点只进行了签收 此时还可以撤回）
        QueryWrapper<OperatorEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(OperatorEntity::getTaskId, taskEntity.getId());
        if (CollUtil.isNotEmpty(nextCodes)) {
            wrapper.lambda().in(OperatorEntity::getNodeCode, nextCodes);
        }
        wrapper.lambda().orderByDesc(OperatorEntity::getCreatorTime);
        List<OperatorEntity> list = operatorMapper.selectList(wrapper);
        boolean mark = this.checkDraftAndRecord(list);
        if (mark) {
            if (isException) {
                throw new WorkFlowException(MsgCode.WF077.get());
            }
            return false;
        }

        //加签和被加签人不能撤回
        if (operator != null) {
            if (!Objects.equals(operator.getParentId(), FlowNature.PARENT_ID)) {
                return false;
            }
            if (StringUtil.isNotEmpty(operator.getHandleParameter())) {
                return false;
            }
        }
        return true;
    }

    // 依次审批的撤回判断
    public boolean checkInTurn(OperatorEntity operator, boolean isException) throws WorkFlowException {
        if (StringUtils.isNotBlank(operator.getHandleAll())) {
            List<String> handleIds = Arrays.stream(operator.getHandleAll().split(",")).collect(Collectors.toList());
            int index = handleIds.indexOf(operator.getHandleId());
            if (index != -1 && index < handleIds.size() - 1) {
                String nextHandleId = handleIds.get(index + 1);
                QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
                queryWrapper.lambda().eq(OperatorEntity::getHandleId, nextHandleId).eq(OperatorEntity::getParentId, operator.getParentId())
                        .eq(OperatorEntity::getNodeId, operator.getNodeId()).orderByDesc(OperatorEntity::getCreatorTime);
                List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
                if (CollUtil.isNotEmpty(list)) {
                    OperatorEntity nextOperator = list.get(0);
                    if (StringUtil.isNotEmpty(nextOperator.getDraftData())) {
                        if (isException) {
                            throw new WorkFlowException(MsgCode.WF036.get());
                        }
                        return false;
                    }
                    QueryWrapper<RecordEntity> recordWrapper = new QueryWrapper<>();
                    recordWrapper.lambda().eq(RecordEntity::getOperatorId, nextOperator.getId()).ne(RecordEntity::getStatus, FlowNature.INVALID);
                    List<RecordEntity> recordList = recordMapper.selectList(recordWrapper);
                    if (CollUtil.isNotEmpty(recordList)) {
                        if (isException) {
                            throw new WorkFlowException(MsgCode.WF036.get());
                        }
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // 逐级审批的撤回判断
    public boolean checkStep(OperatorEntity operator, boolean isException) throws WorkFlowException {
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId());
        List<OperatorEntity> operatorList = operatorMapper.selectList(queryWrapper);
        List<OperatorEntity> list = operatorList.stream().filter(e -> Objects.equals(e.getHandleAll(), operator.getId())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(list)) {
            OperatorEntity nextOperator = list.get(0);
            if (StringUtil.isNotEmpty(nextOperator.getDraftData())) {
                if (isException) {
                    throw new WorkFlowException(MsgCode.WF036.get());
                }
                return false;
            }
            QueryWrapper<RecordEntity> recordWrapper = new QueryWrapper<>();
            recordWrapper.lambda().eq(RecordEntity::getOperatorId, nextOperator.getId()).ne(RecordEntity::getStatus, FlowNature.INVALID);
            List<RecordEntity> recordList = recordMapper.selectList(recordWrapper);
            if (CollUtil.isNotEmpty(recordList)) {
                if (isException) {
                    throw new WorkFlowException(MsgCode.WF036.get());
                }
                return false;
            }
        }
        return true;
    }

    public boolean checkDraftAndRecord(List<OperatorEntity> list) {
        return this.checkDraftAndRecord(list, false);
    }

    // 判断草稿、操作记录
    public boolean checkDraftAndRecord(List<OperatorEntity> list, boolean filterAddSign) {
        list = list.stream().filter(e -> !ObjectUtil.equals(e.getStatus(), OperatorStateEnum.FUTILITY.getCode())).collect(Collectors.toList());
        if (CollUtil.isEmpty(list)) {
            return false;
        }
        // 只要有操作就不让撤回，草稿数据（暂存）
        long draftCount = list.stream().filter(e -> StringUtil.isNotEmpty(e.getDraftData())).count();
        boolean mark = false;
        // 不存在草稿数据，继续根据记录判断
        if (draftCount == 0) {
            List<String> collect = list.stream().map(OperatorEntity::getId).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(collect)) {
                QueryWrapper<RecordEntity> recordWrapper = new QueryWrapper<>();
                recordWrapper.lambda().in(RecordEntity::getOperatorId, collect).ne(RecordEntity::getStatus, FlowNature.INVALID);
                List<RecordEntity> recordList = recordMapper.selectList(recordWrapper);
                if (filterAddSign) {
                    List<Integer> status = ImmutableList.of(RecordEnum.ADD_SIGN.getCode(), RecordEnum.SUBTRACT_SIGN.getCode());
                    recordList = recordList.stream().filter(e -> !status.contains(e.getHandleType())).collect(Collectors.toList());
                }
                mark = !recordList.isEmpty();
            }
        } else {
            mark = true;
        }
        return mark;
    }

    /**
     * 判断打印
     */
    public boolean checkPrint(FlowMethod flowMethod) {
        NodeModel nodeModel = flowMethod.getNodeModel();
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        List<TaskNodeModel> nodeList = flowMethod.getPrintNodeList();

        // 发起除了草稿状态都显示
        if (Objects.equals(taskEntity.getStatus(), TaskStatusEnum.TO_BE_SUBMIT.getCode())) {
            return false;
        }
        PrintConfig printConfig = nodeModel.getPrintConfig();
        if (Boolean.TRUE.equals(printConfig.getOn())) {
            // 打印没有选择模版，按钮不显示
            if (CollUtil.isEmpty(printConfig.getPrintIds())) {
                return false;
            }
            switch (PrintEnum.getPrint(printConfig.getConditionType())) {
                case NONE:
                    // 不限制
                    return true;
                case NODE_END:
                    // 节点结束
                    // 开始节点直接返回true，除草稿状态
                    if (nodeModel.getType().equals(NodeEnum.START.getType())
                            && !taskEntity.getStatus().equals(TaskStatusEnum.TO_BE_SUBMIT.getCode())) {
                        return true;
                    }
                    TaskNodeModel taskNode = nodeList.stream().filter(e -> e.getNodeCode().equals(nodeModel.getNodeId())).findFirst().orElse(null);
                    if (null != taskNode && Objects.equals(taskNode.getType(), NodeTypeEnum.PASS.getType())) {
                        return true;
                    }
                    break;
                case FLOW_END:
                    // 流程结束
                    List<Integer> status = ImmutableList.of(TaskStatusEnum.PASSED.getCode(), TaskStatusEnum.REJECTED.getCode());
                    if (status.contains(taskEntity.getStatus())) {
                        return true;
                    }
                    break;
                case CONDITIONS:
                    // 条件设置
                    List<ProperCond> conditions = printConfig.getConditions();
                    String matchLogic = printConfig.getMatchLogic();
                    UserInfo userInfo = UserProvider.getUser();
                    flowMethod.setUserInfo(userInfo);
                    flowMethod.setUserEntity(serviceUtil.getUserInfo(userInfo.getUserId()));
                    flowMethod.setConditions(conditions);
                    flowMethod.setMatchLogic(matchLogic);
                    if (FlowJsonUtil.nodeConditionDecide(flowMethod)) {
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    /**
     * 判断加签
     */
    public void checkAddSign(OperatorEntity operator, AddSignModel parameter) throws WorkFlowException {
        this.checkAddSign(operator, parameter, true);
    }

    public boolean checkAddSign(OperatorEntity operator, AddSignModel parameter, Boolean isException) throws WorkFlowException {
        int level = 1;
        if (!ObjectUtil.equals(operator.getParentId(), FlowNature.PARENT_ID)) {
            OperatorEntity parent = operatorMapper.selectById(operator.getParentId());
            if (null != parent) {
                String handleParameter = parent.getHandleParameter() == null ? operator.getHandleParameter() : parent.getHandleParameter();
                AddSignModel jsonToBean = JsonUtil.getJsonToBean(handleParameter, AddSignModel.class);
                level = jsonToBean.getLevel() + 1;
            }
        }
        SysConfigModel sysConfig = serviceUtil.getSysConfig();
        Integer addSignLevel = sysConfig.getAddSignLevel();
        if (level > addSignLevel) {
            if (Boolean.TRUE.equals(isException)) {
                throw new WorkFlowException(MsgCode.WF143.get());
            }
            return false;
        }
        if (null != parameter) {
            parameter.setLevel(level);
        }
        return true;
    }
}

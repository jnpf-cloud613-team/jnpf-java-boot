package jnpf.flowable.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.ColumnDataModel;
import jnpf.base.model.VisualDevJsonModel;
import jnpf.base.model.VisualWebTypeEnum;
import jnpf.base.model.flow.FlowFormDataModel;
import jnpf.base.model.flow.FlowStateModel;
import jnpf.base.model.schedule.ScheduleNewCrForm;
import jnpf.constant.DataInterfaceVarConst;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.database.model.query.SuperJsonModel;
import jnpf.database.model.query.SuperQueryJsonModel;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.*;
import jnpf.flowable.job.FlowJobUtil;
import jnpf.flowable.mapper.*;
import jnpf.flowable.model.candidates.CandidateCheckFo;
import jnpf.flowable.model.candidates.CandidateCheckVo;
import jnpf.flowable.model.candidates.CandidateListModel;
import jnpf.flowable.model.candidates.CandidateUserVo;
import jnpf.flowable.model.flowable.*;
import jnpf.flowable.model.free.FreeConnect;
import jnpf.flowable.model.message.FlowMsgModel;
import jnpf.flowable.model.operator.AddSignModel;
import jnpf.flowable.model.free.FreeModel;
import jnpf.flowable.model.record.NodeRecordModel;
import jnpf.flowable.model.record.RecordVo;
import jnpf.flowable.model.task.*;
import jnpf.flowable.model.templatejson.FlowFormModel;
import jnpf.flowable.model.templatejson.FlowParamModel;
import jnpf.flowable.model.templatenode.FlowErrorModel;
import jnpf.flowable.model.templatenode.TemplateNodeUpFrom;
import jnpf.flowable.model.templatenode.nodejson.*;
import jnpf.flowable.model.trigger.*;
import jnpf.flowable.model.util.*;
import jnpf.flowable.model.xml.FlowXmlUtil;
import jnpf.flowable.model.xml.XmlModel;
import jnpf.message.model.SentMessageForm;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.FormDataModel;
import jnpf.model.visualjson.TableFields;
import jnpf.model.visualjson.TableModel;
import jnpf.model.visualjson.config.ConfigModel;
import jnpf.onlinedev.model.PaginationModel;
import jnpf.permission.entity.*;
import jnpf.util.*;
import jnpf.util.text.CharsetKit;
import jnpf.util.visiual.JnpfKeyConsts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperatorUtil {


    private final ServiceUtil serviceUtil;

    private final MsgUtil msgUtil;

    private final FlowAbleUrl flowAbleUrl;

    private final RedisUtil redisUtil;

    private final ConditionUtil conditionUtil;

    private final FlowUtil flowUtil;

    private final TaskUtil taskUtil;


    private final CandidatesMapper candidatesMapper;

    private final LaunchUserMapper launchUserMapper;

    private final RecordMapper recordMapper;

    private final TaskMapper taskMapper;

    private final OperatorMapper operatorMapper;

    private final TemplateMapper templateMapper;

    private final TemplateJsonMapper templateJsonMapper;

    private final TemplateNodeMapper templateNodeMapper;

    private final TaskLineMapper taskLineMapper;

    private final RevokeMapper revokeMapper;

    private final NodeRecordMapper nodeRecordMapper;

    private final SubtaskDataMapper subtaskDataMapper;

    private final CirculateMapper circulateMapper;

    private final RejectDataMapper rejectDataMapper;

    private final EventLogMapper eventLogMapper;

    private final TriggerTaskMapper triggerTaskMapper;

    private final TriggerRecordMapper triggerRecordMapper;

    private final TriggerLaunchflowMapper triggerLaunchflowMapper;


    //-------------------------------operatorUtil------------------------------------------------------------

    // 组建FlowMethod
    public FlowMethod getFlowMethod(FlowMethod flowMethod) {
        FlowModel flowModel = flowMethod.getFlowModel();
        String nodeCode = flowMethod.getNodeCode();
        Map<String, NodeModel> nodes = flowMethod.getNodes();
        FlowModel model = JsonUtil.getJsonToBean(flowModel, FlowModel.class);
        model.setHandleOpinion("系统审批");
        model.setSignImg(null);
        model.setFileList(null);
        model.setHandleStatus(FlowNature.AUDIT_COMPLETION);
        model.setTaskEntity(flowMethod.getTaskEntity());
        model.setFlowableTaskId(flowMethod.getFlowableTaskId());
        FlowMethod method = new FlowMethod();
        method.setFlowModel(model);
        NodeModel nodeModel = nodes.get(nodeCode);
        method.setNodeModel(nodeModel);
        method.setNodeCode(nodeCode);
        return method;
    }

    // 处理撤销的经办
    public void handleRevokeOperator(FlowMethod flowMethod) throws WorkFlowException {
        FlowModel flowModel = flowMethod.getFlowModel();
        String taskId = flowMethod.getTaskId();
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        String instanceId = taskEntity.getInstanceId();
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        Map<String, NodeModel> nodes = flowModel.getNodes();
        NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
        List<OperatorEntity> operatorEntities = new ArrayList<>();
        // 生成经办，获取之前同意的经办，再次生成经办
        List<OperatorEntity> operatorList = operatorMapper.getList(taskId)
                .stream().filter(e -> ObjectUtil.equals(e.getHandleStatus(), FlowNature.AUDIT_COMPLETION)
                        && !ObjectUtil.equals(e.getStatus(), OperatorStateEnum.FUTILITY.getCode())).collect(Collectors.toList());
        List<FlowableTaskModel> taskModelList = flowAbleUrl.getCurrentTask(instanceId);

        // 原来的当前节点
        List<String> srcCurrentList = StringUtils.isNotEmpty(taskEntity.getCurrentNodeCode()) ?
                Arrays.stream(taskEntity.getCurrentNodeCode().split(",")).collect(Collectors.toList()) : new ArrayList<>();

        updateCurrentNode(taskModelList, nodes, taskEntity);

        List<NodeRecordEntity> nodeRecordList = nodeRecordMapper.getList(taskId);
        nodeRecordList = nodeRecordList.stream()
                .sorted(Comparator.comparing(NodeRecordEntity::getCreatorTime).reversed()).collect(Collectors.toList());

        for (FlowableTaskModel flowableTaskModel : taskModelList) {
            TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                    .filter(e -> ObjectUtil.equals(e.getNodeCode(), flowableTaskModel.getTaskKey())).findFirst().orElse(null);

            // 当前节点已存在，跳过
            if ((CollUtil.isNotEmpty(srcCurrentList) && srcCurrentList.contains(flowableTaskModel.getTaskKey()))
                    || (null == nodeEntity)) {
                continue;
            }
            // 处理子流程、办理节点、外部节点
            List<String> typeList = ImmutableList.of(NodeEnum.SUB_FLOW.getType(), NodeEnum.PROCESSING.getType(), NodeEnum.OUTSIDE.getType());
            if (typeList.contains(nodeEntity.getNodeType())) {

                CompleteFo completeFo = new CompleteFo();
                completeFo.setTaskId(flowableTaskModel.getTaskId());
                flowAbleUrl.complete(completeFo);
                this.handleRevokeOperator(flowMethod);
                continue;
            }

            String nodeCode = flowableTaskModel.getTaskKey();
            String nodeId = flowableTaskModel.getTaskId();
            flowMethod.setFlowableTaskId(flowableTaskModel.getTaskId());
            flowMethod.setNodes(nodes);
            flowMethod.setNodeCode(nodeCode);
            // 判断拒绝
            NodeRecordEntity nodeRecord = nodeRecordList.stream()
                    .filter(e -> ObjectUtil.equals(e.getNodeCode(), flowableTaskModel.getTaskKey())).findFirst().orElse(new NodeRecordEntity());
            if (ObjectUtil.equals(nodeRecord.getNodeStatus(), NodeStateEnum.REJECT.getCode())) {
                // 系统通过
                FlowMethod method = this.getFlowMethod(flowMethod);
                this.autoAudit(method);
                continue;
            }
            List<OperatorEntity> list = operatorList.stream()
                    .filter(e -> ObjectUtil.equals(e.getNodeCode(), nodeCode)).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(list)) {
                if ((list.size() == 1 && ObjectUtil.equals(list.get(0).getHandleId(), FlowNature.SYSTEM_CODE))) {
                    // 系统通过
                    FlowMethod method = this.getFlowMethod(flowMethod);
                    this.autoAudit(method);
                    continue;
                }
                List<String> userIdList = list.stream().map(OperatorEntity::getHandleId).distinct().collect(Collectors.toList());
                for (String userId : userIdList) {
                    OperatorEntity operator = list.stream().filter(e -> StringUtils.equals(e.getHandleId(), userId)).findFirst().orElse(null);
                    if (operator != null) {
                        OperatorEntity operatorEntity = this.createOperator(operator, OperatorStateEnum.REVOKE.getCode(), operator.getHandleId(), global);
                        operatorEntity.setTaskId(taskEntity.getId());
                        operatorEntity.setNodeId(nodeId);
                        operatorEntity.setParentId(FlowNature.PARENT_ID);
                        operatorEntities.add(operatorEntity);
                    }
                }
            }
        }
        if (CollUtil.isNotEmpty(operatorEntities)) {
            operatorMapper.insert(operatorEntities);
            addOperatorList(operatorEntities, flowModel);
            // 消息
            FlowMsgModel flowMsgModel = new FlowMsgModel();
            flowMsgModel.setNodeList(nodeEntityList);
            flowMsgModel.setTaskEntity(taskEntity);
            flowMsgModel.setUserInfo(flowModel.getUserInfo());
            flowMsgModel.setOperatorList(operatorEntities);
            flowMsgModel.setFormData(FlowContextHolder.getAllData());
            msgUtil.message(flowMsgModel);
        }
    }

    // 校验经办
    public OperatorEntity checkOperator(String id) throws WorkFlowException {
        OperatorEntity operator = operatorMapper.getInfo(id);
        if (null == operator) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        if (null == operator.getSignTime()) {
            throw new WorkFlowException(MsgCode.WF087.get());
        }
        if (null == operator.getStartHandleTime()) {
            throw new WorkFlowException(MsgCode.WF088.get());
        }
        if (null != operator.getHandleStatus()) {
            throw new WorkFlowException(MsgCode.WF031.get());
        }
        return operator;
    }


    //验证流程权限
    public void checkOperatorPermission(String id) throws WorkFlowException {
        OperatorEntity operator = operatorMapper.getInfo(id);
        if (Objects.equals(operator.getHandleId(), UserProvider.getLoginUserId())) {
            return;
        }
        List<DelegateEntity> delegateList = flowUtil.getByToUserId(UserProvider.getLoginUserId());
        List<String> handleId = new ArrayList<>();
        Map<String, String[]> delegateListAll = new HashMap<>();
        for (DelegateEntity delegate : delegateList) {
            if (StringUtils.isNotEmpty(delegate.getFlowId())) {
                String[] flowIds = delegate.getFlowId().split(",");
                delegateListAll.put(delegate.getUserId(), flowIds);
            } else {
                handleId.add(delegate.getUserId());
            }
        }
        if (handleId.contains(operator.getHandleId())) {
            return;
        }
        boolean isCheckOperator = false;
        TaskEntity task = taskMapper.getInfo(operator.getTaskId());
        for (Map.Entry<String, String[]> stringEntry : delegateListAll.entrySet()) {
            String userId = stringEntry.getKey();
            List<String> templateId = Arrays.asList(delegateListAll.get(userId));
            if (Objects.equals(userId, operator.getHandleId()) && templateId.contains(task.getTemplateId())) {
                isCheckOperator = true;
                break;
            }
        }
        if (!isCheckOperator) {
            throw new WorkFlowException(MsgCode.AD104.get());
        }
    }

    // 创建经办
    public List<OperatorEntity> createOperator(FlowMethod flowMethod) {
        List<String> userIds = flowMethod.getUserIds();
        NodeModel nodeModel = flowMethod.getNodeModel();
        FlowModel flowModel = flowMethod.getFlowModel();
        List<OperatorEntity> entityList = new ArrayList<>();
        if (userIds.isEmpty()) {
            return entityList;
        }
        TaskEntity taskEntity = flowModel.getTaskEntity();
        String flowableTaskId = flowModel.getFlowableTaskId();
        NodeModel global = flowModel.getNodes().get(NodeEnum.GLOBAL.getType());

        // 是否签收
        Boolean signFor = global.getHasSignFor();
        flowMethod.setTaskEntity(taskEntity);
        flowMethod.setSignFor(signFor);
        flowMethod.setFlowableTaskId(flowableTaskId);

        if (nodeModel.getCounterSign().equals(FlowNature.IMPROPER_APPROVER)) {
            List<String> list = improperSort(userIds, nodeModel);
            if (CollUtil.isNotEmpty(list)) {
                OperatorEntity entity = this.createOperatorEntity(flowMethod);
                entity.setHandleId(list.get(0));
                entity.setHandleAll(String.join(",", list));
                entity.setParentId(FlowNature.PARENT_ID);
                entityList.add(entity);
            }
        } else {
            // 或签、会签
            for (String userId : userIds) {
                OperatorEntity entity = this.createOperatorEntity(flowMethod);
                entity.setHandleId(userId);
                entity.setParentId(FlowNature.PARENT_ID);
                entityList.add(entity);
            }
        }

        if (CollUtil.isNotEmpty(entityList)) {
            operatorMapper.insert(entityList);
        }
        return entityList;
    }

    public OperatorEntity createOperatorEntity(FlowMethod flowMethod) {
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        TemplateNodeEntity nodeEntity = flowMethod.getNodeEntity();
        NodeModel nodeModel = flowMethod.getNodeModel();
        Boolean signFor = flowMethod.getSignFor();
        String flowableTaskId = flowMethod.getFlowableTaskId();
        OperatorEntity entity = new OperatorEntity();
        entity.setId(RandomUtil.uuId());
        entity.setIsProcessing(ObjectUtil.equals(nodeEntity.getNodeType(), NodeEnum.PROCESSING.getType()) ? FlowNature.PROCESSING : FlowNature.NOT_PROCESSING);
        entity.setNodeId(flowableTaskId);
        entity.setNodeName(nodeModel.getNodeName());
        entity.setNodeCode(nodeEntity.getNodeCode());
        entity.setStatus(OperatorStateEnum.RUNING.getCode());
        Boolean flowTodo = serviceUtil.getFlowTodo();
        if (Boolean.TRUE.equals(flowTodo)) {
            entity.setSignTime(new Date());
            entity.setStartHandleTime(new Date());
        } else {
            Boolean flowSign = serviceUtil.getFlowSign();
            if (Boolean.TRUE.equals(flowSign)) {
                entity.setSignTime(new Date());
            } else {
                if (Boolean.FALSE.equals(signFor)) {
                    entity.setSignTime(new Date());
                }
            }
        }
        entity.setCompletion(FlowNature.NORMAL);
        entity.setTaskId(taskEntity.getId());
        entity.setEngineType(taskEntity.getEngineType());
        return entity;
    }

    // 判断节点的审批结果
    public boolean checkAudit(FlowMethod flowMethod) throws WorkFlowException {
        OperatorEntity operator = flowMethod.getOperatorEntity();
        NodeModel nodeModel = flowMethod.getNodeModel();
        NodeModel global = flowMethod.getNodes().get(NodeEnum.GLOBAL.getType());
        Integer handleStatus = flowMethod.getHandleStatus();
        boolean result = false;

        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId())
                .eq(OperatorEntity::getNodeCode, operator.getNodeCode())
                .eq(OperatorEntity::getParentId, FlowNature.PARENT_ID)
                .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode());
        List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);

        Boolean auditFlag = flowMethod.getAuditFlag();
        if (Boolean.TRUE.equals(auditFlag)) {
            for (OperatorEntity entity : list) {
                if (ObjectUtil.equals(entity.getId(), operator.getId())) {
                    entity.setHandleStatus(operator.getHandleStatus());
                    break;
                }
            }
        }
        List<LaunchUserEntity> stepUserList = launchUserMapper.getTaskList(operator.getTaskId());
        LaunchUserEntity launchUser = stepUserList.stream().filter(e -> e.getNodeCode().equals(operator.getNodeCode())).findFirst().orElse(null);

        boolean isStep = Objects.equals(OperatorEnum.STEP.getCode(), nodeModel.getAssigneeType());
        Integer counterSign = isStep ? FlowNature.FIXED_APPROVER : nodeModel.getCounterSign();
        if (FlowNature.FIXED_APPROVER.equals(counterSign)) {
            // 或签，一人同意或拒绝
            if (Boolean.TRUE.equals(auditFlag)) {
                result = checkStep(list, nodeModel, launchUser);
            } else {
                result = operator.getHandleStatus().equals(handleStatus);
            }
        } else if (FlowNature.FIXED_JOINTLY_APPROVER.equals(counterSign)) {
            // 会签
            long numAudit;
            long numReject;
            if (Boolean.TRUE.equals(auditFlag)) {
                numAudit = list.stream().filter(e -> FlowNature.AUDIT_COMPLETION.equals(e.getHandleStatus())).count();
                numReject = list.stream().filter(e -> FlowNature.REJECT_COMPLETION.equals(e.getHandleStatus())).count();
            } else {
                numAudit = list.stream()
                        .filter(e -> FlowNature.AUDIT_COMPLETION.equals(e.getHandleStatus()) && null != e.getHandleTime()).count();
                numReject = list.stream()
                        .filter(e -> FlowNature.REJECT_COMPLETION.equals(e.getHandleStatus()) && null != e.getHandleTime()).count();
            }
            CounterSignConfig config = nodeModel.getCounterSignConfig();

            // 判断计算方式，延后计算（在所有审批人审批完成后进行规则判断）
            int type = config.getCalculateType();
            if (ObjectUtil.equals(type, FlowNature.DELAY)) {
                int auditCount = (int) list.stream().filter(e -> null != e.getHandleStatus()).count();
                if (list.size() != auditCount) {
                    return false;
                }
            }

            int auditNum = config.getAuditNum();
            int auditRatio = config.getAuditRatio();
            int rejectNum = config.getRejectNum();
            int rejectRatio = config.getRejectRatio();

            if (config.getAuditType().equals(FlowNature.PERCENT)) {
                // 百分比
                int res = (int) (numAudit * 100 / list.size());
                result = res >= auditRatio;
            } else if (config.getAuditType().equals(FlowNature.NUMBER)) {
                // 人数
                result = numAudit >= auditNum;
            }
            // 同意比例没通过，再计算拒绝的比例
            if (!result) {
                if (config.getRejectType().equals(FlowNature.PERCENT)) {
                    // 百分比
                    int res = (int) (numReject * 100 / list.size());
                    result = res >= rejectRatio;
                } else if (config.getRejectType().equals(FlowNature.NUMBER)) {
                    // 人数
                    result = numReject >= rejectNum;
                } else {
                    // 同意取反
                    if (numReject > 0) {
                        if (config.getAuditType().equals(FlowNature.PERCENT)) {
                            int res = (int) (numReject * 100 / list.size());
                            result = res >= (100 - auditRatio);
                        } else if (config.getAuditType().equals(FlowNature.NUMBER)) {
                            result = numReject >= (list.size() - auditNum);
                        }
                    }
                }
                if (result && ObjectUtil.equals(type, FlowNature.DELAY)) {
                    flowMethod.setHandleStatus(FlowNature.REJECT_COMPLETION);
                }
            } else {
                if (ObjectUtil.equals(type, FlowNature.DELAY)) {
                    flowMethod.setHandleStatus(FlowNature.AUDIT_COMPLETION);
                }
            }
        } else if (FlowNature.IMPROPER_APPROVER.equals(counterSign)) {
            // 依次审批
            if (ObjectUtil.equals(handleStatus, FlowNature.REJECT_COMPLETION)) {
                // 拒绝直接返回true，根据拒绝是否继续流转，直接结束 或 流转一下节点
                return true;
            }
            List<String> allList = StringUtils.isNotEmpty(operator.getHandleAll()) ? Arrays.stream(operator.getHandleAll().split(",")).collect(Collectors.toList()) : new ArrayList<>();

            String userId = operator.getHandleId();
            // 转审的经办，通过记录获取原来的审批人
            if (ObjectUtil.equals(operator.getStatus(), OperatorStateEnum.TRANSFER.getCode())) {
                RecordEntity transferRecord = recordMapper.getTransferRecord(operator.getId());
                String handleId = transferRecord.getHandleId();
                if (StringUtils.isNotBlank(handleId)) {
                    userId = handleId;
                }
            }
            int index = allList.indexOf(userId);
            // 最后一人直接返回
            if (index == allList.size() - 1) {
                return true;
            }
            if (Boolean.TRUE.equals(auditFlag)) {
                return false;
            }
            String handleId = "";
            if (index != -1) {
                handleId = allList.get(index + 1);
            }
            OperatorEntity entity = this.createOperator(operator, OperatorStateEnum.RUNING.getCode(), handleId, global);
            operatorMapper.insert(entity);

            this.improperApproverMessage(flowMethod, entity);
        }
        if (Boolean.FALSE.equals(auditFlag)) {
            flowMethod.setTaskId(operator.getTaskId());
            flowMethod.setNodeCode(operator.getNodeCode());
            if (result) {
                // 结束该节点的其他经办
                endOperator(flowMethod);
            } else {
                //当前审批完，结束协办数据
                endAssist(flowMethod);
            }
        }
        return result;
    }

    //判断逐级
    public boolean checkStep(List<OperatorEntity> list, NodeModel nodeModel, LaunchUserEntity launchUser) {
        if (null != launchUser) {
            String positionId = launchUser.getPositionId();
            List<String> positionList = Arrays.asList(positionId.split(","));
            String organizeId = launchUser.getOrganizeId();
            List<String> organizeList = Arrays.asList(organizeId.split(","));
            //判断结束层级
            ApproversConfig approversConfig = nodeModel.getApproversConfig();
            Integer end = approversConfig.getEnd();
            boolean isOrganize = Objects.equals(end, FlowNature.ORGANIZATION);
            int endLevel = isOrganize ? approversConfig.getOriginLevel() : approversConfig.getLevel();
            int totalLevel = positionList.size() + organizeList.size() - 1;
            int level = totalLevel - list.size();
            //判断逐级是否结束
            int i = positionList.size() == 1 ? 1 : 0;
            boolean result = isOrganize ? level > endLevel : list.size() < Math.min(totalLevel - (i), endLevel);
            return !result;
        }
        return true;
    }

    // 依次审批消息
    public void improperApproverMessage(FlowMethod flowMethod, OperatorEntity entity) throws WorkFlowException {
        FlowModel flowModel = flowMethod.getFlowModel();
        List<CirculateEntity> circulateList = flowMethod.getCirculateList();
        TemplateNodeEntity nodeEntity = flowMethod.getNodeEntity();
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        List<TemplateNodeEntity> nodeEntityList = flowMethod.getNodeEntityList();

        List<OperatorEntity> entityList = new ArrayList<>();
        entityList.add(entity);
        // 消息
        FlowMsgModel flowMsgModel = new FlowMsgModel();
        flowMsgModel.setNodeList(nodeEntityList);
        flowMsgModel.setCirculateList(circulateList);
        flowMsgModel.setUserInfo(flowModel.getUserInfo());
        flowMsgModel.setTaskEntity(taskEntity);
        flowMsgModel.setNodeCode(nodeEntity.getNodeCode());
        flowMsgModel.setOperatorList(entityList);
        flowMsgModel.setCopy(true);
        flowMsgModel.setApprove(true);
        flowMsgModel.setFormData(FlowContextHolder.getAllData());
        msgUtil.message(flowMsgModel);
    }

    // 结束协办
    public void endAssist(FlowMethod flowMethod) {
        OperatorEntity operator = flowMethod.getOperatorEntity();
        String taskId = flowMethod.getTaskId();
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskId).eq(OperatorEntity::getParentId, operator.getId())
                .eq(OperatorEntity::getStatus, OperatorStateEnum.ASSIST.getCode());
        List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            for (OperatorEntity entity : list) {
                entity.setCompletion(FlowNature.ACTION);
            }
            operatorMapper.updateById(list);
        }
    }

    // 结束经办
    public void endOperator(FlowMethod flowMethod) {
        String taskId = flowMethod.getTaskId();
        String nodeCode = flowMethod.getNodeCode();
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskId)
                .isNull(OperatorEntity::getHandleStatus);
        if (StringUtils.isNotEmpty(nodeCode)) {
            queryWrapper.lambda().eq(OperatorEntity::getNodeCode, nodeCode);
        }
        List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            for (OperatorEntity entity : list) {
                entity.setCompletion(FlowNature.ACTION);
            }
            operatorMapper.updateById(list);
        }
    }

    // 结束加签经办
    public void endAddSign(String parentId) {
        List<OperatorEntity> list = new ArrayList<>();
        this.getAddSignChildren(parentId, list);
        if (CollUtil.isNotEmpty(list)) {
            operatorMapper.updateById(list);
        }
    }

    // 撤回复原
    public void recallRestore(OperatorEntity operator, String nodeId, List<String> userIds) {
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId())
                .eq(OperatorEntity::getNodeCode, operator.getNodeCode()).eq(OperatorEntity::getNodeId, nodeId)
                .eq(OperatorEntity::getCompletion, FlowNature.ACTION).isNull(OperatorEntity::getHandleTime);
        List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
        if (CollUtil.isEmpty(list)) {
            return;
        }

        List<OperatorEntity> updateList = new ArrayList<>();
        for (OperatorEntity operatorEntity : list) {
            if (ObjectUtil.equals(operatorEntity.getParentId(), operator.getParentId())) {
                updateList.add(operatorEntity);
                if (null != userIds) {
                    userIds.add(operatorEntity.getHandleId());
                }
            }
        }

        if (CollUtil.isNotEmpty(updateList)) {
            Date date = new Date();
            UpdateWrapper<OperatorEntity> wrapper = new UpdateWrapper<>();
            List<String> ids = updateList.stream().map(OperatorEntity::getId).collect(Collectors.toList());
            wrapper.lambda().in(OperatorEntity::getId, ids)
                    .set(OperatorEntity::getCreatorTime, date)
                    .set(OperatorEntity::getStatus, OperatorStateEnum.RUNING.getCode())
                    .set(OperatorEntity::getCompletion, FlowNature.NORMAL)
                    .set(OperatorEntity::getHandleStatus, null);
            operatorMapper.update(wrapper);
            // 作废记录
            recordMapper.invalid(updateList);
        }

        List<OperatorEntity> deleteList = new ArrayList<>();
        if (CollUtil.isNotEmpty(updateList)) {
            for (OperatorEntity op : updateList) {
                this.getAddSignChildren(op.getId(), deleteList);
            }
            if (CollUtil.isNotEmpty(deleteList)) {
                deleteList.forEach(e -> e.setStatus(OperatorStateEnum.FUTILITY.getCode()));
                operatorMapper.updateById(deleteList);
                // 作废记录
                recordMapper.invalid(deleteList);
            }
        }
    }

    public List<OperatorEntity> getAddSignChildren(String operatorId, List<OperatorEntity> list) {
        list = null == list ? new ArrayList<>() : list;
        List<OperatorEntity> childList = this.getChildList(operatorId);
        if (CollUtil.isNotEmpty(childList)) {
            for (OperatorEntity operator : childList) {
                operator.setCompletion(FlowNature.ACTION);
                list.add(operator);
                this.getAddSignChildren(operator.getId(), list);
            }
        }
        return list;
    }

    // 判断自动审批
    public int checkAuto(FlowMethod flowMethod) {
        NodeModel nodeModel = flowMethod.getNodeModel();
        if (Boolean.TRUE.equals(nodeModel.getHasAutoApprover())) {
            AutoAuditRule autoAuditRule = nodeModel.getAutoAuditRule();
            if (null != autoAuditRule) {
                List<ProperCond> conditions = autoAuditRule.getConditions();
                flowMethod.setConditions(conditions);
                flowMethod.setMatchLogic(autoAuditRule.getMatchLogic());
                if (conditionUtil.hasCondition(flowMethod)) {
                    return 1;
                }
            }
            AutoAuditRule autoRejectRule = nodeModel.getAutoRejectRule();
            if (null != autoRejectRule) {
                List<ProperCond> conditions = autoRejectRule.getConditions();
                flowMethod.setConditions(conditions);
                flowMethod.setMatchLogic(autoRejectRule.getMatchLogic());
                if (conditionUtil.hasCondition(flowMethod)) {
                    return 2;
                }
            }
        }
        return 3;
    }

    // 节点的自动审批
    public void autoAudit(FlowMethod flowMethod) throws WorkFlowException {
        FlowModel flowModel = flowMethod.getFlowModel();

        OperatorEntity entity = this.saveSystemOperator(flowMethod);
        if (null == entity) {
            return;
        }
        audit(entity, flowModel);
    }

    public void autoAudit(OperatorEntity entity, FlowModel flowModel) throws WorkFlowException {
        if (null == entity || null == flowModel) {
            return;
        }
        audit(entity, flowModel);
    }

    public void audit(OperatorEntity operator, FlowModel flowModel) throws WorkFlowException {
        TaskEntity taskEntity = flowModel.getTaskEntity();
        //判断是否审批过
        if (null != operator.getHandleStatus()) {
            return;
        }
        flowUtil.isSuspend(taskEntity);
        flowUtil.isCancel(taskEntity);
        flowUtil.isTrigger(taskEntity, ImmutableList.of(operator.getNodeCode()));

        if (null == flowModel.getUserInfo()) {
            flowModel.setUserInfo(UserProvider.getUser());
        }
        RevokeEntity revokeEntity = revokeMapper.getRevokeTask(taskEntity.getId());
        if (null != revokeEntity) {
            // 处理撤销的经办
            addTask(ImmutableList.of(revokeEntity.getTaskId()));
            handleRevoke(flowModel, operator, revokeEntity);
            return;
        }

        String nodeCode = operator.getNodeCode();
        addTask(ImmutableList.of(taskEntity.getId()));

        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                .filter(e -> StringUtils.equals(e.getNodeCode(), nodeCode)).findFirst().orElse(null);
        if (nodeEntity == null) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }

        TemplateNodeEntity startEntity = nodeEntityList.stream()
                .filter(e -> StringUtils.equals(e.getNodeType(), NodeEnum.START.getType())).findFirst().orElse(null);
        if (startEntity == null) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }

        flowModel.setNodeEntity(nodeEntity);
        String deploymentId = flowModel.getDeploymentId();
        Map<String, NodeModel> nodes = flowModel.getNodes();
        NodeModel nodeModel = nodes.get(operator.getNodeCode());
        // 全局节点
        TemplateNodeEntity globalEntity = nodeEntityList.stream()
                .filter(e -> StringUtils.equals(NodeEnum.GLOBAL.getType(), e.getNodeType())).findFirst().orElse(new TemplateNodeEntity());
        NodeModel global = JsonUtil.getJsonToBean(globalEntity.getNodeJson(), NodeModel.class);
        // 表单
        Map<String, Object> formData = flowModel.getFormData();

        if (CollUtil.isNotEmpty(flowModel.getCandidateList()) && ObjectUtil.equals(nodeModel.getCounterSign(), FlowNature.FIXED_APPROVER)) {
            NextOrPrevFo nextOrPrevFo = new NextOrPrevFo();
            nextOrPrevFo.setDeploymentId(flowModel.getDeploymentId());
            nextOrPrevFo.setTaskKey(operator.getNodeCode());
            List<FlowableNodeModel> nextModels = flowAbleUrl.getNext(nextOrPrevFo);
            List<String> nextCodes = nextModels.stream().map(FlowableNodeModel::getId).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(nextCodes)) {
                candidatesMapper.deleteByCodes(operator.getTaskId(), nextCodes);
            }
        }

        // 保存候选人、异常人
        flowUtil.create(flowModel, operator.getTaskId(), nodeEntityList, operator);

        // 流程参数
        updateGlobalParam(taskEntity, nodeModel, global, flowModel.getFormData());
        flowModel.setTaskEntity(taskEntity);

        FlowMethod flowMethod = new FlowMethod();
        Integer handleStatus = flowModel.getHandleStatus();
        boolean isAudit = handleStatus.equals(FlowNature.AUDIT_COMPLETION);
        if (isAudit) {
            operator.setHandleStatus(FlowNature.AUDIT_COMPLETION);
            flowMethod.setType(RecordEnum.AUDIT.getCode());
            flowModel.setEventStatus(EventEnum.APPROVE.getStatus());
        } else {
            operator.setHandleStatus(FlowNature.REJECT_COMPLETION);
            flowMethod.setType(RecordEnum.REJECT.getCode());
            flowModel.setEventStatus(EventEnum.REJECT.getStatus());
        }

        // 同意、拒绝
        operator.setHandleTime(new Date());
        operator.setCompletion(FlowNature.ACTION);
        operatorMapper.updateById(operator);
        if (StringUtils.isNotBlank(operator.getDraftData())) {
            // 清除草稿数据
            UpdateWrapper<OperatorEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.lambda().eq(OperatorEntity::getId, operator.getId())
                    .set(OperatorEntity::getDraftData, null);
            operatorMapper.update(updateWrapper);
        }
        // 记录
        flowMethod.setFlowModel(flowModel);
        flowMethod.setOperatorEntity(operator);
        recordMapper.createRecord(flowMethod);

        flowMethod.setTaskEntity(taskEntity);
        flowMethod.setNodeEntity(nodeEntity);
        flowMethod.setNodeEntityList(nodeEntityList);
        flowMethod.setDeploymentId(deploymentId);
        flowMethod.setFormData(formData);
        flowMethod.setNodes(nodes);
        flowMethod.setNodeCode(operator.getNodeCode());
        flowMethod.setNodeModel(nodeModel);
        flowMethod.setHandleStatus(handleStatus);
        // 抄送
        List<CirculateEntity> circulateList = new ArrayList<>();
        if (Boolean.TRUE.equals(flowModel.getCopyMsgFlag())) {
            circulateList = circulateList(flowMethod);
        }
        flowMethod.setCirculateList(circulateList);

        //消息数据
        FlowMsgModel flowMsgModel = new FlowMsgModel();
        flowMsgModel.setNodeList(nodeEntityList);
        flowMsgModel.setCirculateList(circulateList);
        flowMsgModel.setUserInfo(flowModel.getUserInfo());
        flowMsgModel.setTaskEntity(taskEntity);
        flowMsgModel.setNodeCode(nodeEntity.getNodeCode());
        flowMsgModel.setCopy(true);
        flowMsgModel.setFormData(FlowContextHolder.getAllData());

        // 判断加签比例
        if (!StringUtils.equals(operator.getParentId(), FlowNature.PARENT_ID)) {
            handleAddCounterSign(operator, flowModel);
            msgUtil.message(flowMsgModel);
            return;
        }
        // 选择分支
        candidatesMapper.createBranch(flowModel.getBranchList(), operator);

        // 指派的经办无需计算比例，或系统审批
        if (!ObjectUtil.equals(operator.getStatus(), OperatorStateEnum.ASSIGNED.getCode())
                || ObjectUtil.equals(operator.getHandleId(), FlowNature.SYSTEM_CODE)) {
            // 判断比例
            boolean auditRes = checkAudit(flowMethod);
            if (!auditRes) {
                msgUtil.message(flowMsgModel);
                return;
            }
            //逐级审批过
            List<LaunchUserEntity> launchUserList = launchUserMapper.getTaskList(operator.getTaskId());
            LaunchUserEntity launchUser = launchUserList.stream().filter(e -> Objects.equals(e.getNodeCode(), nodeCode)).findFirst().orElse(null);
            if ((launchUser != null) && (isAudit || Boolean.TRUE.equals(global.getHasContinueAfterReject()))) {
                QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
                queryWrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId())
                        .eq(OperatorEntity::getNodeCode, operator.getNodeCode())
                        .eq(OperatorEntity::getParentId, FlowNature.PARENT_ID)
                        .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode());
                List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
                boolean result = checkStep(list, nodeModel, launchUser);
                if (!result) {
                    String positionId = launchUser.getPositionId() != null ? launchUser.getPositionId() : "";
                    List<String> positionList = Arrays.asList(positionId.split(","));
                    String organizeId = launchUser.getOrganizeId() != null ? launchUser.getOrganizeId() : "";
                    List<String> organizeList = Arrays.asList(organizeId.split(","));
                    //获取逐级下个审批人
                    FlowMethod jsonToBean = new FlowMethod();
                    flowModel.setFlowableTaskId(operator.getNodeId());
                    jsonToBean.setTaskEntity(flowMethod.getTaskEntity());
                    jsonToBean.setFlowModel(flowModel);
                    jsonToBean.setNodeEntityList(flowMethod.getNodeEntityList());
                    jsonToBean.setNodeEntity(flowMethod.getNodeEntity());
                    jsonToBean.setSubFormData(flowMethod.getSubFormData());
                    jsonToBean.setExtraRule(true);
                    jsonToBean.setNodeModel(nodeModel);
                    jsonToBean.setOperatorEntity(operator);
                    UserEntity userEntity = new UserEntity();
                    userEntity.setPositionId(positionList.get(positionList.size() - 1));
                    userEntity.setOrganizeId(organizeList.get(organizeList.size() - 1));
                    String managerByLevel = flowUtil.getManagerByLevel(userEntity, list.size() + 1);
                    UserEntity userInfo = serviceUtil.getUserInfo(managerByLevel);
                    if (userInfo == null) {
                        throw new WorkFlowException(MsgCode.WF153.get());
                    }
                    jsonToBean.setUserIds(ImmutableList.of(managerByLevel));
                    List<OperatorEntity> entityList = createOperator(jsonToBean);
                    for (OperatorEntity entity : entityList) {
                        entity.setHandleAll(operator.getId());
                    }
                    operatorMapper.updateById(entityList);
                    addOperatorList(entityList, flowModel);

                    // 消息
                    flowMsgModel.setOperatorList(entityList);
                    if (isAudit) {
                        flowMsgModel.setApprove(true);
                    } else {
                        flowMsgModel.setReject(true);
                    }
                    msgUtil.message(flowMsgModel);
                    return;
                }
            }

        }
        //审批事件
        addEvent(flowModel);
        Integer tempStatus = flowMethod.getHandleStatus() != null ? flowMethod.getHandleStatus() : handleStatus;
        // 判断拒绝后是否继续流转
        if (FlowNature.REJECT_COMPLETION.equals(tempStatus) && Boolean.TRUE.equals(!global.getHasContinueAfterReject())) {
            flowMethod.setOperatorEntity(operator);
            flowMethod.setFlowModel(flowModel);
            handleEndTask(flowMethod);
            launchUserMapper.deleteStepUser(taskEntity.getId());
            return;
        }

        //自由节点，不是结束，变更流程
        if (Objects.equals(taskEntity.getFlowType(), FlowNature.FREE)) {
            FreeModel freeParameter = flowModel.getFreeFlowConfig() != null ? flowModel.getFreeFlowConfig() : new FreeModel();
            if (Boolean.TRUE.equals(!freeParameter.getIsEnd()) && !freeParameter.getApprovers().isEmpty()) {
                freeParameter.setIsEnd(null);
                TemplateJsonEntity templateJson = templateJsonMapper.getInfo(taskEntity.getFlowId());
                String flowXml = templateJson.getFlowXml();
                String freeCode = taskEntity.getFreeCode();
                if (StringUtils.isNotEmpty(flowXml)) {
                    try {
                        flowXml = URLDecoder.decode(flowXml, StringPool.UTF_8);
                    } catch (Exception e) {
                        log.info(e.getMessage());
                    }
                    XmlModel xmlModel = FlowXmlUtil.model(flowXml, taskEntity.getCurrentNodeCode(), false);
                    String newLineCode = xmlModel.getNewLineCode();
                    String newNodeCode = xmlModel.getNewNodeCode();
                    Map<String, NodeModel> nodeMode = new HashMap<>();
                    Map<String, Map<String, Object>> flowNodes = new HashMap<>();
                    String freeNodeName = "自由节点";
                    for (TemplateNodeEntity templateNode : nodeEntityList) {
                        Map<String, Object> map = JsonUtil.stringToMap(templateNode.getNodeJson());
                        flowNodes.put(templateNode.getNodeCode(), map);
                        if (Objects.equals(nodeEntity.getNodeType(), NodeEnum.APPROVER.getType())) {
                            NodeModel model = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
                            freeNodeName = model.getNodeName().replaceAll(nodeModel.getNodeId(), "");
                        }
                        nodeMode.put(templateNode.getNodeCode(), JsonUtil.getJsonToBean(map, NodeModel.class));
                    }

                    Map<String, Object> freeMap = flowNodes.get(taskEntity.getCurrentNodeCode());
                    if (CollUtil.isNotEmpty(freeMap)) {
                        freeParameter.setNodeId(newNodeCode);
                        freeParameter.setNodeName(freeNodeName + newNodeCode);
                        if (Objects.equals(freeParameter.getCounterSign(), FlowNature.IMPROPER_APPROVER)) {
                            freeParameter.setApproversSortList(freeParameter.getApprovers());
                        }
                        Map<String, Object> parameter = JsonUtil.entityToMap(freeParameter);
                        Map<String, Object> nodeModelMap = new HashMap<>(freeMap);
                        nodeModelMap.putAll(parameter);
                        flowNodes.put(newNodeCode, nodeModelMap);
                        nodeMode.put(newNodeCode, JsonUtil.getJsonToBean(nodeModelMap, NodeModel.class));
                    }
                    List<String> connectList = new ArrayList<>(global.getConnectList());
                    connectList.add(newLineCode);
                    FreeConnect freeConnect = new FreeConnect();
                    freeConnect.setConnectList(connectList);
                    Map<String, Object> connectMap = JsonUtil.entityToMap(freeConnect);
                    Map<String, Object> globalMap = flowNodes.get(NodeEnum.GLOBAL.getType());
                    globalMap.putAll(connectMap);
                    templateNodeMapper.deleteByIds(nodeEntityList);
                    flowUtil.craeteNodeModel(flowNodes, taskEntity.getFlowId());

                    //-----------------------------操作flowable数据库开始--------------------------------------
                    //更新版本部署id
                    String xml = FlowXmlUtil.xml(xmlModel);
                    String flowableId = flowAbleUrl.deployFlowAble(xml, RandomUtil.uuId());

                    // 启动流程示例
                    FlowMethod method = new FlowMethod();
                    method.setDeploymentId(flowableId);
                    method.setFormData(flowMethod.getFormData());
                    method.setNodeCode(startEntity.getNodeCode());
                    method.setNodes(nodeMode);
                    method.setTaskEntity(taskEntity);
                    Map<String, Boolean> resMap = conditionUtil.handleCondition(method); //// 判断条件
                    // 判断条件、候选人
                    try {
                        conditionUtil.checkCondition(resMap, nodeMode);
                    } catch (WorkFlowException e) {
                        throw new WorkFlowException(MsgCode.WF133.get());
                    }
                    String instanceId = flowAbleUrl.startInstance(flowableId, new HashMap<>(resMap));

                    //更改实例
                    UpdateWrapper<TaskEntity> taskWrapper = new UpdateWrapper<>();
                    taskWrapper.lambda().eq(TaskEntity::getId, taskEntity.getId());
                    taskWrapper.lambda().set(TaskEntity::getInstanceId, instanceId);
                    taskMapper.update(taskWrapper);

                    //指向待办节点
                    List<String> sourceTaskCode = new ArrayList<>();
                    if (StringUtils.isNotEmpty(freeCode)) {
                        sourceTaskCode.add(freeCode);
                    } else {
                        List<FlowableTaskModel> currentTask = flowAbleUrl.getCurrentTask(instanceId);
                        sourceTaskCode.addAll(currentTask.stream().map(FlowableTaskModel::getTaskKey).collect(Collectors.toList()));
                    }
                    JumpFo jumpFo = new JumpFo();
                    jumpFo.setInstanceId(instanceId);
                    jumpFo.setSource(sourceTaskCode);
                    jumpFo.setTarget(Arrays.asList(taskEntity.getCurrentNodeCode().split(",")));
                    flowAbleUrl.jump(jumpFo);

                    List<FlowableTaskModel> approverTask = flowAbleUrl.getCurrentTask(instanceId);

                    //更新数据
                    for (FlowableTaskModel taskModel : approverTask) {
                        if (Objects.equals(taskModel.getTaskKey(), operator.getNodeCode())) {
                            //更改代办
                            UpdateWrapper<OperatorEntity> operatorWrapper = new UpdateWrapper<>();
                            operatorWrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId());
                            operatorWrapper.lambda().eq(OperatorEntity::getNodeId, operator.getNodeId());
                            operatorWrapper.lambda().set(OperatorEntity::getNodeId, taskModel.getTaskId());
                            operatorMapper.update(operatorWrapper);
                            //记录
                            UpdateWrapper<RecordEntity> recordWrapper = new UpdateWrapper<>();
                            recordWrapper.lambda().eq(RecordEntity::getTaskId, operator.getTaskId());
                            recordWrapper.lambda().eq(RecordEntity::getNodeId, operator.getNodeId());
                            recordWrapper.lambda().set(RecordEntity::getNodeId, taskModel.getTaskId());
                            recordMapper.update(recordWrapper);
                            //抄送
                            UpdateWrapper<CirculateEntity> circulateWrapper = new UpdateWrapper<>();
                            circulateWrapper.lambda().eq(CirculateEntity::getTaskId, operator.getTaskId());
                            circulateWrapper.lambda().eq(CirculateEntity::getNodeId, operator.getNodeId());
                            circulateWrapper.lambda().set(CirculateEntity::getNodeId, taskModel.getTaskId());
                            circulateMapper.update(circulateWrapper);

                            //更新nodeId
                            operator.setNodeId(taskModel.getTaskId());
                        }
                    }

                    //更改xml和部署id
                    UpdateWrapper<TemplateJsonEntity> jsonWrapper = new UpdateWrapper<>();
                    jsonWrapper.lambda().eq(TemplateJsonEntity::getId, taskEntity.getFlowId());
                    jsonWrapper.lambda().set(TemplateJsonEntity::getFlowableId, flowableId);
                    jsonWrapper.lambda().set(TemplateJsonEntity::getFlowXml, xml);
                    templateJsonMapper.update(jsonWrapper);

                    //获取最新数据
                    flowUtil.setFlowModel(operator.getTaskId(), flowModel);

                    flowMethod.setDeploymentId(flowModel.getDeploymentId());
                    flowMethod.setNodes(flowModel.getNodes());
                    flowMethod.setTaskEntity(flowModel.getTaskEntity());

                }
            }
        }

        flowModel.setOperatorEntity(operator);
        boolean isRejectDataId = taskEntity.getRejectDataId() != null;
        if (isRejectDataId) {
            handleRejectData(flowModel);
        } else {
            List<String> branchList = candidatesMapper.getBranch(operator.getTaskId(), operator.getNodeCode());
            Map<String, Boolean> resMap;
            if (branchList.isEmpty()) {
                resMap = conditionUtil.handleCondition(flowMethod);
                conditionUtil.checkCondition(resMap, nodes);
            } else {
                resMap = conditionUtil.getForBranch(flowMethod, branchList);
            }
            taskLineMapper.create(taskEntity.getId(), resMap);

            // 完成
            CompleteFo fo = new CompleteFo();
            fo.setTaskId(operator.getNodeId());
            fo.setVariables(new HashMap<>(resMap));
            flowAbleUrl.complete(fo);
        }
        // 节点记录
        NodeRecordModel nodeRecordModel = new NodeRecordModel();
        nodeRecordModel.setTaskId(operator.getTaskId());
        nodeRecordModel.setNodeId(operator.getNodeId());
        nodeRecordModel.setNodeCode(operator.getNodeCode());
        nodeRecordModel.setNodeName(operator.getNodeName());
        nodeRecordModel.setNodeStatus(ObjectUtil.equals(flowMethod.getHandleStatus(), FlowNature.AUDIT_COMPLETION) ? NodeStateEnum.PASS.getCode() : NodeStateEnum.REJECT.getCode());
        nodeRecordMapper.create(nodeRecordModel);

        // 生成下一节点
        List<OperatorEntity> entityList = this.handleOperator(flowModel);
        // 删除选择分支
        candidatesMapper.deleteBranch(operator.getTaskId(), operator.getNodeCode());

        //删除逐级数据
        launchUserMapper.delete(operator.getTaskId(), ImmutableList.of(operator.getNodeCode()));

        if (!isRejectDataId) {
            boolean isProcessing = ObjectUtil.equals(nodeEntity.getNodeType(), NodeEnum.PROCESSING.getType());
            Integer i = isProcessing ? ActionEnum.PROCESSING.getCode() : ActionEnum.AUDIT.getCode();
            flowModel.setAction(ObjectUtil.equals(handleStatus, FlowNature.REJECT_COMPLETION) ? ActionEnum.REJECT.getCode() : i);
            handleTrigger(operator, flowModel);
        }

        // 判断任务是否结束
        flowModel.setHandleStatus(flowMethod.getHandleStatus() != null ? flowMethod.getHandleStatus() : handleStatus);
        isFinished(flowModel);

        // 消息
        flowMsgModel.setOperatorList(entityList);
        flowMsgModel.setNodeList(flowModel.getNodeEntityList());
        if (isAudit) {
            flowMsgModel.setApprove(true);
        } else {
            flowMsgModel.setReject(true);
        }
        msgUtil.message(flowMsgModel);

        // 系统审批
        systemAudit();
    }

    // 退回
    public void back(String id, FlowModel flowModel) throws WorkFlowException {
        Boolean triggerBack = flowModel.getTriggerBack();
        OperatorEntity operator = checkOperator(id);

        flowUtil.setFlowModel(operator.getTaskId(), flowModel);
        TaskEntity taskEntity = flowModel.getTaskEntity();
        checkTemplateHide(taskEntity.getTemplateId());
        flowUtil.isSuspend(taskEntity);
        flowUtil.isCancel(taskEntity);
        flowUtil.isTrigger(taskEntity);
        if (null != taskEntity.getRejectDataId()) {
            throw new WorkFlowException(MsgCode.WF083.get());
        }
        Map<String, NodeModel> nodes = flowModel.getNodes();
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        String deploymentId = flowModel.getDeploymentId();

        NodeModel nodeModel = nodes.get(operator.getNodeCode());
        TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                .filter(e -> ObjectUtil.equals(e.getNodeCode(), operator.getNodeCode())).findFirst().orElse(null);
        flowModel.setNodeEntity(nodeEntity);

        TemplateNodeEntity start = nodeEntityList.stream().filter(e -> e.getNodeType().equals(NodeEnum.START.getType())).findFirst().orElse(null);
        if (null == start) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }

        Integer backType = flowModel.getBackType();
        String backStep = flowModel.getBackNodeCode();
        boolean isPresentType = FlowNature.PRESENT_TYPE.equals(backType);
        List<String> stepList = new ArrayList<>();
        if (FlowNature.START.equals(backStep)) {
            stepList.add(start.getNodeCode());
        } else {
            stepList = StringUtils.isNotEmpty(backStep) ? Arrays.stream(backStep.split(",")).collect(Collectors.toList()) : new ArrayList<>();
            // 判断没有经过的节点，不能退回
            List<String> nodeCodeList = operatorMapper.getList(taskEntity.getId()).stream()
                    .filter(e -> !e.getStatus().equals(OperatorStateEnum.FUTILITY.getCode()))
                    .map(OperatorEntity::getNodeCode).distinct().collect(Collectors.toList());
            nodeCodeList.add(start.getNodeCode());
            for (String step : stepList) {
                if (!nodeCodeList.contains(step)) {
                    throw new WorkFlowException(MsgCode.WF047.get());
                }
            }
        }

        flowModel.setAction(ActionEnum.BACK.getCode());
        flowModel.setOperatorEntity(operator);
        if (null != nodeEntity) {
            Map<String, Object> map = serviceUtil.infoData(nodeEntity.getFormId(), operator.getTaskId());
            FlowContextHolder.addChildData(operator.getTaskId(), nodeEntity.getFormId(), map, nodeModel.getFormOperates(), false);
            flowModel.setFormData(map);
        }

        if (Boolean.TRUE.equals(!triggerBack) && !isPresentType) {
            handleTrigger(operator, flowModel);
        }

        //获取是否触发数据
        List<TriggerLaunchflowEntity> launchFlowList = getLaunchFlowList(taskEntity.getId());

        AfterFo afterFo = new AfterFo();
        afterFo.setDeploymentId(deploymentId);
        afterFo.setTaskKeys(stepList);
        // 获取目标节点之后的所有节点，将这些节点的经办全部保存、更新状态
        List<String> afterListAll = flowAbleUrl.getAfter(afterFo);
        List<String> deleteAfterList = new ArrayList<>(afterListAll);
        Set<String> afterCodeList = new HashSet<>();
        afterCodeList.addAll(stepList);
        afterCodeList.addAll(afterListAll);
        List<String> afterList = new ArrayList<>(afterCodeList);

        QueryWrapper<OperatorEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId())
                .in(OperatorEntity::getNodeCode, afterList);
        List<OperatorEntity> list = operatorMapper.selectList(wrapper);

        // 当前审批时  保存 任务、经办、外部 转json
        QueryWrapper<EventLogEntity> logWrapper = new QueryWrapper<>();
        logWrapper.lambda().eq(EventLogEntity::getTaskId, operator.getTaskId())
                .in(EventLogEntity::getNodeCode, afterList);
        logWrapper.lambda().select(EventLogEntity::getId, EventLogEntity::getNodeCode);
        List<EventLogEntity> logList = eventLogMapper.selectList(logWrapper);

        if (Boolean.FALSE.equals(triggerBack)) {
            // 记录
            FlowMethod flowMethod = new FlowMethod();
            flowMethod.setType(RecordEnum.BACK.getCode());
            flowMethod.setFlowModel(flowModel);
            flowMethod.setOperatorEntity(operator);
            recordMapper.createRecord(flowMethod);
        }

        if (!isPresentType) {
            // 子流程处理，重新审批：子流程需删除，当前审批：无需终止
            deleteSubflow(taskEntity.getId(), afterList);
            // 记录作废
            recordMapper.updateStatusToInvalid(taskEntity.getId(), afterList);
            // 更改经办的操作
            for (OperatorEntity operatorEntity : list) {
                operatorEntity.setCompletion(FlowNature.ACTION);
                // 删除超时相关
                FlowJobUtil.deleteByOperatorId(operatorEntity.getId(), redisUtil);
            }
            operatorMapper.updateById(list);
        }

        if (!launchFlowList.isEmpty()) {
            return;
        }

        // 节点记录
        NodeRecordModel nodeRecordModel = new NodeRecordModel();
        nodeRecordModel.setTaskId(operator.getTaskId());
        nodeRecordModel.setNodeId(operator.getNodeId());
        nodeRecordModel.setNodeCode(operator.getNodeCode());
        nodeRecordModel.setNodeName(operator.getNodeName());
        nodeRecordModel.setNodeStatus(NodeStateEnum.BACK.getCode());
        nodeRecordMapper.create(nodeRecordModel);

        addTask(ImmutableList.of(taskEntity.getId()));

        RejectDataEntity rejectDataEntity = null;
        if (isPresentType) {
            rejectDataEntity = rejectDataMapper.create(taskEntity, list, logList, stepList.get(0));
            //更新外部节点
            UpdateWrapper<EventLogEntity> updateLogWrapper = new UpdateWrapper<>();
            updateLogWrapper.lambda().eq(EventLogEntity::getTaskId, operator.getTaskId());
            updateLogWrapper.lambda().in(EventLogEntity::getNodeCode, afterList);
            updateLogWrapper.lambda().set(EventLogEntity::getNodeCode, null);
            eventLogMapper.update(updateLogWrapper);
        } else {
            // 重新审批，删除候选人、逐级、外部
            if (CollUtil.isNotEmpty(deleteAfterList)) {
                candidatesMapper.deleteByCodes(taskEntity.getId(), deleteAfterList);
                launchUserMapper.delete(taskEntity.getId(), deleteAfterList);
                eventLogMapper.delete(taskEntity.getId(), deleteAfterList);
            }
        }

        // 更改状态 state 转 -1
        for (OperatorEntity operatorEntity : list) {
            operatorEntity.setStatus(OperatorStateEnum.FUTILITY.getCode());
            // 删除超时相关
            FlowJobUtil.deleteByOperatorId(operatorEntity.getId(), redisUtil);
        }
        operatorMapper.updateById(list);

        UpdateWrapper<TaskEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().eq(TaskEntity::getId, operator.getTaskId());
        if (null != rejectDataEntity) {
            updateWrapper.lambda().set(TaskEntity::getRejectDataId, rejectDataEntity.getId());
        }
        FlowMsgModel flowMsgModel = new FlowMsgModel();

        // 开始节点
        if (stepList.size() == 1 && stepList.get(0).equals(start.getNodeCode())) {
            // 更新任务
            updateWrapper.lambda()
                    .set(TaskEntity::getCurrentNodeName, FlowNature.START_NAME)
                    .set(TaskEntity::getCurrentNodeCode, FlowNature.START_CODE)
                    .set(TaskEntity::getStatus, TaskStatusEnum.BACKED.getCode());
            if (rejectDataEntity == null) {
                updateWrapper.lambda().set(TaskEntity::getInstanceId, null);
            }
            taskMapper.update(updateWrapper);

            List<OperatorEntity> entityList = new ArrayList<>();
            OperatorEntity operatorEntity = new OperatorEntity();
            operatorEntity.setNodeCode(start.getNodeCode());
            operatorEntity.setHandleId(taskEntity.getCreatorUserId());
            entityList.add(operatorEntity);
            flowMsgModel.setOperatorList(entityList);
            flowMsgModel.setWait(false);

            if (!isPresentType) {
                // 删除发起用户
                launchUserMapper.delete(taskEntity.getId());
            }
            // 删除引擎实例
            if (rejectDataEntity == null) {
                String instanceId = taskEntity.getInstanceId();
                flowAbleUrl.deleteInstance(instanceId, "back");
            }
        } else {
            String sourceId = StringUtils.isNotEmpty(flowModel.getBackId()) ? flowModel.getBackId() : operator.getNodeId();
            BackFo fo = new BackFo();
            fo.setTaskId(sourceId);
            fo.setTargetKey(backStep);
            flowAbleUrl.back(fo);
            List<OperatorEntity> entityList = this.handleOperator(flowModel);

            // 更新状态为退回状态
            for (OperatorEntity entity : entityList) {
                entity.setStatus(OperatorStateEnum.BACK.getCode());
            }
            operatorMapper.updateById(entityList);

            flowMsgModel.setOperatorList(entityList);
            addOperatorList(entityList, flowModel);

            if (null != rejectDataEntity) {
                taskMapper.update(updateWrapper);
            }
        }

        // 消息
        flowMsgModel.setNodeList(nodeEntityList);
        flowMsgModel.setUserInfo(flowModel.getUserInfo());
        flowMsgModel.setTaskEntity(taskEntity);
        flowMsgModel.setNodeCode(nodeModel.getNodeId());
        flowMsgModel.setBack(true);
        flowMsgModel.setFormData(FlowContextHolder.getAllData());
        msgUtil.message(flowMsgModel);

        //退回事件
        flowModel.setEventStatus(EventEnum.BACK.getStatus());
        addEvent(flowModel);
    }

    // 系统审批
    public void systemAudit() {
        List<SystemAuditModel> systemList = SystemAuditHolder.getAll();
        if (CollUtil.isNotEmpty(systemList)) {
            List<SystemAuditModel> jsonToList = JsonUtil.getJsonToList(systemList, SystemAuditModel.class);

            SystemAuditHolder.clear();
            Map<String, TaskEntity> taskMap = new HashMap<>();
            for (SystemAuditModel model : jsonToList) {
                OperatorEntity operator = model.getOperator();
                FlowModel flowModel = model.getFlowModel();
                try {
                    String taskId = operator.getTaskId();
                    TaskEntity task = taskMap.get(taskId) != null ? taskMap.get(taskId) : taskMapper.getInfo(taskId);
                    if (Objects.equals(task.getStatus(), TaskStatusEnum.REJECTED.getCode())) {
                        continue;
                    }
                    flowModel.setTaskEntity(task);
                    this.autoAudit(operator, flowModel);
                    taskMap.put(taskId, task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public OperatorEntity saveSystemOperator(FlowMethod flowMethod) {
        NodeModel nodeModel = flowMethod.getNodeModel();
        FlowModel flowModel = flowMethod.getFlowModel();

        TaskEntity taskEntity = flowModel.getTaskEntity();
        String flowableTaskId = flowModel.getFlowableTaskId();

        OperatorEntity entity = new OperatorEntity();
        entity.setStatus(OperatorStateEnum.RUNING.getCode());
        entity.setCompletion(FlowNature.ACTION);
        entity.setParentId(FlowNature.PARENT_ID);

        entity.setTaskId(taskEntity.getId());
        entity.setNodeCode(nodeModel.getNodeId());
        entity.setNodeId(flowableTaskId);
        entity.setNodeName(nodeModel.getNodeName());
        entity.setEngineType(taskEntity.getEngineType());

        entity.setSignTime(new Date());
        entity.setStartHandleTime(new Date());
        entity.setHandleId(FlowNature.SYSTEM_CODE);
        entity.setHandleTime(new Date());
        entity.setIsProcessing(ObjectUtil.equals(nodeModel.getType(), NodeEnum.PROCESSING.getType()) ? FlowNature.PROCESSING : FlowNature.NOT_PROCESSING);

        operatorMapper.insert(entity);

        return entity;
    }

    // 全局属性的自动审批
    public boolean handleGlobalAuto(FlowMethod flowMethod) throws WorkFlowException {
        OperatorEntity entity = flowMethod.getOperatorEntity();
        NodeModel global = flowMethod.getNodeModel();
        FlowModel flowModel = flowMethod.getFlowModel();
        TaskEntity taskEntity = flowModel.getTaskEntity();
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        String deploymentId = flowModel.getDeploymentId();
        if (null == entity.getSignTime()) {
            entity.setSignTime(new Date());
        }

        // 自动提交规则
        AutoSubmitConfig autoSubmitConfig = global.getAutoSubmitConfig();
        // 相邻节点审批人重复
        if (Boolean.TRUE.equals(autoSubmitConfig.getAdjacentNodeApproverRepeated())) {
            // 获取上一级节点编码
            List<String> nodeCodeList = new ArrayList<>();
            flowUtil.prevNodeList(deploymentId, entity.getNodeCode(), nodeEntityList, nodeCodeList);
            if (CollUtil.isNotEmpty(nodeCodeList)) {
                QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
                queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskEntity.getId())
                        .eq(OperatorEntity::getHandleId, entity.getHandleId()).isNotNull(OperatorEntity::getHandleStatus)
                        .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode())
                        .in(OperatorEntity::getNodeCode, nodeCodeList);
                long count = operatorMapper.selectCount(queryWrapper);
                if (count > 0) {
                    entity.setStartHandleTime(new Date());
                    return true;
                }
            }
        }
        // 审批人审批过该流程
        if (Boolean.TRUE.equals(autoSubmitConfig.getApproverHasApproval())) {
            QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskEntity.getId())
                    .eq(OperatorEntity::getHandleId, entity.getHandleId()).ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode())
                    .isNotNull(OperatorEntity::getHandleStatus);
            long count = operatorMapper.selectCount(queryWrapper);
            if (count > 0) {
                entity.setStartHandleTime(new Date());
                return true;
            }
        }
        // 发起人与审批人重复
        if (Boolean.TRUE.equals(autoSubmitConfig.getInitiatorApproverRepeated())
                && StringUtils.equals(taskEntity.getCreatorUserId(), entity.getHandleId())) {
            entity.setStartHandleTime(new Date());
            return true;
        }

        return false;
    }

    public List<OperatorEntity> getChildList(String id) {
        QueryWrapper<OperatorEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(OperatorEntity::getParentId, id)
                .ne(OperatorEntity::getStatus, OperatorStateEnum.ASSIST.getCode());
        return operatorMapper.selectList(wrapper);
    }

    // 处理加签审批比例
    public void handleAddCounterSign(OperatorEntity operator, FlowModel flowModel) throws WorkFlowException {
        if (null == operator.getParentId()) {
            return;
        }
        OperatorEntity parentEntity = operatorMapper.getInfo(operator.getParentId());
        if (null == parentEntity) {
            return;
        }
        String handleParameter = parentEntity.getHandleParameter();
        AddSignModel addSignModel = JsonUtil.getJsonToBean(handleParameter, AddSignModel.class);
        if (null == addSignModel) {
            return;
        }
        TaskEntity taskEntity = flowModel.getTaskEntity();
        List<OperatorEntity> list = this.getChildList(operator.getParentId());

        int total = list.size();
        long countAudit = list.stream().filter(e -> FlowNature.AUDIT_COMPLETION.equals(e.getHandleStatus()) && null != e.getHandleTime()).count();
        long countReject = list.stream().filter(e -> FlowNature.REJECT_COMPLETION.equals(e.getHandleStatus()) && null != e.getHandleTime()).count();

        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setAddSignModel(addSignModel);
        flowMethod.setOperatorEntity(parentEntity);
        flowMethod.setFlowModel(flowModel);
        if (ObjectUtil.equals(addSignModel.getCounterSign(), FlowNature.IMPROPER_APPROVER)) {
            if (ObjectUtil.equals(flowModel.getHandleStatus(), FlowNature.REJECT_COMPLETION)) {
                Map<String, NodeModel> nodes = flowModel.getNodes();
                NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
                // 101003前加签给10001、10002、10003，不管是否开启拒绝继续流转，10001通过，10002拒绝，应该回到101003那里
                // 后加签逻辑：流程设置继续流转，依次审批中只要有人拒绝，其他审批人不用审批流程就流转下一节点；流程未设置继续流转，依次审批中只要有人拒绝，就拒绝回发起节点
                if (Boolean.FALSE.equals(global.getHasContinueAfterReject())) {
                    if (ObjectUtil.equals(addSignModel.getAddSignType(), FlowNature.LATER)) {
                        flowMethod.setTaskEntity(taskEntity);
                        handleEndTask(flowMethod);
                    } else {
                        this.handleAddSign(flowMethod);
                    }
                } else {
                    this.handleAddSign(flowMethod);
                }
                return;
            }
            String userId = operator.getHandleId();
            // 转审的经办，通过记录获取原来的审批人
            if (ObjectUtil.equals(operator.getStatus(), OperatorStateEnum.TRANSFER.getCode())) {
                RecordEntity transferRecord = recordMapper.getTransferRecord(operator.getId());
                String handleId = transferRecord.getHandleId();
                if (StringUtils.isNotBlank(handleId)) {
                    userId = handleId;
                }
            }
            List<String> idList = addSignModel.getAddSignUserIdList();
            int index = idList.indexOf(userId);
            if (index == idList.size() - 1) {
                // 依次审批最后一个人
                this.handleAddSign(flowMethod);
            } else {
                NodeModel global = flowModel.getNodes().get(NodeEnum.GLOBAL.getType());
                String handleId = idList.get(index + 1);
                OperatorEntity entity = this.createOperator(operator, OperatorStateEnum.ADD_SIGN.getCode(), handleId, global);
                operatorMapper.insert(entity);
                flowMethod.setNodeEntity(flowModel.getNodeEntity());
                flowMethod.setTaskEntity(taskEntity);
                flowMethod.setNodeEntityList(flowModel.getNodeEntityList());
                this.improperApproverMessage(flowMethod, entity);
            }
        } else if (ObjectUtil.equals(addSignModel.getCounterSign(), FlowNature.FIXED_JOINTLY_APPROVER)) {
            // 会签
            Integer ratio = addSignModel.getAuditRatio();

            int rejectRatio = 100 - ratio;
            int rejectRes = (int) (countReject * 100 / total);
            if (rejectRes != 0 && rejectRes >= rejectRatio) {
                // 直接拒绝
                flowMethod.setHandleStatus(FlowNature.REJECT_COMPLETION);
                this.handleAddSign(flowMethod);
                this.endAddSign(parentEntity.getId());
                return;
            }

            int res = (int) (countAudit * 100 / total);
            if (res >= ratio) {
                flowMethod.setHandleStatus(FlowNature.AUDIT_COMPLETION);
                this.handleAddSign(flowMethod);
                this.endAddSign(parentEntity.getId());
            }
        } else {
            int auditFlag = FlowNature.AUDIT_COMPLETION;
            // 或签
            if (countReject >= 1) {
                auditFlag = FlowNature.REJECT_COMPLETION;
            }
            flowMethod.setHandleStatus(auditFlag);
            this.handleAddSign(flowMethod);
            this.endAddSign(parentEntity.getId());
        }
    }

    public void handleAddSign(FlowMethod flowMethod) throws WorkFlowException {
        AddSignModel addSignModel = flowMethod.getAddSignModel();
        OperatorEntity parentEntity = flowMethod.getOperatorEntity();
        FlowModel flowModel = flowMethod.getFlowModel();
        if (addSignModel.getAddSignType().equals(FlowNature.LATER)) {
            // 后加签需要默认同意
            flowModel.setHandleStatus(FlowNature.AUDIT_COMPLETION);
            UserEntity userEntity = serviceUtil.getUserInfo(parentEntity.getHandleId());
            UserInfo userInfo = JsonUtil.getJsonToBean(UserProvider.getUser(), UserInfo.class);
            userInfo.setUserId(userEntity.getId());
            userInfo.setUserName(userEntity.getRealName());
            flowModel.setUserInfo(userInfo);
            flowModel.setCopyMsgFlag(false);
            parentEntity.setHandleStatus(null);
            audit(parentEntity, flowModel);
            flowModel.setCopyMsgFlag(true);
        } else {
            UpdateWrapper<OperatorEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.lambda().eq(OperatorEntity::getId, parentEntity.getId())
                    .set(OperatorEntity::getCreatorTime, new Date())
                    .set(OperatorEntity::getCompletion, FlowNature.NORMAL);
            NodeModel global = flowModel.getNodes().get(NodeEnum.GLOBAL.getType());
            Boolean flowTodo = serviceUtil.getFlowTodo();
            if (Boolean.FALSE.equals(flowTodo)) {
                updateWrapper.lambda().set(OperatorEntity::getStartHandleTime, null);
                Boolean flowSign = serviceUtil.getFlowSign();
                if (Boolean.FALSE.equals(flowSign) && global != null && Boolean.TRUE.equals(global.getHasSignFor())) {
                    updateWrapper.lambda().set(OperatorEntity::getSignTime, null);
                }

            }
            operatorMapper.update(updateWrapper);
            parentEntity = operatorMapper.selectById(parentEntity.getId());
            // 超时经办
            List<OperatorEntity> list = new ArrayList<>();
            list.add(parentEntity);
            for (OperatorEntity operatorEntity : list) {
                FlowJobUtil.deleteByOperatorId(operatorEntity.getId(), redisUtil);
            }
            TaskEntity taskEntity = flowModel.getTaskEntity();
            UserEntity userEntity = serviceUtil.getUserInfo(taskEntity.getCreatorUserId());
            if (null != userEntity) {
                FlowModel model = JsonUtil.getJsonToBean(flowModel, FlowModel.class);
                UserInfo userInfo = model.getUserInfo();
                userInfo.setUserName(userEntity.getRealName());
                userInfo.setUserAccount(userEntity.getAccount());
                userInfo.setUserId(userEntity.getId());
                model.setUserInfo(userInfo);
                addOperatorList(list, model);
            }
        }
    }

    // 设置可减签的人员名称
    public List<CandidateUserVo> getReduceUsers(List<OperatorEntity> todoList, Pagination pagination) {
        List<String> ids = todoList.stream().map(OperatorEntity::getHandleId).collect(Collectors.toList());
        return getUserModel(ids, pagination);
    }


    // 处理退回数据，当前的经办通过计算比例后进入该方法，无需变更状态
    public void handleRejectData(FlowModel flowModel) throws WorkFlowException {
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        OperatorEntity operatorEntity = flowModel.getOperatorEntity();

        TaskEntity taskEntity = flowModel.getTaskEntity();
        String currentNodeCode = taskEntity.getCurrentNodeCode();
        // 待提交（即未发起）重新发起，需要获取当前节点
        String instanceId = taskEntity.getInstanceId();
        if (instanceId != null) {
            List<FlowableTaskModel> taskModelList = flowAbleUrl.getCurrentTask(instanceId);
            List<String> currentCodes = taskModelList.stream().map(FlowableTaskModel::getTaskKey).collect(Collectors.toList());
            currentNodeCode = String.join(",", currentCodes);
        }

        String rejectDataId = taskEntity.getRejectDataId();

        RejectDataEntity rejectData = rejectDataMapper.getInfo(taskEntity.getRejectDataId());
        if (!ObjectUtil.equals(rejectData.getNodeCode(), operatorEntity.getNodeCode())) {
            throw new WorkFlowException("当前节点无法操作");
        }
        String taskJson = rejectData.getTaskJson();
        String operatorJson = rejectData.getOperatorJson();
        String eventJson = rejectData.getEventLogJson();

        List<OperatorEntity> srcOperatorList = JsonUtil.getJsonToList(operatorJson, OperatorEntity.class);
        // 获取被退回的节点编码，用于表单值传递
        List<String> nodeCodes = srcOperatorList.stream().sorted(Comparator.comparing(OperatorEntity::getCreatorTime))
                .map(OperatorEntity::getNodeCode).distinct().collect(Collectors.toList());

        List<EventLogEntity> srcEventList = JsonUtil.getJsonToList(eventJson, EventLogEntity.class);

        TaskEntity srcTask = JsonUtil.getJsonToBean(taskJson, TaskEntity.class);
        String srcCurrentNodeCode = srcTask.getCurrentNodeCode();
        // 跳转
        JumpFo fo = new JumpFo();
        fo.setInstanceId(instanceId);
        List<String> sourceList = StringUtils.isNotEmpty(currentNodeCode) ? Arrays.stream(currentNodeCode.split(",")).collect(Collectors.toList()) : new ArrayList<>();
        List<String> targetList = StringUtils.isNotEmpty(srcCurrentNodeCode) ? Arrays.stream(srcCurrentNodeCode.split(",")).collect(Collectors.toList()) : new ArrayList<>();

        List<String> source = sourceList.stream().filter(e -> !targetList.contains(e)).collect(Collectors.toList());
        List<String> target = targetList.stream().filter(e -> !sourceList.contains(e)).collect(Collectors.toList());
        fo.setSource(source);
        fo.setTarget(target);
        flowAbleUrl.jump(fo);

        // 跳转后，获取当前的节点信息，更新经办
        List<FlowableTaskModel> taskModelList = flowAbleUrl.getCurrentTask(instanceId);
        if (CollUtil.isNotEmpty(taskModelList)) {
            for (OperatorEntity operator : srcOperatorList) {
                FlowableTaskModel model = taskModelList.stream()
                        .filter(e -> ObjectUtil.equals(e.getTaskKey(), operator.getNodeCode())).findFirst().orElse(null);
                if (null != model) {
                    operator.setNodeId(model.getTaskId());
                }
                operator.setCreatorTime(new Date());
            }
        }
        // 还原经办
        operatorMapper.updateById(srcOperatorList);

        //还原外部节点
        eventLogMapper.updateById(srcEventList);

        // 还原任务
        UpdateWrapper<TaskEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().eq(TaskEntity::getId, srcTask.getId())
                .set(TaskEntity::getInstanceId, instanceId)
                .set(TaskEntity::getCurrentNodeCode, srcTask.getCurrentNodeCode())
                .set(TaskEntity::getCurrentNodeName, srcTask.getCurrentNodeName())
                .set(TaskEntity::getStatus, srcTask.getStatus())
                .set(TaskEntity::getStartTime, srcTask.getStartTime())
                .set(TaskEntity::getRejectDataId, null);
        taskMapper.update(updateWrapper);

        // 赋值更新后的任务
        flowModel.setTaskEntity(srcTask);

        rejectDataMapper.deleteById(rejectDataId);

        // 处理表单值传递
        for (TemplateNodeEntity nodeEntity : nodeEntityList) {
            if (nodeCodes.contains(nodeEntity.getNodeCode())) {
                FlowMethod flowMethod = new FlowMethod();
                flowMethod.setTaskEntity(taskEntity);
                flowMethod.setNodeEntity(nodeEntity);
                flowMethod.setNodeEntityList(nodeEntityList);
                flowMethod.setFlowModel(flowModel);
                flowMethod.setIsAssign(true);
                dataTransfer(flowMethod);
            }
        }
    }

    // 获取任务 节点下的经办
    public List<OperatorEntity> getByNodeCode(String taskId, String nodeCode) {
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskId)
                .eq(OperatorEntity::getNodeCode, nodeCode)
                .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode());
        List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            return list;
        }
        return new ArrayList<>();
    }

    // 处理指派
    public OperatorEntity handleAssign(FlowModel flowModel) throws WorkFlowException {
        TaskEntity taskEntity = flowModel.getTaskEntity();
        NodeModel global = flowModel.getNodes().get(NodeEnum.GLOBAL.getType());
        String nodeCode = flowModel.getNodeCode();
        String assignUserId = flowModel.getHandleIds();
        NodeModel nodeModel = flowModel.getNodes().get(nodeCode);
        Integer transferState = ObjectUtil.equals(nodeModel.getType(), NodeEnum.PROCESSING.getType()) ?
                OperatorStateEnum.TRANSFER_PROCESSING.getCode() : OperatorStateEnum.TRANSFER.getCode();

        Integer state = Boolean.FALSE.equals(flowModel.getAutoTransferFlag()) ? OperatorStateEnum.ASSIGNED.getCode() : transferState;

        List<OperatorEntity> operatorList = this.getByNodeCode(taskEntity.getId(), nodeCode);
        if (CollUtil.isNotEmpty(operatorList)) {
            // 作废记录
            recordMapper.invalid(operatorList);
            // 删除原来的经办
            operatorMapper.deleteByIds(operatorList);

            OperatorEntity operator = operatorList.get(0);
            operator.setParentId(FlowNature.PARENT_ID);
            // 生成指派经办
            OperatorEntity entity = this.createOperator(operator, state, assignUserId, global);

            operatorMapper.insert(entity);
            return entity;
        } else {
            OperatorEntity entity = new OperatorEntity();
            entity.setId(RandomUtil.uuId());
            NodeModel node = flowModel.getNodes().get(nodeCode);
            entity.setNodeName(node.getNodeName());
            List<FlowableTaskModel> currentTask = flowAbleUrl.getCurrentTask(taskEntity.getInstanceId());
            FlowableTaskModel model = currentTask.stream().filter(e -> e.getTaskKey().equals(nodeCode)).findFirst().orElse(null);
            if (null == model) {
                throw new WorkFlowException(MsgCode.FA001.get());
            }
            entity.setNodeId(model.getTaskId());
            entity.setNodeCode(nodeCode);
            entity.setStatus(state);
            entity.setTaskId(taskEntity.getId());
            entity.setEngineType(taskEntity.getEngineType());
            Boolean flowTodo = serviceUtil.getFlowTodo();
            if (Boolean.TRUE.equals(flowTodo)) {
                entity.setSignTime(new Date());
                entity.setStartHandleTime(new Date());
            } else {
                Boolean flowSign = serviceUtil.getFlowSign();
                if (Boolean.TRUE.equals(flowSign)) {
                    entity.setSignTime(new Date());
                } else {
                    if (global != null && !global.getHasSignFor()) {
                        entity.setSignTime(new Date());
                    }
                }
            }
            if (node.getCounterSign().equals(FlowNature.IMPROPER_APPROVER)) {
                entity.setHandleAll(assignUserId);
            }
            entity.setHandleId(assignUserId);
            entity.setParentId(FlowNature.PARENT_ID);
            entity.setCompletion(FlowNature.NORMAL);
            operatorMapper.insert(entity);
            return entity;
        }
    }

    public void handleRevoke(FlowModel flowModel, OperatorEntity operator, RevokeEntity revokeEntity) throws WorkFlowException {
        TaskEntity taskEntity = flowModel.getTaskEntity();
        Integer handleStatus = flowModel.getHandleStatus();
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                .filter(e -> StringUtils.equals(e.getNodeCode(), operator.getNodeCode())).findFirst().orElse(null);

        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setNodeEntity(nodeEntity);
        flowMethod.setNodeEntityList(nodeEntityList);

        FlowMsgModel flowMsgModel = new FlowMsgModel();
        flowMsgModel.setFormData(FlowContextHolder.getAllData());
        flowMsgModel.setNodeList(nodeEntityList);
        flowMsgModel.setUserInfo(flowModel.getUserInfo());
        flowMsgModel.setTaskEntity(taskEntity);
        flowMsgModel.setNodeCode(operator.getNodeCode());
        flowMsgModel.setWait(false);
        List<OperatorEntity> operatorList = new ArrayList<>();
        OperatorEntity entity = new OperatorEntity();
        entity.setNodeCode(operator.getNodeCode());
        entity.setHandleId(taskEntity.getCreatorUserId());
        operatorList.add(entity);
        flowMsgModel.setOperatorList(operatorList);

        TaskEntity task = taskMapper.selectById(revokeEntity.getTaskId());
        boolean endFlag = false;
        if (ObjectUtil.equals(handleStatus, FlowNature.AUDIT_COMPLETION)) {
            flowMsgModel.setApprove(true);
            // 消息
            msgUtil.message(flowMsgModel);
            operator.setHandleStatus(FlowNature.AUDIT_COMPLETION);
            flowMethod.setType(RecordEnum.AUDIT.getCode());
        } else {
            operator.setHandleStatus(FlowNature.REJECT_COMPLETION);
            flowMethod.setType(RecordEnum.REJECT.getCode());
            // 结束
            endFlag = true;
        }

        // 经办记录
        flowMethod.setFlowModel(flowModel);
        flowMethod.setOperatorEntity(operator);
        recordMapper.createRecord(flowMethod);

        operator.setHandleTime(new Date());
        operator.setCompletion(FlowNature.ACTION);
        operatorMapper.updateById(operator);

        flowMethod.setTaskEntity(taskEntity);
        List<CirculateEntity> circulateList = circulateList(flowMethod);
        flowMethod.setCirculateList(circulateList);

        if (endFlag) {
            flowMethod.setIsRevoke(true);
            handleEndTask(flowMethod);
            revokeMapper.deleteRevoke(taskEntity.getId());
            return;
        }
        // 判断撤销经办的比例
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId()).eq(OperatorEntity::getNodeCode, operator.getNodeCode())
                .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode());
        List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);

        long count = list.stream().filter(e -> ObjectUtil.equals(e.getHandleStatus(), FlowNature.AUDIT_COMPLETION)).count();
        if (count == list.size()) {
            // 节点记录
            NodeRecordModel nodeRecordModel = new NodeRecordModel();
            nodeRecordModel.setTaskId(operator.getTaskId());
            nodeRecordModel.setNodeId(operator.getNodeId());
            nodeRecordModel.setNodeCode(operator.getNodeCode());
            nodeRecordModel.setNodeName(operator.getNodeName());
            nodeRecordModel.setNodeStatus(NodeStateEnum.PASS.getCode());
            nodeRecordMapper.create(nodeRecordModel);

            // 节点通过
            CompleteFo fo = new CompleteFo();
            fo.setTaskId(operator.getNodeId());
            flowAbleUrl.complete(fo);

            flowMethod.setTaskId(revokeEntity.getTaskId());
            flowMethod.setTaskEntity(taskEntity);
            this.handleRevokeOperator(flowMethod);

            // 判断结束，更新源任务的状态
            if (isFinished(flowModel) && null != task) {

                endRevoke(task);

            }
        }
    }

    public OperatorEntity createOperator(OperatorEntity operator, Integer state, String handleId, NodeModel global) {
        OperatorEntity entity = new OperatorEntity();
        entity.setId(RandomUtil.uuId());
        entity.setNodeName(operator.getNodeName());
        entity.setNodeId(operator.getNodeId());
        entity.setNodeCode(operator.getNodeCode());
        entity.setStatus(state);
        entity.setTaskId(operator.getTaskId());
        entity.setIsProcessing(operator.getIsProcessing());
        entity.setEngineType(operator.getEngineType());
        Boolean flowTodo = serviceUtil.getFlowTodo();
        if (Boolean.TRUE.equals(flowTodo)) {
            entity.setSignTime(new Date());
            entity.setStartHandleTime(new Date());
        } else {
            Boolean flowSign = serviceUtil.getFlowSign();
            if (Boolean.TRUE.equals(flowSign)) {
                entity.setSignTime(new Date());
            } else {
                if (global != null && !global.getHasSignFor()) {
                    entity.setSignTime(new Date());
                }
            }
        }
        entity.setHandleId(handleId);
        entity.setHandleAll(operator.getHandleAll());
        entity.setParentId(operator.getParentId());
        entity.setCompletion(FlowNature.NORMAL);
        return entity;
    }

    // 处理子流程
    public void handleSubFlow(TemplateNodeEntity nodeEntity, FlowModel flowModel) throws WorkFlowException {
        if (!nodeEntity.getNodeType().equals(NodeEnum.SUB_FLOW.getType())) {
            return;
        }
        NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        Map<String, List<String>> errorRuleUserList = flowModel.getErrorRuleUserList();

        String templateId = nodeModel.getFlowId();
        TemplateEntity template = templateMapper.selectById(templateId);
        if (null == template) {
            return;
        }
        if (!ObjectUtil.equals(template.getStatus(), TemplateStatueEnum.UP.getCode())) {
            throw new WorkFlowException(MsgCode.WF140.get());
        }
        String flowId = template.getFlowId();

        TemplateJsonEntity jsonEntity = templateJsonMapper.selectById(flowId);
        if (null == jsonEntity) {
            return;
        }
        List<TemplateNodeEntity> subNodeEntityList = templateNodeMapper.getList(jsonEntity.getId());
        Map<String, NodeModel> subNodes = new HashMap<>();
        for (TemplateNodeEntity node : subNodeEntityList) {
            subNodes.put(node.getNodeCode(), JsonUtil.getJsonToBean(node.getNodeJson(), NodeModel.class));
        }
        FlowMethod method = new FlowMethod();
        method.setDeploymentId(jsonEntity.getFlowableId());
        TemplateNodeEntity subStart = subNodeEntityList.stream()
                .filter(e -> StringUtils.equals(NodeEnum.START.getType(), e.getNodeType())).findFirst().orElse(new TemplateNodeEntity());
        method.setNodes(subNodes);
        method.setTaskEntity(flowModel.getTaskEntity());
        method.setNodeCode(subStart.getNodeCode());
        method.setNodeEntityList(subNodeEntityList);

        List<NodeModel> nextApprover = flowUtil.getNextApprover(method);
        boolean autoSubmit = ObjectUtil.equals(nodeModel.getAutoSubmit(), 1);
        if (autoSubmit) {
            if (flowUtil.checkBranch(subStart)) {
                throw new WorkFlowException(MsgCode.WF121.get());
            }
            if (!flowUtil.checkNextCandidates(nextApprover)) {
                throw new WorkFlowException(MsgCode.WF121.get());
            }
        }

        String nodeCode = nodeModel.getNodeId();
        TaskEntity taskEntity = flowModel.getTaskEntity();
        String deploymentId = flowModel.getDeploymentId();
        Map<String, NodeModel> nodes = flowModel.getNodes();
        String flowableTaskId = flowModel.getFlowableTaskId();

        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setFlowModel(flowModel);
        flowMethod.setTaskEntity(taskEntity);
        flowMethod.setNodeEntity(nodeEntity);
        flowMethod.setNodeEntityList(nodeEntityList);
        flowMethod.setErrorRule(true);
        flowMethod.setExtraRule(true);
        // 获取上一级节点
        List<String> nodeCodeList = new ArrayList<>();
        flowUtil.prevNodeList(deploymentId, nodeCode, nodeEntityList, nodeCodeList);
        nodeCodeList = nodeCodeList.stream().distinct().collect(Collectors.toList());
        String parentCode;
        if (nodeCodeList.size() == 1) {
            parentCode = nodeCodeList.get(0);
        } else if (nodeCodeList.size() > 1) {
            // 如果子流程是合流节点 就存最后一个审批的分流节点
            QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskEntity.getId())
                    .in(OperatorEntity::getNodeCode, nodeCodeList).isNotNull(OperatorEntity::getHandleStatus)
                    .orderByDesc(OperatorEntity::getHandleTime);
            List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
            if (CollUtil.isNotEmpty(list)) {
                parentCode = list.get(0).getNodeCode();
            } else {
                parentCode = null;
            }
        } else {
            TemplateNodeEntity startNode = nodeEntityList.stream()
                    .filter(e -> StringUtils.equals(e.getNodeType(), NodeEnum.START.getType())).findFirst().orElse(new TemplateNodeEntity());
            parentCode = startNode.getNodeCode();
        }
        TemplateNodeEntity parentNode = nodeEntityList.stream()
                .filter(e -> StringUtils.equals(e.getNodeCode(), parentCode)).findFirst().orElse(new TemplateNodeEntity());
        String formId = parentNode.getFormId();
        Map<String, Boolean> resMap = new HashMap<>();
        Map<String, Map<String, Object>> allData = FlowContextHolder.getAllData();
        Map<String, Object> formData = allData.get(taskEntity.getId() + JnpfConst.SIDE_MARK + formId);
        if (nodeModel.getIsAsync().equals(FlowNature.CHILD_ASYNC)) {
            flowMethod.setDeploymentId(deploymentId);
            flowMethod.setNodeCode(nodeCode);
            flowMethod.setFormData(formData);
            flowMethod.setNodes(nodes);
            flowMethod.setTaskEntity(taskEntity);
            resMap = conditionUtil.handleCondition(flowMethod);
            conditionUtil.checkCondition(resMap, nodes);
        }
        // 表单数据传递
        formData = flowUtil.createOrUpdate(flowMethod);
        flowMethod.setSubFormData(formData);

        List<String> userIds = flowUtil.userListAll(flowMethod);
        List<UserEntity> userList = serviceUtil.getUserName(userIds, true);
        if (userList.isEmpty()) {
            userIds.add(serviceUtil.getAdmin());
        }

        Integer createRule = nodeModel.getCreateRule();
        boolean inTurn = ObjectUtil.equals(createRule, 1);
        int flag = 0;
        List<FlowModel> subTaskData = new ArrayList<>();

        UserInfo userInfo = flowModel.getUserInfo();

        Map<String, FlowModel> taskMap = new HashMap<>();
        for (UserEntity user : userList) {
            flag++;
            FlowModel model = new FlowModel();
            model.setSubCode(nodeCode);
            model.setParentId(taskEntity.getId());
            model.setDeploymentId(jsonEntity.getFlowableId());

            Map<String, Object> map = new HashMap<>();
            if (CollUtil.isNotEmpty(formData)) {
                map = new HashMap<>(formData);
            }
            map.put(FlowFormConstant.FLOWID, flowId);
            model.setFormData(map);
            model.setFlowId(flowId);
            model.setErrorRuleUserList(errorRuleUserList);

            // 自动提交 0.否
            if (autoSubmit) {
                model.setStatus(TaskStatusEnum.RUNNING.getCode());
            } else {
                model.setSubFlow(true);
            }

            SubParameterModel subParameter = new SubParameterModel();
            subParameter.setParentCode(parentCode);
            subParameter.setNodeId(flowableTaskId);
            model.setSubParameter(subParameter);

            UserInfo info = JsonUtil.getJsonToBean(userInfo, UserInfo.class);
            info.setUserName(user.getRealName());
            info.setUserAccount(user.getAccount());
            info.setUserId(user.getId());
            model.setUserInfo(info);

            model.setIsAsync(nodeModel.getIsAsync());
            model.setIsFlow(1);

            TaskEntity subTask = new TaskEntity();
            subTask.setCreatorUserId(user.getId());
            model.setTaskEntity(subTask);

            // 依次创建，只创建第一个人的，存储后续的人员参数
            if (inTurn && flag > 1) {
                subTaskData.add(model);
                continue;
            }

            if (nodeModel.getAutoSubmit().equals(1)) {
                model.setNodeEntityList(subNodeEntityList);
                if (flowUtil.checkNextError(model, nextApprover, false, false) != 0) {
                    throw new WorkFlowException(MsgCode.WF121.get());
                }
            }

            UserProvider.setLocalLoginUser(info);
            // 生成任务，根据是否自动发起进行提交
            try {
                saveOrSubmit(model);
                // 组装消息发送的人
                OperatorEntity operator = new OperatorEntity();
                operator.setHandleId(user.getId());
                operator.setNodeCode(nodeEntity.getNodeCode());
                // 子流程任务
                TaskEntity task = model.getTaskEntity();
                operator.setTaskId(task.getId());

                List<OperatorEntity> operatorList = new ArrayList<>();
                operatorList.add(operator);

                // 消息
                if (ObjectUtil.equals(nodeModel.getAutoSubmit(), 0)) {
                    FlowMsgModel flowMsgModel = new FlowMsgModel();
                    flowMsgModel.setOperatorList(operatorList);
                    flowMsgModel.setNodeList(nodeEntityList);
                    flowMsgModel.setUserInfo(flowModel.getUserInfo());
                    flowMsgModel.setTaskEntity(task);
                    flowMsgModel.setNodeCode(nodeCode);
                    flowMsgModel.setWait(false);
                    flowMsgModel.setLaunch(true);
                    flowMsgModel.setFormData(FlowContextHolder.getAllData());
                    msgUtil.message(flowMsgModel);
                }

                taskMap.put(task.getId(), model);
            } catch (WorkFlowException e) {
                throw new WorkFlowException(MsgCode.WF121.get());
            }
        }

        // 保存依次创建的子流程数据
        subtaskDataMapper.save(subTaskData);
        for (Map.Entry<String, FlowModel> stringFlowModelEntry : taskMap.entrySet()) {
            String taskId = stringFlowModelEntry.getKey();
            FlowModel model = taskMap.get(taskId);
            if (model == null) {
                continue;
            }
            this.autoAudit(model);
        }

        UserProvider.setLocalLoginUser(userInfo);

        // 异步，直接通过节点，后续继续递归下一级节点生成经办
        if (nodeModel.getIsAsync().equals(FlowNature.CHILD_ASYNC)) {
            flowMethod.setFlowableTaskId(flowableTaskId);
            flowMethod.setResMap(resMap);
            completeNode(flowMethod);
        }

        //子流程自动审批，更新主流程的当前节点
        String instanceId = taskEntity.getInstanceId();
        List<FlowableTaskModel> taskModelList = flowAbleUrl.getCurrentTask(instanceId);
        if (CollUtil.isEmpty(taskModelList)) {
            return;
        }
        updateCurrentNode(taskModelList, nodes, taskEntity);
    }

    // 完成节点
    public void completeNode(FlowMethod flowMethod) throws WorkFlowException {
        FlowModel flowModel = flowMethod.getFlowModel();
        String flowableTaskId = flowMethod.getFlowableTaskId();
        Map<String, Boolean> resMap = flowMethod.getResMap();
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        taskLineMapper.create(taskEntity.getId(), resMap);
        CompleteFo completeFo = new CompleteFo();
        completeFo.setTaskId(flowableTaskId);
        completeFo.setVariables(new HashMap<>(resMap));
        flowAbleUrl.complete(completeFo);

        this.addTask(ImmutableList.of(taskEntity.getId()));
        if (isFinished(flowModel)) {
            return;
        }

        // 生成经办
        List<OperatorEntity> operatorEntities = handleOperator(flowModel);

        List<TemplateNodeEntity> nodeEntityList = flowMethod.getNodeEntityList();
        FlowMsgModel flowMsgModel = new FlowMsgModel();
        flowMsgModel.setNodeList(nodeEntityList);
        flowMsgModel.setTaskEntity(taskEntity);
        flowMsgModel.setUserInfo(flowModel.getUserInfo());
        flowMsgModel.setOperatorList(operatorEntities);
        flowMsgModel.setFormData(FlowContextHolder.getAllData());
        msgUtil.message(flowMsgModel);

        if (taskEntity.getRejectDataId() == null) {
            this.autoAudit(flowModel);
        }
    }

    public void autoAudit(FlowModel flowModel) throws WorkFlowException {
        flowModel.setAutoAudit(true);
        this.autoAudit(flowModel, true);
    }

    public void autoAudit(FlowModel flowModel, Boolean flag) throws WorkFlowException {
        TaskEntity taskEntity = flowModel.getTaskEntity();
        if (null == taskEntity) {
            return;
        }
        flowModel.setBranchList(new ArrayList<>());
        String taskId = taskEntity.getId();
        List<OperatorEntity> operatorList = operatorMapper.getList(taskId);
        operatorList = operatorList.stream().filter(e -> !ObjectUtil.equals(e.getStatus(), OperatorStateEnum.BACK.getCode())
                && !ObjectUtil.equals(e.getCompletion(), FlowNature.ACTION)).collect(Collectors.toList());
        this.autoAudit(flowModel, operatorList, flag);
    }

    @DSTransactional
    public void autoAudit(FlowModel flowModel, List<OperatorEntity> operatorList, Boolean flag) throws WorkFlowException {
        // flag标识为true时 需要排除未激活的经办
        if (Boolean.TRUE.equals(flag)) {
            operatorList = operatorList.stream()
                    .filter(e -> !ObjectUtil.equals(e.getStatus(), OperatorStateEnum.WAITING.getCode())).collect(Collectors.toList());
        }
        if (CollUtil.isEmpty(operatorList)) {
            return;
        }
        String deploymentId = flowModel.getDeploymentId();
        TaskEntity taskEntity = flowModel.getTaskEntity();
        Map<String, NodeModel> nodes = flowModel.getNodes();
        NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        TemplateNodeEntity globalEntity = nodeEntityList.stream()
                .filter(e -> StringUtils.equals(NodeEnum.GLOBAL.getType(), e.getNodeType())).findFirst().orElse(new TemplateNodeEntity());

        flowModel.setSignImg(null);
        flowModel.setFileList(null);
        Map<String, List<OperatorEntity>> map = operatorList.stream().collect(Collectors.groupingBy(OperatorEntity::getNodeCode));
        List<String> handleIds = operatorList.stream().map(OperatorEntity::getHandleId).distinct().collect(Collectors.toList());
        List<UserEntity> users = serviceUtil.getUserName(handleIds);
        for (Map.Entry<String, List<OperatorEntity>> entry : map.entrySet()) {
            String key = entry.getKey();

            NodeModel model = nodes.get(key);
            String modelFormId = model.getFormId();
            modelFormId = StringUtils.isNotBlank(modelFormId) ? modelFormId : globalEntity.getFormId();
            Map<String, Object> formData = serviceUtil.infoData(modelFormId, taskEntity.getId());
            // 获取流程参数
            getGlobalParam(taskEntity, model, global, formData);
            flowModel.setTaskEntity(taskEntity);
            FlowMethod flowMethod = new FlowMethod();
            flowMethod.setTaskEntity(taskEntity);
            flowMethod.setFormData(formData);
            flowMethod.setDeploymentId(deploymentId);
            flowMethod.setNodeCode(key);
            flowMethod.setNodes(nodes);
            // 判断节点的线的条件
            Map<String, Boolean> resMap = conditionUtil.handleCondition(flowMethod);
            try {
                conditionUtil.checkCondition(resMap, nodes);
            } catch (WorkFlowException e) {
                continue;
            }
            List<NodeModel> nextApprover;
            boolean mark = true;
            try {
                nextApprover = flowUtil.getNextApprover(flowMethod);
            } catch (WorkFlowException e) {
                nextApprover = null;
                mark = false;
            }
            if (!mark) {
                continue;
            }
            List<OperatorEntity> list = map.get(key);
            if (CollUtil.isNotEmpty(list)) {
                flowModel.setOperatorEntity(list.get(0));
            }
            boolean isBranch = flowUtil.checkBranch(model);
            boolean nextCandidates = flowUtil.checkNextCandidates(nextApprover);
            int nextError = flowUtil.checkNextError(flowModel, nextApprover, false, false);
            if (!isBranch && nextCandidates && nextError == 0) {
                for (OperatorEntity operator : list) {
                    operator = operatorMapper.selectById(operator.getId());
                    if (null == operator || ObjectUtil.equals(operator.getCompletion(), FlowNature.ACTION)) {
                        continue;
                    }
                    String handleId = operator.getHandleId();
                    String username = "";
                    String userId = "";
                    String userName = "";
                    String userAccount = "";
                    UserEntity user = users.stream().filter(e -> ObjectUtil.equals(e.getId(), handleId)).findFirst().orElse(null);
                    if (user != null) {
                        username = user.getAccount() + "(" + user.getRealName() + ")";
                        userId = user.getId();
                        userName = user.getRealName();
                        userAccount = user.getAccount();
                    }
                    String str = ObjectUtil.equals(operator.getIsProcessing(), FlowNature.NOT_PROCESSING) ? "自动审批通过" : "自动办理通过";
                    flowModel.setHandleOpinion(username + str);
                    FlowMethod method = new FlowMethod();
                    method.setOperatorEntity(operator);
                    method.setNodeModel(global);
                    method.setFlowModel(flowModel);
                    if (this.handleGlobalAuto(method)) {
                        if (taskEntity.getEndTime() != null) {
                            return;
                        }
                        UserInfo userInfo = flowModel.getUserInfo();
                        UserInfo info = JsonUtil.getJsonToBean(userInfo, UserInfo.class);
                        info.setUserId(userId);
                        info.setUserAccount(userAccount);
                        info.setUserName(userName);
                        UserProvider.setLocalLoginUser(info);

                        flowModel.setUserInfo(info);
                        flowModel.setHandleStatus(FlowNature.AUDIT_COMPLETION);
                        audit(operator, flowModel);
                        UserProvider.setLocalLoginUser(userInfo);
                        if (taskEntity.getRejectDataId() == null) {
                            this.autoAudit(flowModel);
                        }
                    }
                }
            }

        }

    }

    // 异常补偿
    public void compensate(TaskEntity taskEntity) throws WorkFlowException {
        String instanceId = taskEntity.getInstanceId();
        CompensateFo fo = new CompensateFo();
        fo.setInstanceId(instanceId);
        List<String> sourceList = Arrays.stream(taskEntity.getCurrentNodeCode().split(",")).collect(Collectors.toList());
        fo.setSource(sourceList);
        List<FlowableTaskModel> models = flowAbleUrl.compensate(fo);

        if (CollUtil.isNotEmpty(models)) {
            FlowableTaskModel flowableTaskModel = models.get(0);
            // 实例ID不一样，更新
            String newInstanceId = flowableTaskModel.getInstanceId();
            if (StringUtils.isNotBlank(newInstanceId) && !instanceId.equals(newInstanceId)) {
                taskEntity.setInstanceId(newInstanceId);
                taskMapper.updateById(taskEntity);
            }
            List<NodeRecordEntity> nodeRecords = nodeRecordMapper.getNodeRecord(taskEntity.getId());
            List<String> nodeIds = nodeRecords.stream().map(NodeRecordEntity::getNodeId).filter(StringUtils::isNotBlank).collect(Collectors.toList());
            // 更新经办关联的flowable的任务id
            List<OperatorEntity> operatorList = operatorMapper.getList(taskEntity.getId());
            List<OperatorEntity> updateList = new ArrayList<>();
            // 更新记录关联的flowable的任务id
            List<RecordEntity> recordList = recordMapper.getList(taskEntity.getId());
            List<RecordEntity> updateRecordList = new ArrayList<>();
            //外部节点
            List<EventLogEntity> eventLogList = eventLogMapper.getList(taskEntity.getId());
            List<EventLogEntity> updateEventLogList = new ArrayList<>();

            if (CollUtil.isNotEmpty(nodeIds)) {
                // 在节点记录中的存在的nodeId，不需要更新
                operatorList = operatorList.stream().filter(e -> !nodeIds.contains(e.getNodeId())).collect(Collectors.toList());
                recordList = recordList.stream().filter(e -> !nodeIds.contains(e.getNodeId())).collect(Collectors.toList());
            }

            for (FlowableTaskModel model : models) {
                List<OperatorEntity> list = operatorList.stream()
                        .filter(e -> e.getNodeCode().equals(model.getTaskKey())).collect(Collectors.toList());
                list.forEach(e -> e.setNodeId(model.getTaskId()));
                updateList.addAll(list);
                // 记录修改
                List<RecordEntity> collect = recordList.stream()
                        .filter(e -> e.getNodeCode().equals(model.getTaskKey())).collect(Collectors.toList());
                collect.forEach(e -> e.setNodeId(model.getTaskId()));
                updateRecordList.addAll(collect);
                //外部节点
                List<EventLogEntity> logList = eventLogList.stream()
                        .filter(e -> e.getNodeCode().equals(model.getTaskKey())).collect(Collectors.toList());
                logList.forEach(e -> e.setNodeId(model.getTaskId()));
                updateEventLogList.addAll(logList);
            }
            operatorMapper.updateById(updateList);
            recordMapper.updateRecords(updateRecordList);
            eventLogMapper.updateById(updateEventLogList);
        }
        if (!ObjectUtil.equals(taskEntity.getParentId(), FlowNature.PARENT_ID)) {
            TaskEntity parent = taskMapper.selectById(taskEntity.getParentId());
            if (null == parent) {
                return;
            }
            this.compensate(parent);
            List<FlowableTaskModel> currentModel = flowAbleUrl.getCurrentTask(parent.getInstanceId());
            FlowableTaskModel model = currentModel.stream().filter(e -> ObjectUtil.equals(taskEntity.getSubCode(), e.getTaskKey())).findFirst().orElse(null);
            if (null != model) {
                SubParameterModel subParameter = JsonUtil.getJsonToBean(taskEntity.getSubParameter(), SubParameterModel.class);
                subParameter.setNodeId(model.getTaskId());
                taskEntity.setSubParameter(JsonUtil.getObjectToString(subParameter));
                taskMapper.updateById(taskEntity);
            }
        }
    }

    /**
     * 校验经办的任务是否暂停
     *
     * @param ids     经办主键集合
     * @param resList 结果经办主键集合
     * @param flag    true则判断退签
     */
    public void checkBatch(List<String> ids, List<String> resList, Boolean flag) throws WorkFlowException {
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(OperatorEntity::getId, ids);
        List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
        List<String> taskIds = list.stream().map(OperatorEntity::getTaskId).collect(Collectors.toList());
        List<TaskEntity> taskEntityList = taskIds.isEmpty() ? new ArrayList<>() : taskMapper.selectByIds(taskIds);

        for (OperatorEntity operator : list) {
            TaskEntity taskEntity = taskEntityList.stream()
                    .filter(e -> !Objects.equals(e.getStatus(), TaskStatusEnum.PAUSED.getCode())
                            && e.getId().equals(operator.getTaskId())).findFirst().orElse(null);
            if (null != taskEntity) {
                resList.add(operator.getId());
            }
        }
        if (CollUtil.isEmpty(resList)) {
            throw new WorkFlowException(MsgCode.WF112.get());
        }

        if (Boolean.TRUE.equals(flag)) {
            resList.clear();
            List<String> flowIds = taskEntityList.stream().map(TaskEntity::getFlowId).collect(Collectors.toList());
            List<TemplateNodeEntity> globalList = templateNodeMapper.getList(flowIds, NodeEnum.GLOBAL.getType());
            for (OperatorEntity operator : list) {
                TaskEntity taskEntity = taskEntityList.stream()
                        .filter(e -> e.getId().equals(operator.getTaskId())).findFirst().orElse(null);
                if (null != taskEntity) {
                    TemplateNodeEntity globalEntity = globalList.stream()
                            .filter(e -> e.getFlowId().equals(taskEntity.getFlowId())).findFirst().orElse(null);
                    if (null != globalEntity) {
                        NodeModel global = JsonUtil.getJsonToBean(globalEntity.getNodeJson(), NodeModel.class);
                        // 开启签收，允许退签
                        if (Boolean.TRUE.equals(global.getHasSignFor())) {
                            resList.add(operator.getId());
                        }
                    }
                }
            }
            if (CollUtil.isEmpty(resList)) {
                throw new WorkFlowException(MsgCode.WF080.get());
            }
        }
    }

    public void checkBatchRevoke(List<String> ids, List<String> resList, Integer batchType) throws WorkFlowException {
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(OperatorEntity::getId, ids);
        List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
        List<Integer> status = ImmutableList.of(2, 3);
        for (OperatorEntity operator : list) {
            if (status.contains(batchType) && ObjectUtil.equals(operator.getStatus(), OperatorStateEnum.REVOKE.getCode())) {
                continue;
            }
            resList.add(operator.getId());
        }
        if (CollUtil.isEmpty(resList)) {
            if (ObjectUtil.equals(batchType, 2)) {
                throw new WorkFlowException(MsgCode.WF126.get());
            } else if (ObjectUtil.equals(batchType, 3)) {
                throw new WorkFlowException(MsgCode.WF127.get());
            }
        }
    }

    // 判断终止
    public void checkCancel(List<String> ids, List<String> resList) throws WorkFlowException {
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(OperatorEntity::getId, ids);
        List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
        List<String> taskIds = list.stream().map(OperatorEntity::getTaskId).collect(Collectors.toList());
        List<TaskEntity> taskEntityList = taskIds.isEmpty() ? new ArrayList<>() : taskMapper.selectByIds(taskIds);

        for (OperatorEntity operator : list) {
            TaskEntity taskEntity = taskEntityList.stream()
                    .filter(e -> !Objects.equals(e.getStatus(), TaskStatusEnum.CANCEL.getCode())
                            && e.getId().equals(operator.getTaskId())).findFirst().orElse(null);
            if (null != taskEntity) {
                resList.add(operator.getId());
            }
        }
        if (CollUtil.isEmpty(resList)) {
            throw new WorkFlowException(MsgCode.WF123.get());
        }
    }

    // 更新经办的接收时间
    public void updateCreateTime(FlowModel flowModel) {
        TaskEntity taskEntity = flowModel.getTaskEntity();
        String taskId = taskEntity.getId();
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskId)
                .isNull(OperatorEntity::getHandleStatus);
        List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            list.forEach(e -> e.setCreatorTime(new Date()));
            operatorMapper.updateById(list);
            addOperatorList(list, flowModel);
        }
    }

    /**
     * 添加事件到线程变量
     */
    public void addEvent(FlowModel flowModel) {
        Integer status = flowModel.getEventStatus();
        FlowModel model = new FlowModel();
        model.setTaskEntity(flowModel.getTaskEntity());
        model.setNodeEntity(flowModel.getNodeEntity());
        model.setNodes(flowModel.getNodes());
        model.setFormData(flowModel.getFormData());
        if (ObjectUtil.equals(EventEnum.END.getStatus(), status)) {
            List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
            TemplateNodeEntity start = nodeEntityList.stream()
                    .filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.START.getType())).findFirst().orElse(null);
            if (null != start) {
                model.setNodeEntity(start);
            }
        }
        FlowEventHolder.addEvent(status, model, FlowContextHolder.getAllData());
    }

    /**
     * 处理线程变量中的超时
     */
    public void handleOperator() {
        List<FlowOperatorModel> operatorListAll = FlowOperatorHolder.getOperatorList();
        for (FlowOperatorModel model : operatorListAll) {
            List<OperatorEntity> list = model.getList() != null ? model.getList() : new ArrayList<>();
            Map<String, Map<String, Object>> allData = model.getAllData();
            Map<String, List<OperatorEntity>> taskMap = list.stream().collect(Collectors.groupingBy(OperatorEntity::getTaskId));
            for (Map.Entry<String, List<OperatorEntity>> stringListEntry : taskMap.entrySet()) {
                String taskId = stringListEntry.getKey();

                List<OperatorEntity> operatorList = taskMap.get(taskId) != null ? taskMap.get(taskId) : new ArrayList<>();
                Map<String, List<OperatorEntity>> operatorMap = operatorList.stream().collect(Collectors.groupingBy(OperatorEntity::getNodeCode));
                for (Map.Entry<String, List<OperatorEntity>> entry : operatorMap.entrySet()) {
                    String nodeCode = entry.getKey();

                    List<OperatorEntity> operator = operatorMap.get(nodeCode) != null ? operatorMap.get(nodeCode) : new ArrayList<>();
                    if (operator.isEmpty()) {
                        continue;
                    }
                    FlowModel flowModel = JsonUtil.getJsonToBean(model.getFlowModel(), FlowModel.class);
                    Map<String, NodeModel> nodes = flowModel.getNodes();
                    NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
                    NodeModel nodeModel = nodes.get(nodeCode);
                    String modelFormId = nodeModel.getFormId();
                    modelFormId = StringUtils.isNotBlank(modelFormId) ? modelFormId : global.getFormId();
                    Map<String, Object> formData = allData.get(taskId + JnpfConst.SIDE_MARK + modelFormId);
                    if (formData != null) {
                        flowModel.setFormData(formData);
                    }
                    TimeUtil.timeModel(operator, flowModel, redisUtil);

                }


            }


        }
        FlowOperatorHolder.clear();
    }

    /**
     * 添加超时到线程变量
     */
    public void addOperatorList(List<OperatorEntity> list, FlowModel flowModel) {
        if (!list.isEmpty()) {
            FlowOperatorModel model = new FlowOperatorModel();
            model.setList(JsonUtil.getJsonToList(list, OperatorEntity.class));
            model.setFlowModel(JsonUtil.getJsonToBean(flowModel, FlowModel.class));
            FlowOperatorHolder.addOperator(model, FlowContextHolder.getAllData());
        }
    }

    //保存流程id
    public void addTask(List<String> taskIdList) {
        FlowStatusHolder.addTaskIdList(taskIdList);
    }

    //删除流程id
    public void addDelTask(String taskId, String flowId) {
        FlowStatusHolder.addDelTaskIdList(taskId, flowId);
    }

    public List<OperatorEntity> getList(String taskId, List<String> nodeCodes) {
        if (StringUtils.isEmpty(taskId)) {
            return new ArrayList<>();
        }
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskId).ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode());
        if (CollUtil.isNotEmpty(nodeCodes)) {
            queryWrapper.lambda().in(OperatorEntity::getNodeCode, nodeCodes);
        }
        queryWrapper.lambda().orderByDesc(OperatorEntity::getCreatorTime);
        return operatorMapper.selectList(queryWrapper);
    }

    public void deleteInTurnOperator(OperatorEntity operator, String handleId) {
        List<String> handleAll = new ArrayList<>();
        if (operator.getHandleAll() != null) {
            handleAll = Arrays.stream(operator.getHandleAll().split(",")).collect(Collectors.toList());
        } else {
            String handleParameter = operator.getHandleParameter();
            AddSignModel addSignModel = JsonUtil.getJsonToBean(handleParameter, AddSignModel.class);
            if (ObjectUtil.equals(addSignModel.getCounterSign(), FlowNature.IMPROPER_APPROVER)) {
                handleAll = addSignModel.getAddSignUserIdList();
            }
        }
        int index = handleAll.indexOf(handleId);
        if (index != -1 && index < handleAll.size() - 1) {
            String nextHandleId = handleAll.get(index + 1);
            QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(OperatorEntity::getNodeId, operator.getNodeId())
                    .eq(OperatorEntity::getHandleId, nextHandleId);
            List<OperatorEntity> deleteList = operatorMapper.selectList(queryWrapper);
            if (CollUtil.isNotEmpty(deleteList)) {
                operatorMapper.deleteByIds(deleteList);
            }
        }
    }

    public void deleteStepOperator(OperatorEntity operator) {
        if (operator != null) {
            QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId());
            List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
            List<OperatorEntity> deleteList = list.stream().filter(e -> Objects.equals(e.getHandleAll(), operator.getId())).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(deleteList)) {
                operatorMapper.deleteByIds(deleteList);
            }
        }
    }

    // 处理嵌入的任务流程
    public void handleTrigger(OperatorEntity operatorEntity, FlowModel flowModel) throws WorkFlowException {
        String nodeCode = operatorEntity.getNodeCode();
        Map<String, NodeModel> nodes = flowModel.getNodes();
        List<String> nextList = new ArrayList<>();
        nextList = getNextList(flowModel.getDeploymentId(), nodeCode, nodes, nextList);
        if (CollUtil.isEmpty(nextList)) {
            return;
        }

        Integer action = flowModel.getAction();
        TaskEntity taskEntity = flowModel.getTaskEntity();
        String flowId = taskEntity.getFlowId();
        Map<String, Object> formData = flowModel.getFormData();
        List<SystemAuditModel> systemList = SystemAuditHolder.getAll();

        // 存在同步
        boolean isAsync = false;

        List<String> approveNode = new ArrayList<>();
        Map<String, NodeModel> triggers = new HashMap<>();
        for (String next : nextList) {
            NodeModel nodeModel = nodes.get(next);
            if (null != nodeModel) {
                if (ObjectUtil.equals(nodeModel.getType(), NodeEnum.TRIGGER.getType())) {
                    if (ObjectUtil.equals(nodeModel.getIsAsync(), FlowNature.CHILD_SYNC)) {
                        isAsync = true;
                    }
                    triggers.put(next, nodeModel);
                } else {
                    approveNode.add(nodeModel.getNodeId());
                }
            }
        }

        if (CollUtil.isEmpty(triggers)) {
            return;
        }
        //清空系统审批
        SystemAuditHolder.clear();
        Boolean rejectTrigger = flowModel.getRejectTrigger();
        boolean isBack = ObjectUtil.equals(action, ActionEnum.BACK.getCode());
        if (isBack || Boolean.TRUE.equals(rejectTrigger)) {
            OutgoingFlowsFo flowsFo = new OutgoingFlowsFo();
            flowsFo.setDeploymentId(flowModel.getDeploymentId());
            flowsFo.setTaskKey(nodeCode);
            List<String> outgoingFlows = flowAbleUrl.getOutgoingFlows(flowsFo);
            Map<String, Boolean> variables = new HashMap<>();
            for (String line : outgoingFlows) {
                variables.put(line, true);
            }
            if (isBack) {
                List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
                TemplateNodeEntity end = nodeEntityList.stream()
                        .filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.END.getType())).findFirst().orElse(null);
                if (null != end) {
                    // 将目标是结束节点的线 的值变更为false
                    FlowableNodeModel info = flowAbleUrl.getElementInfo(flowModel.getDeploymentId(), end.getNodeCode());
                    List<String> incomingList = info.getIncomingList();
                    if (CollUtil.isNotEmpty(incomingList)) {
                        for (String key : variables.keySet()) {
                            if (incomingList.contains(key)) {
                                variables.put(key, false);
                            }
                        }
                    }
                }
            }
            taskLineMapper.create(taskEntity.getId(), variables);
            complete(operatorEntity.getNodeId(), new HashMap<>(variables));
        } else {
            // 异步处理
            if (!isAsync && CollUtil.isNotEmpty(systemList)) {
                for (SystemAuditModel model : systemList) {
                    try {
                        this.autoAudit(model.getOperator(), model.getFlowModel());
                    } catch (Exception e) {
                        e.getMessage();
                    }
                }
                systemList = new ArrayList<>();
            }
        }

        UserInfo userInfo = UserProvider.getUser();
        List<ExecuteModel> list = new ArrayList<>();
        for (String key : triggers.keySet()) {
            NodeModel nodeModel = nodes.get(key);
            if (null == nodeModel || (ObjectUtil.equals(nodeModel.getTriggerEvent(), 2)
                    && !nodeModel.getActionList().contains(action))) {
                continue;
            }

            List<OperatorEntity> operatorList = new ArrayList<>();
            List<TaskEntity> subTaskList = new ArrayList<>();
            Boolean sync = ObjectUtil.equals(nodeModel.getIsAsync(), FlowNature.CHILD_SYNC);
            if (Boolean.TRUE.equals(sync) && CollUtil.isNotEmpty(approveNode)) {
                // 子流程的处理
                List<TaskEntity> childTaskList = new ArrayList<>();
                childTaskList.addAll(taskMapper.getSubTask(taskEntity.getId(), approveNode));
                if (CollUtil.isNotEmpty(childTaskList)) {
                    List<String> idAll = new ArrayList<>();
                    taskMapper.deleTaskAll(childTaskList.stream().map(TaskEntity::getId).collect(Collectors.toList()), idAll);
                    if (!idAll.isEmpty()) {
                        QueryWrapper<TaskEntity> queryWrapper = new QueryWrapper<>();
                        queryWrapper.lambda().in(TaskEntity::getId, idAll);
                        subTaskList.addAll(taskMapper.selectList(queryWrapper));
                        for (TaskEntity subTask : subTaskList) {
                            Integer status = subTask.getStatus();
                            subTask.setHisStatus(status);
                            subTask.setStatus(TaskStatusEnum.WAITING.getCode());
                            operatorList.addAll(operatorMapper.getList(subTask.getId()));
                        }
                        taskMapper.updateById(subTaskList);
                    }
                }
                //审批数据,排除系统用户
                operatorList.addAll(this.getList(taskEntity.getId(), approveNode).stream().filter(e -> !Objects.equals(FlowNature.SYSTEM_CODE, e.getHandleId())).collect(Collectors.toList()));
                if (CollUtil.isNotEmpty(operatorList)) {
                    for (OperatorEntity operator : operatorList) {
                        operator.setStatus(OperatorStateEnum.WAITING.getCode());
                    }
                    operatorMapper.updateById(operatorList);
                }
            }
            List<Map<String, Object>> dataList = new ArrayList<>();
            dataList.add(formData);
            FlowModel model = JsonUtil.getJsonToBean(flowModel, FlowModel.class);
            model.setUserInfo(userInfo);
            ExecuteModel executeModel = new ExecuteModel();
            executeModel.setFlowId(flowId);
            executeModel.setDataList(dataList);
            executeModel.setTaskId(taskEntity.getId());
            executeModel.setInstanceId(taskEntity.getInstanceId());
            executeModel.setGroupId(nodeModel.getGroupId());
            executeModel.setNodeCode(nodeCode);
            executeModel.setNodeId(operatorEntity.getNodeId());
            executeModel.setOperatorId(operatorEntity.getId());
            executeModel.setTriggerKey(key);
            executeModel.setIsAsync(nodeModel.getIsAsync());
            executeModel.setFlowModel(model);
            executeModel.setSync(sync);
            if (isBack || Boolean.TRUE.equals(rejectTrigger)) {
                try {
                    operatorExecute(executeModel);
                } catch (Exception e) {
                    e.getMessage();
                }
                flowModel.setBackId(executeModel.getCurrentNodeId());
            } else {
                if (Boolean.TRUE.equals(sync)) {
                    executeModel.setOperatorList(operatorList);
                    executeModel.setSubTaskList(subTaskList);
                    executeModel.setSystemList(systemList);
                }
                list.add(executeModel);
            }
        }
        List<Boolean> resultList = new ArrayList<>();
        try {
            for (ExecuteModel model : list) {
                boolean async = this.operatorExecute(model);
                if (!async) {
                    resultList.add(async);
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
        if (resultList.isEmpty()) {
            List<TriggerLaunchflowEntity> launchFlowList = getLaunchFlowList(taskEntity.getId());
            if (launchFlowList.isEmpty()) {
                executeAudit(list, flowModel);
            }
        }
    }

    public List<TriggerLaunchflowEntity> getLaunchFlowList(String taskId) {
        List<TriggerTaskEntity> triggerList = triggerTaskMapper.getListByTaskId(taskId, null);
        List<String> triggerIdList = triggerList.stream().map(TriggerTaskEntity::getId).collect(Collectors.toList());
        return triggerLaunchflowMapper.getTriggerList(triggerIdList);
    }

    // 通过taskId
    public List<TriggerLaunchflowEntity> getTaskList(String taskId) {
        return getTaskList(taskId, null);
    }

    // 通过taskId
    public List<TriggerLaunchflowEntity> getTaskList(String taskId, List<String> nodeCode) {
        return triggerLaunchflowMapper.getTaskList(ImmutableList.of(taskId), nodeCode);
    }

    public boolean operatorExecute(ExecuteModel model) throws WorkFlowException {
        boolean result = true;
        Boolean isAsync = model.getSync();
        FlowModel flowModel = model.getFlowModel();
        try {
            execute(model);
        } catch (Exception e) {
            e.printStackTrace();
            // 终止流程
            if (Boolean.TRUE.equals(isAsync)) {
                TaskEntity taskEntity = flowModel.getTaskEntity();
                cancel(taskEntity.getId(), flowModel, true);
                result = false;
            }
        }
        return result;
    }

    public void executeAudit(List<ExecuteModel> list, FlowModel flowModel) {
        for (ExecuteModel model : list) {
            List<OperatorEntity> operatorList = model.getOperatorList();
            if (CollUtil.isNotEmpty(operatorList)) {
                for (OperatorEntity operator : operatorList) {
                    operator.setStatus(OperatorStateEnum.RUNING.getCode());
                }
                operatorMapper.updateById(operatorList);
            }
            List<TaskEntity> subTaskList = model.getSubTaskList();
            if (CollUtil.isNotEmpty(subTaskList)) {
                for (TaskEntity taskEntity : subTaskList) {
                    Integer hisStatus = taskEntity.getHisStatus();
                    taskEntity.setStatus(hisStatus);
                }
                taskMapper.updateById(subTaskList);
            }
            List<SystemAuditModel> systemList = model.getSystemList();
            if (CollUtil.isNotEmpty(systemList)) {
                for (SystemAuditModel systemAuditModel : systemList) {
                    try {
                        autoAudit(systemAuditModel.getOperator(), systemAuditModel.getFlowModel());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                autoAudit(flowModel, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //更改状态
    public void handleTaskStatus() {
        Map<String, String> delTaskMap = FlowStatusHolder.getDelTaskMap();
        for (Map.Entry<String, String> stringStringEntry : delTaskMap.entrySet()) {
            String taskId = stringStringEntry.getKey();
            String flowId = delTaskMap.get(taskId);
            if (StringUtils.isEmpty(flowId)) {
                continue;
            }
            List<String> flowIdList = ImmutableList.of(flowId);
            List<TemplateNodeEntity> list = templateNodeMapper.getList(flowIdList, null);
            Set<String> formIds = list.stream().map(TemplateNodeEntity::getFormId).collect(Collectors.toSet());
            FlowStateModel model = new FlowStateModel(new ArrayList<>(formIds), taskId, 0);
            serviceUtil.saveState(model);

        }


        List<String> taskIdList = FlowStatusHolder.getTaskList();
        if (!taskIdList.isEmpty()) {
            List<TaskEntity> taskList = taskMapper.getInfosSubmit(taskIdList.toArray(new String[]{}), TaskEntity::getId, TaskEntity::getStatus, TaskEntity::getFlowId);
            List<String> flowIdList = taskList.stream().map(TaskEntity::getFlowId).collect(Collectors.toList());
            List<TemplateNodeEntity> list = templateNodeMapper.getList(flowIdList, null);
            for (TaskEntity task : taskList) {
                String id = task.getId();
                String flowId = task.getFlowId();
                Integer status = task.getStatus();
                Set<String> formIds = list.stream().filter(e -> e.getFlowId().equals(flowId)).map(TemplateNodeEntity::getFormId).collect(Collectors.toSet());
                FlowStateModel model = new FlowStateModel(new ArrayList<>(formIds), id, status);
                serviceUtil.saveState(model);
            }
        }
        FlowStatusHolder.clear();
    }

    /**
     * 处理线程变量中的事件
     */
    public void handleEvent() {
        List<EventModel> allEvent = FlowEventHolder.getAllEvent();
        for (EventModel eventModel : allEvent) {
            String type = eventModel.getType();
            if (Objects.equals(NodeEnum.APPROVER.getType(), type)) {
                flowUtil.event(eventModel.getFlowModel(), eventModel.getStatus(), eventModel.getAllData());
            } else if (Objects.equals(NodeEnum.OUTSIDE.getType(), type)) {
                String interId = eventModel.getInterfaceId();
                Map<String, String> parameterMap = eventModel.getParameterData();
                String id = eventModel.getId();
                ActionResult<Object> result = serviceUtil.infoToId(interId, parameterMap);
                EventLogEntity eventLog = eventLogMapper.selectById(id);
                if (eventLog != null) {
                    eventLog.setResult(JsonUtil.getObjectToString(result));
                    boolean retryResult = Objects.equals(200, result.getCode());
                    eventLog.setStatus(retryResult ? FlowNature.SUCCESS : FlowNature.LOSE);
                    eventLogMapper.updateById(eventLog);
                }
            }
        }
        FlowEventHolder.clear();
    }

    /**
     * 外部节点参数
     */
    public Map<String, String> outsideData(FlowModel flowModel, List<TemplateJsonModel> templateJsonModelList, Map<String, Map<String, Object>> allData, String resultNodeCode, String eventId) {
        TaskEntity taskEntity = flowModel.getTaskEntity();
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        TemplateNodeEntity startNode = nodeEntityList.stream()
                .filter(e -> Objects.equals(NodeEnum.START.getType(), e.getNodeType())).findFirst().orElse(new TemplateNodeEntity());
        String startNodeFormId = startNode.getFormId();
        String taskId = taskEntity.getId();
        Map<String, Object> formData = flowModel.getFormData();
        formData.put(FlowConstant.START_NODE_FORM_ID, startNodeFormId);
        TemplateNodeEntity prevNode = nodeEntityList.stream()
                .filter(e -> Objects.equals(resultNodeCode, e.getNodeCode())).findFirst().orElse(new TemplateNodeEntity());
        formData.put(FlowConstant.PREV_NODE_FORM_ID, prevNode.getFormId());
        String nodeCode = flowModel.getNodeCode();
        RecordEntity recordEntity = new RecordEntity();
        recordEntity.setNodeCode(nodeCode);
        recordEntity.setNodeId(eventId);
        Map<String, Object> parameterData = new HashMap<>();
        for (TemplateJsonModel templateJsonModel : templateJsonModelList) {
            if (Objects.equals(FieldEnum.FIELD.getCode(), templateJsonModel.getSourceType())) {
                String relationField = templateJsonModel.getRelationField();
                String[] split = relationField.split("\\|");
                if (split.length > 1) {
                    String formId = split[split.length - 1];
                    String field = split[0];
                    Map<String, Object> dataMap = allData.get(taskId + JnpfConst.SIDE_MARK + formId) != null ? allData.get(taskId + JnpfConst.SIDE_MARK + formId) : serviceUtil.infoData(formId, taskId);
                    parameterData.put(relationField, dataMap.get(field));
                } else {
                    String field = split[split.length - 1];
                    parameterData.put(relationField, formData.get(field));
                }
            }
        }
        Map<String, Object> dataMap = new HashMap<>(formData);
        dataMap.putAll(parameterData);
        FlowModel parameterModel = new FlowModel();
        parameterModel.setFormData(dataMap);
        parameterModel.setRecordEntity(recordEntity);
        parameterModel.setTaskEntity(taskEntity);
        return flowUtil.parameterMap(parameterModel, templateJsonModelList);
    }

    /**
     * 处理同步的触发流程
     */
    public void launchTrigger(FlowModel flowModel) throws WorkFlowException {
        TaskEntity taskEntity = flowModel.getTaskEntity();
        if (taskEntity == null) {
            return;
        }
        List<TriggerLaunchflowEntity> triggerList = triggerLaunchflowMapper.getTaskIds(taskEntity.getId());
        if (CollUtil.isEmpty(triggerList)) {
            return;
        }
        for (TriggerLaunchflowEntity entity : triggerList) {
            List<String> taskIdList = JsonUtil.getJsonToList(entity.getTaskIds(), String.class);
            List<TaskEntity> launchTaskList = taskMapper.getInfosSubmit(taskIdList.toArray(new String[]{}), TaskEntity::getId, TaskEntity::getEndTime, TaskEntity::getStatus);
            boolean isFlowEnd = launchTaskList.stream().noneMatch(e -> e.getEndTime() == null);
            if (isFlowEnd) {
                ExtraData extraData = JsonUtil.getJsonToBean(entity.getExtraData(), ExtraData.class);
                ExecuteModel model = getExecuteModel(extraData);
                if (model != null) {
                    triggerLaunchflowMapper.delete(entity);
                    TaskEntity launchTaskEntity = model.getTaskEntity();
                    if (null != launchTaskEntity && ObjectUtil.equals(launchTaskEntity.getStatus(), TaskStatusEnum.CANCEL.getCode())) {
                        return;
                    }
                    FlowModel launchFlowModel = model.getFlowModel();
                    model.setNodeRetry(true);
                    boolean result = true;
                    try {
                        result = this.operatorExecute(model);
                    } catch (Exception e) {
                        log.info(e.getMessage());
                    }
                    if (!result) {
                        return;
                    }
                    if (Objects.equals(FlowNature.CHILD_SYNC, model.getIsAsync())) {
                        List<TriggerRecordEntity> triggerRecordList = triggerRecordMapper.getList(entity.getTriggerId());
                        List<String> nodeCode = triggerRecordList.stream().map(TriggerRecordEntity::getNodeCode).collect(Collectors.toList());
                        boolean isExecuteAudit = getTaskList(entity.getTaskId(), nodeCode).isEmpty();
                        if (isExecuteAudit) {
                            List<String> operatorIdList = extraData.getOperatorIdList();
                            if (CollUtil.isNotEmpty(operatorIdList)) {
                                QueryWrapper<OperatorEntity> operatorWrapper = new QueryWrapper<>();
                                operatorWrapper.lambda().in(OperatorEntity::getId, operatorIdList);
                                List<OperatorEntity> operatorList = new ArrayList<>();
                                List<OperatorEntity> list = operatorMapper.selectList(operatorWrapper);
                                for (OperatorEntity operator : list) {
                                    if (Objects.equals(operator.getStatus(), OperatorStateEnum.WAITING.getCode())) {
                                        operatorList.add(operator);
                                    }
                                }
                                model.setOperatorList(operatorList);
                            }
                            List<String> subTaskIdList = extraData.getSubTaskIdList();
                            if (CollUtil.isNotEmpty(subTaskIdList)) {
                                QueryWrapper<TaskEntity> taskWrapper = new QueryWrapper<>();
                                taskWrapper.lambda().in(TaskEntity::getId, subTaskIdList);
                                List<TaskEntity> taskList = new ArrayList<>();
                                List<TaskEntity> list = taskMapper.selectList(taskWrapper);
                                taskList.addAll(list);
                                model.setSubTaskList(taskList);
                            }
                            Map<String, FlowModel> systemMap = extraData.getSystemMap();
                            if (CollUtil.isNotEmpty(systemMap)) {
                                List<SystemAuditModel> systemList = new ArrayList<>();
                                for (Map.Entry<String, FlowModel> stringFlowModelEntry : systemMap.entrySet()) {
                                    String operatorId = stringFlowModelEntry.getKey();
                                    OperatorEntity operator = operatorMapper.selectById(operatorId);
                                    FlowModel systemFlowModel = systemMap.get(operatorId);
                                    if (operator != null && systemFlowModel != null && Objects.equals(operator.getStatus(), OperatorStateEnum.RUNING.getCode())) {
                                        SystemAuditModel systemAuditModel = new SystemAuditModel();
                                        flowUtil.setFlowModel(operator.getTaskId(), systemFlowModel);
                                        systemAuditModel.setFlowModel(systemFlowModel);
                                        systemAuditModel.setOperator(operator);
                                        systemList.add(systemAuditModel);
                                    }
                                }
                                model.setSystemList(systemList);
                            }

                            //同意、拒绝
                            Boolean rejectTrigger = launchFlowModel.getRejectTrigger();
                            Integer action = launchFlowModel.getAction();
                            launchFlowModel.setTriggerBack(true);
                            List<Integer> actionList = ImmutableList.of(ActionEnum.AUDIT.getCode(), ActionEnum.REJECT.getCode(), ActionEnum.PROCESSING.getCode());
                            if (actionList.contains(action)) {
                                if (Boolean.TRUE.equals(rejectTrigger)) {
                                    launchFlowModel.setHandleStatus(FlowNature.REJECT_COMPLETION);
                                    launchFlowModel.setFinishFlag(false);
                                } else {
                                    executeAudit(ImmutableList.of(model), launchFlowModel);
                                }
                                TaskEntity task = launchFlowModel.getTaskEntity();
                                if (task != null && task.getEndTime() == null) {
                                    isFinished(launchFlowModel);
                                }

                            }

                            //退回
                            if (Objects.equals(ActionEnum.BACK.getCode(), action)) {
                                String operatorId = model.getOperatorId();
                                back(operatorId, launchFlowModel);
                            }
                            handleOperator();
                            handleTaskStatus();
                            handleEvent();
                        }
                    }
                }
            }
        }
    }

    //-------------------------------taskUtil------------------------------------------------------------

    public void cancel(String id, FlowModel flowModel, boolean isCancel) throws WorkFlowException {
        if (isCancel) {
            // 任务流程的终止
            TriggerTaskEntity triggerTask = triggerTaskMapper.selectById(id);
            if (null != triggerTask) {
                triggerTask.setStatus(TaskStatusEnum.CANCEL.getCode());
                triggerTaskMapper.updateById(triggerTask);
                return;
            }
        }
        flowUtil.setFlowModel(id, flowModel);
        TaskEntity taskEntity = flowModel.getTaskEntity();
        flowUtil.isSuspend(taskEntity);

        // 下架的判断
        if (!isCancel) {
            TemplateEntity template = templateMapper.selectById(taskEntity.getTemplateId());
            if (null != template && !ObjectUtil.equals(template.getStatus(), TemplateStatueEnum.UP.getCode())) {
                throw new WorkFlowException(MsgCode.WF140.get());
            }

        }

        List<String> idList = ImmutableList.of(id);

        List<TaskEntity> taskList = new ArrayList<>();
        // 递归获取子流程
        for (String taskId : idList) {
            List<String> childAllList = taskMapper.getChildAllList(taskId);
            taskList.addAll(taskMapper.getOrderStaList(childAllList));
        }

        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();

        for (TaskEntity entity : taskList) {
            flowUtil.isSuspend(entity);
            // 记录
            FlowMethod flowMethod = new FlowMethod();
            flowMethod.setFlowModel(flowModel);
            Integer type = isCancel ? RecordEnum.CANCEL.getCode() : RecordEnum.ACTIVATE.getCode();
            flowMethod.setType(type);
            OperatorEntity operatorEntity = new OperatorEntity();
            String code = StringUtils.isNotEmpty(entity.getCurrentNodeCode()) ? entity.getCurrentNodeCode() : FlowNature.START_CODE;
            operatorEntity.setNodeCode(code);
            String name = StringUtils.isNotEmpty(entity.getCurrentNodeName()) ? entity.getCurrentNodeName() : FlowNature.START_NAME;
            operatorEntity.setNodeName(name);
            operatorEntity.setTaskId(entity.getId());
            operatorEntity.setHandleId(UserProvider.getLoginUserId());
            flowMethod.setOperatorEntity(operatorEntity);
            recordMapper.createRecord(flowMethod);

            if (isCancel) {
                // 终止，更新实例
                entity.setHisStatus(entity.getStatus());
                entity.setStatus(TaskStatusEnum.CANCEL.getCode());
                taskMapper.updateById(entity);

                // 消息
                FlowMsgModel flowMsgModel = new FlowMsgModel();
                flowMsgModel.setNodeList(nodeEntityList);
                flowMsgModel.setUserInfo(flowModel.getUserInfo());
                flowMsgModel.setTaskEntity(entity);
                flowMsgModel.setEnd(true);
                msgUtil.message(flowMsgModel);
            } else {
                // 复活
                entity.setStatus(entity.getHisStatus());
                taskMapper.updateById(entity);
                updateCreateTime(flowModel);
            }
            addTask(ImmutableList.of(entity.getId()));
        }
    }


    // 提交撤销流程
    public void submitOfRevoke(FlowModel flowModel) throws WorkFlowException {
        TaskEntity task = flowModel.getTaskEntity();
        List<TaskLineEntity> lineList = taskLineMapper.getList(task.getId());
        if (CollUtil.isEmpty(lineList)) {
            throw new WorkFlowException("无法撤销");
        }
        // 委托人的处理
        UserInfo userInfo = flowModel.getUserInfo();
        String creatorUserId = task.getCreatorUserId();
        String delegateUserId = task.getDelegateUserId();
        boolean isDelegate = !ObjectUtil.equals(userInfo.getUserId(), creatorUserId);
        if (isDelegate && StringUtils.isNotEmpty(delegateUserId)) {
            String token = AuthUtil.loginTempUser(task.getCreatorUserId(), UserProvider.getUser().getTenantId());
            userInfo = UserProvider.getUser(token);
            flowModel.setUserInfo(userInfo);
        }


        Map<String, Object> variables = new HashMap<>();
        for (TaskLineEntity condition : lineList) {
            variables.put(condition.getLineKey(), Boolean.valueOf(condition.getLineValue()));
        }

        TaskEntity entity = new TaskEntity();
        entity.setId(RandomUtil.uuId());
        entity.setType(1);
        entity.setUrgent(task.getUrgent());
        entity.setCreatorUserId(task.getCreatorUserId());
        entity.setStartTime(new Date());
        entity.setCreatorTime(new Date());
        entity.setFullName(task.getFullName() + "的撤销申请");
        entity.setFlowCategory(task.getFlowCategory());
        entity.setFlowCode(task.getFlowCode());
        entity.setFlowName(task.getFlowName());
        entity.setFlowVersion(task.getFlowVersion());
        entity.setFlowId(task.getFlowId());
        entity.setTemplateId(task.getTemplateId());
        entity.setParentId(task.getParentId());
        entity.setFlowType(task.getFlowType());
        entity.setStatus(TaskStatusEnum.RUNNING.getCode());
        entity.setEngineType(task.getEngineType());
        if (isDelegate) {
            entity.setDelegateUserId(delegateUserId);
        }

        TemplateJsonEntity jsonEntity = templateJsonMapper.getInfo(task.getFlowId());
        String instanceId = flowAbleUrl.startInstance(jsonEntity.getFlowableId(), variables);
        entity.setInstanceId(instanceId);

        if (taskMapper.insert(entity) > 0) {
            List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
            TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                    .filter(e -> StringUtils.equals(NodeEnum.START.getType(), e.getNodeType())).findFirst().orElse(new TemplateNodeEntity());
            NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
            // 记录
            FlowMethod method = new FlowMethod();
            method.setFlowModel(flowModel);
            method.setType(RecordEnum.SUBMIT.getCode());
            OperatorEntity operatorEntity = new OperatorEntity();
            operatorEntity.setNodeCode(nodeEntity.getNodeCode());
            operatorEntity.setNodeName(nodeModel.getNodeName());
            operatorEntity.setTaskId(entity.getId());
            operatorEntity.setHandleId(entity.getCreatorUserId());
            if (StringUtils.isNotBlank(flowModel.getUserId())) {
                method.setHandId(flowModel.getUserId());
            }
            operatorEntity.setHandleTime(entity.getStartTime());
            method.setOperatorEntity(operatorEntity);
            recordMapper.createRecord(method);
            // 节点记录
            NodeRecordModel nodeRecordModel = new NodeRecordModel();
            nodeRecordModel.setTaskId(entity.getId());
            nodeRecordModel.setNodeCode(nodeEntity.getNodeCode());
            nodeRecordModel.setNodeName(nodeModel.getNodeName());
            nodeRecordModel.setNodeStatus(NodeStateEnum.SUBMIT.getCode());
            nodeRecordMapper.create(nodeRecordModel);
            // 保存到撤销表
            RevokeEntity revokeEntity = new RevokeEntity();
            revokeEntity.setId(RandomUtil.uuId());
            revokeEntity.setTaskId(task.getId());
            revokeEntity.setRevokeTaskId(entity.getId());

            RevokeFormDataModel model = new RevokeFormDataModel();
            String billNumber = serviceUtil.getBillNumber();
            model.setBillRule(billNumber);
            model.setHandleOpinion(flowModel.getHandleOpinion());
            Date date = new Date();
            model.setCreatorTime(date.getTime());
            model.setRevokeTaskId(task.getId());
            model.setRevokeTaskName(task.getFullName());
            String str = JsonUtil.getObjectToString(model);
            revokeEntity.setFormData(str);
            revokeMapper.insert(revokeEntity);
            // 处理撤销经办
            FlowMethod flowMethod = new FlowMethod();
            flowMethod.setFlowModel(flowModel);
            flowMethod.setTaskId(task.getId());
            flowMethod.setTaskEntity(entity);
            handleRevokeOperator(flowMethod);
            // 自动审批
            FlowModel revokeModel = JsonUtil.getJsonToBean(flowModel, FlowModel.class);
            revokeModel.setTaskEntity(entity);
            autoAudit(revokeModel);
            // 判断结束
            flowModel.setTaskEntity(entity);
            if (this.isFinished(flowModel)) {
                this.endRevoke(task);
            }
        }
    }

    public void endRevoke(TaskEntity task) {
        QueryWrapper<TaskEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().in(TaskEntity::getParentId, task.getId());
        List<TaskEntity> subTaskList = taskMapper.selectList(wrapper);
        // 变更子流程任务状态
        if (CollUtil.isNotEmpty(subTaskList)) {
            for (TaskEntity subTask : subTaskList) {
                subTask.setStatus(TaskStatusEnum.REVOKED.getCode());
            }
            taskMapper.updateById(subTaskList);
        }
        task.setStatus(TaskStatusEnum.REVOKED.getCode());
        taskMapper.updateById(task);
    }


    // 更新当前节点
    public void updateCurrentNode(List<FlowableTaskModel> taskModelList, Map<String, NodeModel> nodes, TaskEntity taskEntity) {
        List<String> currentNodeList = new ArrayList<>();
        List<String> currentNodeNameList = new ArrayList<>();

        List<String> nodeTypes = ImmutableList.of(NodeEnum.APPROVER.getType(), NodeEnum.SUB_FLOW.getType(), NodeEnum.PROCESSING.getType(), NodeEnum.OUTSIDE.getType());
        for (FlowableTaskModel model : taskModelList) {
            NodeModel nodeModel = nodes.get(model.getTaskKey());
            if (null != nodeModel && nodeTypes.contains(nodeModel.getType())) {
                currentNodeList.add(nodeModel.getNodeId());
                currentNodeNameList.add(nodeModel.getNodeName());
            }
        }
        if (CollUtil.isNotEmpty(currentNodeList)) {
            // 更新任务的当前节点信息
            taskEntity.setCurrentNodeName(String.join(",", currentNodeNameList));
            taskEntity.setCurrentNodeCode(String.join(",", currentNodeList));
            taskMapper.updateById(taskEntity);
        }
    }


    // 拒绝结束流程
    public void handleEndTask(FlowMethod flowMethod) throws WorkFlowException {
        OperatorEntity operator = flowMethod.getOperatorEntity();
        List<CirculateEntity> circulateList = flowMethod.getCirculateList();
        FlowModel flowModel = flowMethod.getFlowModel();
        TaskEntity taskEntity = flowModel.getTaskEntity();
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();

        // 触发
        if (Boolean.FALSE.equals(flowMethod.getIsRevoke())) {
            flowModel.setAction(ActionEnum.REJECT.getCode());
            flowModel.setRejectTrigger(true);
            handleTrigger(operator, flowModel);
        }

        // 直接结束流程
        flowMethod.setState(TaskStatusEnum.REJECTED.getCode());
        boolean isEnd = getTaskList(operator.getTaskId()).isEmpty();
        this.endTask(flowMethod, isEnd);
        // 处理当前节点
        flowModel.setFinishFlag(false);
        List<String> codes = Arrays.stream(taskEntity.getCurrentNodeCode().split(",")).collect(Collectors.toList());
        codes.remove(operator.getNodeCode());
        taskEntity.setCurrentNodeCode(String.join(",", codes));

        // 节点记录
        NodeRecordModel nodeRecordModel = new NodeRecordModel();
        nodeRecordModel.setTaskId(operator.getTaskId());
        nodeRecordModel.setNodeId(operator.getNodeId());
        nodeRecordModel.setNodeCode(operator.getNodeCode());
        nodeRecordModel.setNodeName(operator.getNodeName());
        nodeRecordModel.setNodeStatus(NodeStateEnum.REJECT.getCode());
        nodeRecordMapper.create(nodeRecordModel);

        FlowMsgModel flowMsgModel = new FlowMsgModel();
        flowMsgModel.setNodeList(nodeEntityList);
        flowMsgModel.setUserInfo(flowModel.getUserInfo());
        flowMsgModel.setTaskEntity(taskEntity);
        flowMsgModel.setNodeCode(operator.getNodeCode());
        flowMsgModel.setWait(false);
        flowMsgModel.setReject(true);
        flowMsgModel.setCopy(true);
        List<OperatorEntity> operatorList = new ArrayList<>();
        OperatorEntity entity = new OperatorEntity();
        entity.setNodeCode(operator.getNodeCode());
        entity.setHandleId(taskEntity.getCreatorUserId());
        operatorList.add(entity);
        flowMsgModel.setOperatorList(operatorList);
        flowMsgModel.setCirculateList(circulateList);
        flowMsgModel.setFormData(FlowContextHolder.getAllData());
        msgUtil.message(flowMsgModel);

        this.isFinished(flowModel);
    }


    public void updateGlobalParam(TaskEntity taskEntity, NodeModel nodeModel, NodeModel global, Map<String, Object> formData) {
        this.getGlobalParam(taskEntity, nodeModel, global, formData);
        taskMapper.updateById(taskEntity);
    }


    public void getGlobalParam(TaskEntity taskEntity, NodeModel nodeModel, NodeModel global, Map<String, Object> formData) {
        List<FlowParamModel> paramModelList = global.getGlobalParameterList();
        if (CollUtil.isEmpty(paramModelList)) {
            return;
        }
        formData = formData == null ? new HashMap<>() : formData;
        List<TemplateJsonModel> list = new ArrayList<>();
        for (GroupsModel group : nodeModel.getParameterList()) {
            TemplateJsonModel model = new TemplateJsonModel();
            model.setField(group.getField());
            model.setSourceType(Objects.equals(FieldEnum.CONDITION.getCode(), group.getFieldValueType()) ? FieldEnum.SYSTEM.getCode() : group.getFieldValueType());
            model.setRelationField(String.valueOf(group.getFieldValue()));
            list.add(model);
        }
        Map<String, Object> taskMap = taskEntity.getGlobalParameter() != null ? JsonUtil.stringToMap(taskEntity.getGlobalParameter()) : new HashMap<>();
        if (CollUtil.isEmpty(taskMap)) {
            for (FlowParamModel model : paramModelList) {
                if (model.getDefaultValue() != null) {
                    taskMap.put(model.getFieldName(), model.getDefaultValue());
                }
            }
        }
        RecordEntity recordEntity = new RecordEntity();
        recordEntity.setNodeCode(nodeModel.getNodeId());
        FlowModel parameterModel = new FlowModel();
        parameterModel.setFormData(formData);
        parameterModel.setRecordEntity(recordEntity);
        parameterModel.setTaskEntity(taskEntity);
        Map<String, String> map = flowUtil.parameterMap(parameterModel, list);
        taskMap.putAll(map);
        for (FlowParamModel model : paramModelList) {
            if (ObjectUtil.equals(model.getDataType(), "datetime")) {
                String fieldName = model.getFieldName();
                Long longVal = Long.valueOf(String.valueOf(taskMap.get(fieldName)));
                taskMap.put(fieldName, longVal);
            }
        }
        taskEntity.setGlobalParameter(JsonUtil.getObjectToString(taskMap));
    }

    /**
     * 依次审批的排序
     *
     * @param userIds   用户主键集合
     * @param nodeModel 节点属性
     */
    public List<String> improperSort(List<String> userIds, NodeModel nodeModel) {
        List<String> list = new ArrayList<>();
        List<String> sortList = nodeModel.getApproversSortList();
        if (sortList.isEmpty()) {
            return userIds;
        }
        for (String id : sortList) {
            List<String> collect = userIds.stream().filter(id::contains).collect(Collectors.toList());
            list.addAll(collect);
        }
        return list.stream().distinct().collect(Collectors.toList());
    }

    // 结束任务
    public void endTask(FlowMethod flowMethod, boolean isEnd) throws WorkFlowException {
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        Integer state = flowMethod.getState();
        taskEntity.setStatus(state);
        taskEntity.setEndTime(new Date());
        if (taskMapper.updateById(taskEntity) > 0) {
            flowMethod.setTaskId(taskEntity.getId());
            flowMethod.setNodeCode(null);
            endOperator(flowMethod);
            if (isEnd) {
                flowAbleUrl.deleteInstance(taskEntity.getInstanceId(), "reject");
            }
        }
    }

    /**
     * 判断任务是否结束
     *
     * @param flowModel 参数
     */
    public boolean isFinished(FlowModel flowModel) throws WorkFlowException {
        TaskEntity taskEntity = flowModel.getTaskEntity();
        if (null == taskEntity.getId()) {
            return false;
        }
        TaskEntity task = taskMapper.selectById(taskEntity.getId());
        if (null != task && ObjectUtil.equals(task.getStatus(), TaskStatusEnum.CANCEL.getCode())) {
            return false;
        }
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        String instanceId = taskEntity.getInstanceId();
        boolean flag;
        FlowableInstanceModel instance = flowAbleUrl.getInstance(instanceId);
        if (null != instance && null != instance.getEndTime()) {
            flag = true;
        } else {
            List<String> historicEnd = flowAbleUrl.getHistoricEnd(instanceId);
            flag = CollUtil.isNotEmpty(historicEnd);
        }
        if (flag) {
            TemplateNodeEntity endNodeEntity = nodeEntityList.stream()
                    .filter(e -> StringUtils.equals(e.getNodeType(), NodeEnum.END.getType())).findFirst().orElse(null);
            if (null != endNodeEntity) {
                boolean finishFlag = flowModel.getFinishFlag();
                boolean isEnd = getTaskList(taskEntity.getId()).isEmpty();
                boolean isPass = finishFlag && isEnd;
                if (finishFlag) {
                    taskEntity.setCurrentNodeCode(FlowNature.END_CODE);
                    taskEntity.setCurrentNodeName(FlowNature.END_NAME);
                    if (!isEnd) {
                        taskEntity.setCurrentNodeCode(FlowNature.TRIGGER_CODE);
                        taskEntity.setCurrentNodeName(FlowNature.TRIGGER_NAME);
                    }
                }
                if (isPass) {
                    Integer status = flowModel.getHandleStatus().equals(FlowNature.AUDIT_COMPLETION) ?
                            TaskStatusEnum.PASSED.getCode() : TaskStatusEnum.REJECTED.getCode();
                    taskEntity.setStatus(status);
                    taskEntity.setEndTime(new Date());
                }
                if (taskMapper.updateById(taskEntity) > 0) {
                    if (isPass || !finishFlag) {
                        this.subFlowOfEnd(flowModel);
                        //结束事件
                        flowModel.setEventStatus(EventEnum.END.getStatus());
                        addEvent(flowModel);
                        // 发送结束消息
                        FlowMsgModel flowMsgModel = new FlowMsgModel();
                        flowMsgModel.setNodeList(nodeEntityList);
                        flowMsgModel.setTaskEntity(taskEntity);
                        flowMsgModel.setUserInfo(flowModel.getUserInfo());
                        flowMsgModel.setEnd(true);
                        flowMsgModel.setFormData(FlowContextHolder.getAllData());
                        msgUtil.message(flowMsgModel);
                    }
                    return isEnd;
                }
            }
        }
        return false;
    }


    // 判断能否归档
    public AuditModel getAuditModel(String taskId, FlowModel flowModel, OperatorEntity operator) {
        TaskEntity task = taskMapper.selectById(taskId);
        AuditModel model = new AuditModel();
        if (operator != null && ObjectUtil.equals(operator.getStatus(), OperatorStateEnum.REVOKE.getCode())) {
            return model;
        }
        if (task != null) {
            model.setTaskId(taskId);
            if (task.getEndTime() != null) {
                NodeModel global = flowModel.getNodes().get(NodeEnum.GLOBAL.getType());
                FileConfig config = global.getFileConfig();
                if (Boolean.TRUE.equals(config.getOn()) && StringUtils.isNotBlank(config.getTemplateId())) {
                    model.setIsEnd(true);
                }
            }
        }
        return model;
    }

    // 子流程结束 要处理的事情
    public void subFlowOfEnd(FlowModel flowModel) throws WorkFlowException {
        TaskEntity taskEntity = flowModel.getTaskEntity();
        if (StringUtils.equals(FlowNature.PARENT_ID, taskEntity.getParentId())) {
            return;
        }
        // 获取父级节点集合
        TaskEntity parentTask = taskMapper.getInfo(taskEntity.getParentId());
        if (null == parentTask) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        TemplateJsonEntity jsonEntity = templateJsonMapper.selectById(parentTask.getFlowId());
        String deploymentId = jsonEntity.getFlowableId();
        List<TemplateNodeEntity> parentNodeList = templateNodeMapper.getList(parentTask.getFlowId());

        String subCode = taskEntity.getSubCode();
        SubParameterModel subParameter = JsonUtil.getJsonToBean(taskEntity.getSubParameter(), SubParameterModel.class);
        String flowableTaskId = subParameter.getNodeId();
        String parentCode = subParameter.getParentCode();

        TemplateNodeEntity parentNode = parentNodeList.stream()
                .filter(e -> e.getNodeCode().equals(parentCode)).findFirst().orElse(new TemplateNodeEntity());
        TemplateNodeEntity subNode = parentNodeList.stream()
                .filter(e -> e.getNodeCode().equals(subCode)).findFirst().orElse(new TemplateNodeEntity());
        NodeModel nodeModel = JsonUtil.getJsonToBean(subNode.getNodeJson(), NodeModel.class);
        if (nodeModel == null) {
            return;
        }
        // 判断是否依次创建，存在依次创建的子流程数据，则发起子流程
        if (!ObjectUtil.equals(nodeModel.getCreateRule(), FlowNature.CHILD_SYNC)) {
            List<SubtaskDataEntity> list = subtaskDataMapper.getList(taskEntity.getParentId(), subCode);
            if (CollUtil.isNotEmpty(list)) {
                SubtaskDataEntity subtaskData = list.get(0);
                FlowModel model = JsonUtil.getJsonToBean(subtaskData.getSubtaskJson(), FlowModel.class);
                batchSaveOrSubmit(model);
                subtaskDataMapper.deleteById(subtaskData);
                autoAudit(model);
                return;
            }
        }

        // 判断下一级是否子流程节点，且是否存在候选人
        NextOrPrevFo fo = new NextOrPrevFo();
        fo.setDeploymentId(deploymentId);
        fo.setTaskKey(subCode);
        List<FlowableNodeModel> nextModels = flowAbleUrl.getNext(fo);

        for (FlowableNodeModel nextModel : nextModels) {
            TemplateNodeEntity nodeEntity = parentNodeList.stream().filter(e -> ObjectUtil.equals(e.getNodeCode(), nextModel.getId()))
                    .findFirst().orElse(new TemplateNodeEntity());
            if (ObjectUtil.equals(nodeEntity.getNodeType(), NodeEnum.SUB_FLOW.getType())) {
                NodeModel subFlow = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
                TemplateEntity template = templateMapper.selectById(subFlow.getFlowId());
                TemplateJsonEntity subJsonEntity = templateJsonMapper.selectById(template.getFlowId());
                List<TemplateNodeEntity> subNodeList = templateNodeMapper.getList(template.getFlowId());
                Map<String, NodeModel> subNodes = new HashMap<>();
                for (TemplateNodeEntity node : subNodeList) {
                    subNodes.put(node.getNodeCode(), JsonUtil.getJsonToBean(node.getNodeJson(), NodeModel.class));
                }
                TemplateNodeEntity subStart = subNodeList.stream()
                        .filter(e -> StringUtils.equals(NodeEnum.START.getType(), e.getNodeType())).findFirst().orElse(new TemplateNodeEntity());

                FlowMethod method = new FlowMethod();
                method.setDeploymentId(subJsonEntity.getFlowableId());
                method.setNodes(subNodes);
                method.setNodeEntityList(subNodeList);
                method.setNodeCode(subStart.getNodeCode());

                if (flowUtil.checkBranch(subStart)) {
                    throw new WorkFlowException(MsgCode.WF121.get());
                }
                List<NodeModel> nextApprover = flowUtil.getNextApprover(method);
                if (!flowUtil.checkNextCandidates(nextApprover)) {
                    throw new WorkFlowException(MsgCode.WF121.get());
                }
                FlowModel model = new FlowModel();
                model.setDeploymentId(flowModel.getDeploymentId());
                model.setNodeEntityList(subNodeList);
                if (flowUtil.checkNextError(model, nextApprover, false, false) != 0) {
                    throw new WorkFlowException(MsgCode.WF121.get());
                }
            }
        }

        if (nodeModel.getIsAsync().equals(FlowNature.CHILD_ASYNC)) {
            // 异步已经完成节点
            return;
        }
        // 父节点下的子流程
        QueryWrapper<TaskEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(TaskEntity::getParentId, taskEntity.getParentId())
                .eq(TaskEntity::getSubCode, subCode);
        List<TaskEntity> subList = taskMapper.selectList(wrapper);
        // 未审批集合
        List<TaskEntity> unfinishedList = subList.stream()
                .filter(e -> !e.getStatus().equals(TaskStatusEnum.PASSED.getCode()) && !e.getStatus().equals(TaskStatusEnum.REJECTED.getCode()))
                .collect(Collectors.toList());
        if (unfinishedList.isEmpty()) {
            FlowModel model = JsonUtil.getJsonToBean(flowModel, FlowModel.class);
            model.setFinishFlag(true);
            subList = subList.stream().filter(e -> null != e.getEndTime())
                    .sorted(Comparator.comparing(TaskEntity::getEndTime).reversed()).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(subList)) {
                TaskEntity finalTask = subList.get(0);
                model.setHandleStatus(ObjectUtil.equals(finalTask.getStatus(), TaskStatusEnum.PASSED.getCode()) ? 1 : 0);
            } else {
                model.setHandleStatus(1);
            }
            String formId = parentNode.getFormId();
            flowUtil.setFlowModel(parentTask.getId(), model);
            Map<String, NodeModel> nodes = model.getNodes();
            Map<String, Object> formData = serviceUtil.infoData(formId, parentTask.getId());
            model.setFormData(formData);
            FlowMethod flowMethod = new FlowMethod();
            flowMethod.setDeploymentId(model.getDeploymentId());
            flowMethod.setNodeCode(subCode);
            flowMethod.setFormData(formData);
            flowMethod.setNodes(nodes);
            flowMethod.setTaskEntity(parentTask);
            Map<String, Boolean> resMap = conditionUtil.handleCondition(flowMethod);
            conditionUtil.checkCondition(resMap, nodes);
            flowMethod.setFlowModel(model);
            flowMethod.setFlowableTaskId(flowableTaskId);
            flowMethod.setResMap(resMap);
            flowMethod.setNodeEntityList(parentNodeList);
            // 递归subCode的所有上级节点，获取审批节点的经办
            model.setOperatorEntity(new OperatorEntity());
            List<String> nodeCodeList = new ArrayList<>();
            flowUtil.prevNodeList(flowModel.getDeploymentId(), subCode, parentNodeList, nodeCodeList);
            if (CollUtil.isNotEmpty(nodeCodeList)) {
                List<OperatorEntity> list = getList(parentTask.getId(), nodeCodeList);
                if (CollUtil.isNotEmpty(list)) {
                    model.setOperatorEntity(list.get(0));
                }
            }
            completeNode(flowMethod);
        }
    }

    // 删除子流程（退回时若是重新审批）
    public void deleteSubflow(String taskId, List<String> nodeCodeList) {
        QueryWrapper<TaskEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TaskEntity::getParentId, taskId);
        if (CollUtil.isNotEmpty(nodeCodeList)) {
            queryWrapper.lambda().in(TaskEntity::getSubCode, nodeCodeList);
        }
        List<TaskEntity> list = taskMapper.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            List<String> idList = list.stream().map(TaskEntity::getId).distinct().collect(Collectors.toList());
            List<String> idAll = new ArrayList<>();
            taskMapper.deleTaskAll(idList, idAll);
            List<TaskEntity> childList = taskMapper.getInfosSubmit(idAll.toArray(new String[]{}), TaskEntity::getId, TaskEntity::getFlowId);
            for (TaskEntity entity : childList) {
                addDelTask(entity.getId(), entity.getFlowId());
            }
            taskMapper.setIgnoreLogicDelete().deleteByIds(idAll);
            taskMapper.clearIgnoreLogicDelete();
        }
    }

    public void getByRule(List<String> userIdAll, UserEntity flowUser, int rule) {
        if (flowUser != null) {
            String userId = flowUser.getId();
            //附加条件
            switch (ExtraRuleEnum.getByCode(rule)) {
                case ROLE:
                    List<String> roleId = serviceUtil.getRoleObjectId(ImmutableList.of(userId)).stream().map(RoleRelationEntity::getRoleId).collect(Collectors.toList());
                    List<String> userIdList = serviceUtil.getListByRoleId(roleId).stream().map(RoleRelationEntity::getObjectId).collect(Collectors.toList());
                    List<String> roleUserId = userIdList.stream().filter(e -> !ObjectUtil.equals(e, userId)).collect(Collectors.toList());
                    userIdAll.addAll(roleUserId);
                    break;
                case ORGANIZE:
                case GROUP:
                case POSITION:
                    String type = Objects.equals(ExtraRuleEnum.GROUP.getCode(), rule) ? PermissionConst.GROUP : PermissionConst.POSITION;
                    List<UserRelationEntity> userRelationList = serviceUtil.getListByUserIdAll(ImmutableList.of(userId));
                    List<String> objectIdList = new ArrayList<>();
                    List<String> idList = userRelationList.stream().filter(e -> ObjectUtil.equals(e.getObjectType(), type))
                            .map(UserRelationEntity::getObjectId).collect(Collectors.toList());
                    if (Objects.equals(ExtraRuleEnum.ORGANIZE.getCode(), rule)) {
                        List<PositionEntity> positionList = serviceUtil.getPosList(idList);
                        List<String> organizeList = positionList.stream().map(PositionEntity::getOrganizeId).collect(Collectors.toList());
                        List<String> orgList = serviceUtil.getOrganizeList(organizeList).stream().filter(e -> Objects.equals(e.getCategory(), PermissionConst.DEPARTMENT)).map(OrganizeEntity::getId).collect(Collectors.toList());
                        objectIdList.addAll(orgList);
                    } else {
                        objectIdList.addAll(idList);
                    }
                    List<String> userIds = serviceUtil.getListByObjectIdAll(objectIdList).stream().map(UserRelationEntity::getUserId)
                            .filter(e -> !ObjectUtil.equals(e, userId)).collect(Collectors.toList());
                    userIdAll.addAll(userIds);
                    break;
                default:
                    break;
            }
        }
    }

    //封装用户对象
    public List<CandidateUserVo> getUserModel(List<String> ids, Pagination pagination) {
        List<String> userIds = serviceUtil.getUserListAll(ids);
        List<UserEntity> userList = serviceUtil.getUserName(userIds, pagination);
        pagination.setTotal(pagination.getTotal());
        List<String> userIdList = userList.stream().map(UserEntity::getId).collect(Collectors.toList());
        List<UserRelationEntity> userRelationList = serviceUtil.getListByUserIdAll(userIdList);
        Map<String, List<UserRelationEntity>> userMap = userRelationList.stream()
                .filter(t -> PermissionConst.POSITION.equals(t.getObjectType())).collect(Collectors.groupingBy(UserRelationEntity::getUserId));
        List<CandidateUserVo> list = new ArrayList<>();
        for (UserEntity user : userList) {
            CandidateUserVo vo = JsonUtil.getJsonToBean(user, CandidateUserVo.class);
            vo.setFullName(user.getRealName() + "/" + user.getAccount());
            vo.setHeadIcon(UploaderUtil.uploaderImg(user.getHeadIcon()));
            List<UserRelationEntity> listByUserId = userMap.get(user.getId()) != null ? userMap.get(user.getId()) : new ArrayList<>();
            StringJoiner joiner = new StringJoiner(",");
            for (UserRelationEntity relation : listByUserId) {
                StringJoiner name = new StringJoiner("/");
                PositionEntity position = serviceUtil.getPositionInfo(relation.getObjectId());
                if (position != null) {
                    OrganizeEntity organize = serviceUtil.getOrganizeInfo(position.getOrganizeId());
                    if (organize != null) {
                        List<String> organizeIdTree = new ArrayList<>(Arrays.asList(organize.getOrganizeIdTree().split(",")));
                        List<OrganizeEntity> organizeList = serviceUtil.getOrganizeList(organizeIdTree);
                        for (String organizeId : organizeIdTree) {
                            OrganizeEntity entity = organizeList.stream().filter(e -> Objects.equals(e.getId(), organizeId)).findFirst().orElse(null);
                            if (entity != null) {
                                name.add(entity.getFullName());
                            }
                        }
                    }
                    List<String> positionIdTree = new ArrayList<>(Arrays.asList(position.getPositionIdTree().split(",")));
                    List<PositionEntity> positionList = serviceUtil.getPosList(positionIdTree);
                    for (String positionId : positionIdTree) {
                        PositionEntity entity = positionList.stream().filter(e -> Objects.equals(e.getId(), positionId)).findFirst().orElse(null);
                        if (entity != null) {
                            name.add(entity.getFullName());
                        }
                    }
                }
                joiner.add(name.toString());
            }
            vo.setOrganize(joiner.toString());
            list.add(vo);
        }
        return list;
    }

    /**
     * 处理数据传递，并存储在线程，后续保存数据库
     */
    public Map<String, Object> dataTransfer(FlowMethod flowMethod) throws WorkFlowException {
        List<TemplateNodeEntity> nodeEntityList = flowMethod.getNodeEntityList();
        FlowModel flowModel = flowMethod.getFlowModel();
        Map<String, Object> formData = flowModel.getFormData();
        TemplateNodeEntity globalEntity = nodeEntityList.stream().filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.GLOBAL.getType())).findFirst().orElse(null);
        // 判断是否能数据传递
        if (null != globalEntity) {
            NodeModel global = JsonUtil.getJsonToBean(globalEntity.getNodeJson(), NodeModel.class);
            if (Boolean.FALSE.equals(global.getHasAloneConfigureForms())) {
                return formData;
            }
        }
        TemplateNodeEntity nodeEntity = flowMethod.getNodeEntity();
        NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
        if (CollUtil.isEmpty(nodeModel.getAssignList())) {
            return formData;
        }
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        String rejectDataId = taskEntity.getRejectDataId();
        //保存数据的表单
        String formId = nodeEntity.getFormId();
        String taskId = taskEntity.getId();
        boolean isAssign = flowMethod.getIsAssign();
        Map<String, Object> data = flowUtil.createOrUpdate(flowMethod);
        String resultNodeCode = flowMethod.getResultNodeCode();
        //判断是否是外部节点最后一个审批
        TemplateNodeEntity resultNode = nodeEntityList.stream().filter(e -> Objects.equals(e.getNodeCode(), resultNodeCode)).findFirst().orElse(null);
        if (resultNode != null && Objects.equals(resultNode.getNodeType(), NodeEnum.OUTSIDE.getType()) && StringUtils.isNotEmpty(rejectDataId)) {
            return formData;
        }

        List<String> typeList = ImmutableList.of(NodeEnum.END.getType(), NodeEnum.SUB_FLOW.getType());
        if (isAssign) {
            boolean isWrite = !typeList.contains(nodeModel.getType());
            FlowContextHolder.addChildData(taskId, formId, data, new ArrayList<>(), isWrite);
        }
        return data;
    }

    // 删除表单数据
    public void deleteFormData(List<TaskEntity> taskList) {
        if (taskList.isEmpty()) {
            return;
        }
        List<TemplateNodeEntity> nodeEntityList = new ArrayList<>();
        List<String> flowIds = taskList.stream().map(TaskEntity::getFlowId).collect(Collectors.toList());
        if (!flowIds.isEmpty()) {
            nodeEntityList = templateNodeMapper.getList(flowIds, null);
        }

        for (TaskEntity taskEntity : taskList) {
            List<String> formIds = nodeEntityList.stream().filter(e -> e.getFlowId().equals(taskEntity.getFlowId()) && StringUtils.isNotBlank(e.getFormId())).map(TemplateNodeEntity::getFormId).distinct().collect(Collectors.toList());
            for (String formId : formIds) {
                serviceUtil.deleteFormData(formId, taskEntity.getId());
            }
        }
    }

    public List<String> getNextList(String deploymentId, String nodeCode, Map<String, NodeModel> nodes, List<String> nextList) throws WorkFlowException {
        nextList = nextList != null ? nextList : new ArrayList<>();
        NextOrPrevFo fo = new NextOrPrevFo();
        fo.setDeploymentId(deploymentId);
        fo.setTaskKey(nodeCode);
        List<FlowableNodeModel> nextModels = flowAbleUrl.getNext(fo);
        for (FlowableNodeModel next : nextModels) {

            NodeModel nodeModel = nodes.get(next.getId());
            if (null != nodeModel) {
                // 子流程往下递归
                List<String> typeList = ImmutableList.of(NodeEnum.SUB_FLOW.getType());
                if (typeList.contains(nodeModel.getType())) {
                    nextList.add(next.getId());
                    getNextList(deploymentId, next.getId(), nodes, nextList);
                } else {
                    nextList.add(next.getId());
                }
            }
        }
        return nextList;
    }

    public void checkTemplateHide(String templateId) throws WorkFlowException {
        TemplateEntity template = templateMapper.selectById(templateId);
        if (null != template) {
            List<Integer> templateStatus = ImmutableList.of(TemplateStatueEnum.UP.getCode(), TemplateStatueEnum.DOWN_CONTINUE.getCode());
            if (!templateStatus.contains(template.getStatus())) {
                throw new WorkFlowException(MsgCode.WF140.get());
            }
        }
    }

    public ActionResult<Object> launchFlow(FlowModel flowModel) throws WorkFlowException {
        String templateId = flowModel.getTemplateId();
        List<String> userIds = flowModel.getUserIds();
        List<Map<String, Object>> formDataList = flowModel.getFormDataList();
        if (CollUtil.isEmpty(formDataList)) {
            return ActionResult.fail("未选择记录，发起失败");
        }
        TemplateEntity template = templateMapper.selectById(templateId);
        if (null == template) {
            return ActionResult.fail(MsgCode.WF122.get());
        }
        if (!ObjectUtil.equals(template.getStatus(), TemplateStatueEnum.UP.getCode())) {
            throw new WorkFlowException(MsgCode.WF140.get());
        }
        TemplateJsonEntity jsonEntity = templateJsonMapper.selectById(template.getFlowId());
        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(jsonEntity.getId());
        Map<String, NodeModel> nodes = new HashMap<>();
        for (TemplateNodeEntity node : nodeEntityList) {
            nodes.put(node.getNodeCode(), JsonUtil.getJsonToBean(node.getNodeJson(), NodeModel.class));
        }
        FlowMethod method = new FlowMethod();
        method.setDeploymentId(jsonEntity.getFlowableId());
        TemplateNodeEntity startNode = nodeEntityList.stream().filter(e -> StringUtils.equals(NodeEnum.START.getType(), e.getNodeType())).findFirst().orElse(new TemplateNodeEntity());
        if (flowUtil.checkBranch(startNode)) {
            throw new WorkFlowException(MsgCode.WF147.get());
        }
        method.setNodeCode(startNode.getNodeCode());
        method.setNodes(nodes);
        method.setNodeEntityList(nodeEntityList);
        Map<String, Boolean> resMap = conditionUtil.handleCondition(method);
        // 判断条件、候选人
        try {
            conditionUtil.checkCondition(resMap, nodes);
        } catch (WorkFlowException e) {
            throw new WorkFlowException(MsgCode.WF133.get());
        }
        method.setNextSubFlow(true);
        List<NodeModel> nextApprover = flowUtil.getNextApprover(method);
        if (!flowUtil.checkNextCandidates(nextApprover)) {
            throw new WorkFlowException(MsgCode.WF134.get());
        }

        String flowId = jsonEntity.getId();
        // 判断流程权限
        List<String> userListAll = serviceUtil.getUserListAll(userIds);
        FlowFormModel formIdAndFlowId = flowUtil.getFormIdAndFlowId(userListAll, templateId);
        List<UserEntity> userList = serviceUtil.getUserName(Boolean.TRUE.equals(flowModel.getHasPermission()) ? formIdAndFlowId.getUserId() : formIdAndFlowId.getUserIdAll(), true);
        if (CollUtil.isEmpty(userList)) {
            throw new WorkFlowException(MsgCode.WF136.get());
        }
        for (UserEntity user : userList) {
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(user.getId());
            userInfo.setUserName(user.getRealName());

            FlowModel model = new FlowModel();
            model.setUserInfo(userInfo);
            model.setFlowId(flowId);
            model.setStatus(TaskStatusEnum.RUNNING.getCode());
            model.setDeploymentId(jsonEntity.getFlowableId());
            model.setNodeEntityList(nodeEntityList);
            for (Map<String, Object> formData : formDataList) {
                model.setFormData(formData);
                if (flowUtil.checkNextError(model, nextApprover, false, false) != 0) {
                    throw new WorkFlowException(MsgCode.WF135.get());
                }
                batchSaveOrSubmit(model);
                flowUtil.event(model, 1);
                TaskEntity taskEntity = model.getTaskEntity();
                if (taskEntity.getRejectDataId() == null) {
                    autoAudit(model);
                    handleEvent();
                }
                handleTaskStatus();
            }
        }
        return ActionResult.success(MsgCode.SU006.get());
    }

    public void batchSaveOrSubmit(FlowModel flowModel) throws WorkFlowException {
        UserInfo userInfo = flowModel.getUserInfo();
        if (null == userInfo) {
            userInfo = UserProvider.getUser();
            flowModel.setUserInfo(userInfo);
        }

        String delegateUser = flowModel.getDelegateUser();
        if (StringUtils.isBlank(delegateUser)) {
            this.saveOrSubmit(flowModel);
        } else {
            FlowModel model = JsonUtil.getJsonToBean(flowModel, FlowModel.class);
            model.setDelegateUser(userInfo.getUserId());
            model.setUserId(delegateUser);
            UserEntity userEntity = serviceUtil.getUserInfo(delegateUser);
            if (null != userEntity) {
                UserInfo info = model.getUserInfo();
                info.setUserName(userEntity.getRealName());
                info.setUserAccount(userEntity.getAccount());
                info.setUserId(userEntity.getId());
                model.setUserInfo(info);
            }
            this.saveOrSubmit(model);
            // 赋值给原来的FlowModel
            BeanUtil.copyProperties(model, flowModel);
        }
        // 保存表单数据
        FlowContextHolder.deleteFormOperator();
        Map<String, Map<String, Object>> allData = FlowContextHolder.getAllData();
        Map<String, List<Map<String, Object>>> formOperates = FlowContextHolder.getFormOperates();
        List<String> writeIdList = FlowContextHolder.getWriteIdList();
        for (String idAll : writeIdList) {
            String[] idList = idAll.split(JnpfConst.SIDE_MARK);
            List<Map<String, Object>> operates = formOperates.get(idAll);
            Map<String, Object> formData = allData.get(idAll);
            formData = formData == null ? new HashMap<>() : formData;
            String flowId = (String) formData.get(FlowFormConstant.FLOWID);
            FlowFormDataModel formDataModel = FlowFormDataModel.builder().formId(idList[1]).id(idList[0]).map(formData).formOperates(operates).flowId(flowId).isTransfer(true).build();
            serviceUtil.saveOrUpdateFormData(formDataModel);
        }
        FlowContextHolder.clearAll();
    }

    // 提交或暂存
    public void saveOrSubmit(FlowModel flowModel) throws WorkFlowException {
        TemplateEntity templateEntity = null;

        TemplateJsonEntity jsonEntity = templateJsonMapper.selectById(flowModel.getFlowId());
        if (null == jsonEntity) {
            templateEntity = templateMapper.getInfo(flowModel.getFlowId());
            jsonEntity = templateJsonMapper.getInfo(templateEntity.getFlowId());
        } else {
            templateEntity = templateMapper.getInfo(jsonEntity.getTemplateId());
        }

        UserInfo userInfo = flowModel.getUserInfo();
        List<String> branchList = flowModel.getBranchList();
        String version = jsonEntity.getVersion();

        //自由流程
        if (Objects.equals(templateEntity.getType(), FlowNature.FREE) && Objects.equals(TaskStatusEnum.RUNNING.getCode(), flowModel.getStatus())) {
            TaskEntity task = taskMapper.selectById(flowModel.getId());
            if (task == null || Objects.equals(task.getStatus(), TaskStatusEnum.TO_BE_SUBMIT.getCode())) {
                FreeModel freeParameter = flowModel.getFreeFlowConfig() != null ? flowModel.getFreeFlowConfig() : new FreeModel();
                freeParameter.setIsEnd(null);
                String flowId = RandomUtil.uuId();

                List<TemplateNodeEntity> list = templateNodeMapper.getList(jsonEntity.getId());

                String freeNodeName = "自由节点";
                Map<String, Map<String, Object>> approverMap = new HashMap<>();
                Map<String, Map<String, Object>> flowNodes = new HashMap<>();
                for (TemplateNodeEntity nodeEntity : list) {
                    NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
                    Map<String, Object> map = JsonUtil.stringToMap(nodeEntity.getNodeJson());
                    if (Objects.equals(nodeEntity.getNodeType(), NodeEnum.APPROVER.getType())) {
                        approverMap.put(nodeEntity.getNodeCode(), map);
                        freeNodeName = nodeModel.getNodeName().replaceAll(nodeModel.getNodeId(), "");
                        continue;
                    }
                    flowNodes.put(nodeEntity.getNodeCode(), map);
                }
                String flowXml = jsonEntity.getFlowXml();
                for (Map.Entry<String, Map<String, Object>> stringMapEntry : approverMap.entrySet()) {
                    String key = stringMapEntry.getKey();

                    try {
                        flowXml = URLDecoder.decode(jsonEntity.getFlowXml(), CharsetKit.UTF_8);
                        XmlModel model = FlowXmlUtil.model(flowXml, key, true);
                        String newNodeCode = model.getNewNodeCode();
                        freeParameter.setNodeId(newNodeCode);
                        freeParameter.setNodeName(freeNodeName + newNodeCode);
                        if (Objects.equals(freeParameter.getCounterSign(), FlowNature.IMPROPER_APPROVER)) {
                            freeParameter.setApproversSortList(freeParameter.getApprovers());
                        }
                        flowXml = FlowXmlUtil.xml(model);
                        Map<String, Object> nodeModel = approverMap.get(key);
                        Map<String, Object> parameter = JsonUtil.entityToMap(freeParameter);
                        nodeModel.putAll(parameter);
                        flowNodes.put(newNodeCode, nodeModel);
                        flowModel.setFreeCode(newNodeCode);
                    } catch (Exception e) {
                        log.info(e.getMessage());
                    }

                }


                TemplateNodeUpFrom form = new TemplateNodeUpFrom();
                form.setFlowNodes(flowNodes);
                form.setFlowXml(flowXml);
                form.setId(templateEntity.getId());
                form.setFlowId(flowId);
                form.setIsAddVersion(false);
                flowUtil.create(form);
                String deploymentId = flowAbleUrl.deployFlowAble(form.getFlowXml(), RandomUtil.uuId());

                UpdateWrapper<TemplateJsonEntity> updateWrapper = new UpdateWrapper<>();
                updateWrapper.lambda().eq(TemplateJsonEntity::getId, flowId);
                updateWrapper.lambda().set(TemplateJsonEntity::getState, TemplateJsonStatueEnum.HISTORY.getCode());
                updateWrapper.lambda().set(TemplateJsonEntity::getFlowableId, deploymentId);
                templateJsonMapper.update(updateWrapper);

                //赋值自由流程发布版本
                jsonEntity.setId(flowId);
                jsonEntity.setFlowableId(deploymentId);
                jsonEntity.setVersion(version);
                templateEntity.setFlowId(flowId);
            }
        }

        // 获取节点
        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(jsonEntity.getId());
        if (CollUtil.isEmpty(nodeEntityList)) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }
        Map<String, NodeModel> nodes = new HashMap<>();
        for (TemplateNodeEntity nodeEntity : nodeEntityList) {
            nodes.put(nodeEntity.getNodeCode(), JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class));
        }

        flowModel.setJsonEntity(jsonEntity);
        TaskEntity entity = flowUtil.createEntity(flowModel, templateEntity, nodes);
        // 开始节点
        TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                .filter(e -> StringUtils.equals(NodeEnum.START.getType(), e.getNodeType())).findFirst().orElse(null);
        if (null == nodeEntity) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }
        String nodeCode = nodeEntity.getNodeCode();
        // 传递部署id、节点集合
        String deploymentId = jsonEntity.getFlowableId();
        flowModel.setDeploymentId(deploymentId);
        flowModel.setNodeEntityList(nodeEntityList);
        flowModel.setNodes(nodes);
        flowModel.setNodeEntity(nodeEntity);

        NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
        // 表单主键
        String formId = nodeEntity.getFormId();

        Map<String, Object> data = flowModel.getFormData();
        data.put(FlowFormConstant.FLOWID, jsonEntity.getId());
        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setDeploymentId(deploymentId);
        flowMethod.setNodeCode(nodeCode);
        flowMethod.setFormData(data);
        flowMethod.setNodes(nodes);
        flowMethod.setTaskEntity(entity);

        addTask(ImmutableList.of(entity.getId()));
        FlowFormDataModel model = new FlowFormDataModel();
        model.setFormId(formId);
        model.setId(entity.getId());
        model.setFormOperates(nodeModel.getFormOperates());
        model.setMap(flowModel.getFormData());
        model.setFlowId(jsonEntity.getId());

        if (ObjectUtil.equals(TaskStatusEnum.TO_BE_SUBMIT.getCode(), flowModel.getStatus())) {
            // 生成任务，保存表单数据
            if (Boolean.TRUE.equals(flowModel.getSubFlow())) {
                model.setIsTransfer(true);
            }
            // 菜单入口、编辑
            if (Objects.equals(flowModel.getIsFlow(), 0)) {
                serviceUtil.saveOrUpdateFormData(model);
                entity.setLastModifyTime(new Date());
                entity.setLastModifyUserId(null != userInfo ? userInfo.getUserId() : UserProvider.getLoginUserId());
                taskMapper.updateById(entity);
                flowModel.setTaskEntity(entity);
                return;
            }

            if (StringUtils.isEmpty(flowModel.getId())) {
                entity.setStatus(TaskStatusEnum.TO_BE_SUBMIT.getCode());
                if (taskMapper.insert(entity) > 0) {
                    // 保存表单数据
                    serviceUtil.saveOrUpdateFormData(model);
                }
            } else {
                // 我发起的暂存 直接修改表单数据
                serviceUtil.saveOrUpdateFormData(model);
            }
            flowModel.setTaskEntity(entity);
        } else if (ObjectUtil.equals(TaskStatusEnum.RUNNING.getCode(), flowModel.getStatus())) {
            // 退回到发起
            if (null != entity.getRejectDataId()) {
                serviceUtil.saveOrUpdateFormData(model);
                RejectDataEntity rejectData = rejectDataMapper.getInfo(entity.getRejectDataId());
                String taskJson = rejectData.getTaskJson();

                TaskEntity srcTask = JsonUtil.getJsonToBean(taskJson, TaskEntity.class);
                srcTask.setRejectDataId(rejectData.getId());

                OperatorEntity operator = new OperatorEntity();
                operator.setNodeCode(rejectData.getNodeCode());


                flowModel.setOperatorEntity(operator);
                flowModel.setTaskEntity(srcTask);
                handleRejectData(flowModel);


                // 记录
                flowMethod.setFlowModel(flowModel);
                flowMethod.setType(RecordEnum.SUBMIT.getCode());
                OperatorEntity operatorEntity = new OperatorEntity();
                operatorEntity.setNodeCode(nodeEntity.getNodeCode());
                operatorEntity.setNodeName(nodeModel.getNodeName());
                operatorEntity.setTaskId(entity.getId());
                if (StringUtils.isNotBlank(flowModel.getUserId())) {
                    flowMethod.setHandId(flowModel.getUserId());
                }
                flowMethod.setOperatorEntity(operatorEntity);
                recordMapper.createRecord(flowMethod);

                return;
            }

            // 判断条件
            Map<String, Boolean> resMap = new HashMap<>();
            if (branchList.isEmpty()) {
                resMap.putAll(conditionUtil.handleCondition(flowMethod));
                // 全是false，说明没有分支可走
                conditionUtil.checkCondition(resMap, nodes);
            } else {
                // 选择分支的处理
                resMap.putAll(conditionUtil.getForBranch(flowMethod, branchList));
            }
            // 引擎启动的变量
            Map<String, Object> variables = new HashMap<>(resMap);
            // 生成引擎实例
            String instanceId = entity.getInstanceId();
            if (entity.getInstanceId() == null) {
                taskLineMapper.create(entity.getId(), resMap);
                instanceId = flowAbleUrl.startInstance(jsonEntity.getFlowableId(), variables);
                entity.setInstanceId(instanceId);
            }
            entity.setEngineType(1);
            entity.setStatus(TaskStatusEnum.RUNNING.getCode());
            entity.setStartTime(new Date());
            // 生成任务
            try {
                TaskEntity task = taskMapper.selectById(entity.getId());
                if (null == task) {
                    taskMapper.insert(entity);
                } else {
                    entity.setStatus(TaskStatusEnum.RUNNING.getCode());
                    taskMapper.updateById(entity);
                }
                flowModel.setTaskEntity(entity);
                serviceUtil.saveOrUpdateFormData(model);
                data = serviceUtil.infoData(formId, entity.getId());
                // 表单数据存储
                data.put(FlowFormConstant.FLOWID, jsonEntity.getId());
                FlowContextHolder.addChildData(entity.getId(), nodeEntity.getFormId(), data, nodeModel.getFormOperates(), false);
                flowModel.setFormData(data);
                // 流程参数
                NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
                updateGlobalParam(entity, nodeModel, global, flowModel.getFormData());
                // 记录
                flowMethod.setFlowModel(flowModel);
                flowMethod.setType(RecordEnum.SUBMIT.getCode());
                OperatorEntity operatorEntity = new OperatorEntity();
                operatorEntity.setNodeCode(nodeEntity.getNodeCode());
                operatorEntity.setNodeName(nodeModel.getNodeName());
                operatorEntity.setTaskId(entity.getId());
                if (StringUtils.isNotBlank(flowModel.getUserId())) {
                    flowMethod.setHandId(flowModel.getUserId());
                }
                operatorEntity.setHandleTime(entity.getStartTime());
                flowMethod.setOperatorEntity(operatorEntity);
                recordMapper.createRecord(flowMethod);
                // 节点记录
                NodeRecordModel nodeRecordModel = new NodeRecordModel();
                nodeRecordModel.setTaskId(entity.getId());
                nodeRecordModel.setNodeCode(nodeEntity.getNodeCode());
                nodeRecordModel.setNodeName(nodeModel.getNodeName());
                nodeRecordModel.setNodeStatus(NodeStateEnum.SUBMIT.getCode());
                nodeRecordMapper.create(nodeRecordModel);

                // 保存候选人、异常人
                flowUtil.create(flowModel, entity.getId(), nodeEntityList, null);
                // 保存发起用户信息
                flowUtil.createLaunchUser(entity.getId(), entity.getCreatorUserId());

                // 生成经办
                List<OperatorEntity> operatorEntities = handleOperator(flowModel);

                // 系统审批
                systemAudit();
                // 消息
                FlowMsgModel flowMsgModel = new FlowMsgModel();
                flowMsgModel.setNodeList(nodeEntityList);
                flowMsgModel.setTaskEntity(entity);
                flowMsgModel.setUserInfo(flowModel.getUserInfo());
                flowMsgModel.setOperatorList(operatorEntities);
                flowMsgModel.setFormData(FlowContextHolder.getAllData());
                msgUtil.message(flowMsgModel);

            } catch (WorkFlowException e) {
                // 异常，手动删除实例
                flowAbleUrl.deleteInstance(instanceId, "submitException");
                throw e;
            }
        }
    }

    public List<OperatorEntity> handleOperator(FlowModel flowModel) throws WorkFlowException {
        TaskEntity taskEntity = flowModel.getTaskEntity();
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        Map<String, NodeModel> nodes = flowModel.getNodes();
        Map<String, Object> formData = flowModel.getFormData();
        String instanceId = taskEntity.getInstanceId();
        String deploymentId = flowModel.getDeploymentId();
        NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
        OperatorEntity operatorEntity = flowModel.getOperatorEntity();
        // 最终要返回一个所有节点下的审批人的集合
        List<OperatorEntity> list = new ArrayList<>();

        // 原来的当前节点
        List<String> srcCurrentList = StringUtils.isNotEmpty(taskEntity.getCurrentNodeCode()) ? Arrays.stream(taskEntity.getCurrentNodeCode().split(",")).collect(Collectors.toList()) : new ArrayList<>();

        // 获取引擎当前任务
        List<FlowableTaskModel> taskModelList = flowAbleUrl.getCurrentTask(instanceId);
        if (CollUtil.isEmpty(taskModelList)) {
            return list;
        }
        updateCurrentNode(taskModelList, nodes, taskEntity);
        List<String> types = ImmutableList.of(NodeEnum.SUB_FLOW.getType(), NodeEnum.APPROVER.getType(), NodeEnum.PROCESSING.getType(), NodeEnum.OUTSIDE.getType());
        List<FlowErrorModel> errorList = new ArrayList<>();
        //系统用户
        List<SystemAuditModel> systemList = new ArrayList<>();
        // 生成经办
        for (FlowableTaskModel model : taskModelList) {
            // 当前节点已存在，跳过
            if (CollUtil.isNotEmpty(srcCurrentList) && srcCurrentList.contains(model.getTaskKey())) {
                continue;
            }
            // 获取对应的节点信息
            TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                    .filter(e -> e.getNodeCode().equals(model.getTaskKey())).findFirst().orElse(null);
            if (null == nodeEntity) {
                continue;
            }
            NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
            if (nodeModel == null) {
                continue;
            }
            // 不是审批、子流程、外部、办理节点
            if (!types.contains(nodeModel.getType())) {
                continue;
            }
            FlowMethod flowMethod = new FlowMethod();
            flowMethod.setTaskEntity(taskEntity);
            flowMethod.setNodeEntity(nodeEntity);
            flowMethod.setNodeEntityList(nodeEntityList);
            flowMethod.setFlowModel(flowModel);
            // 流程参数
            updateGlobalParam(taskEntity, nodeModel, global, flowModel.getFormData());
            flowModel.setFlowableTaskId(model.getTaskId());
            List<String> typeList = ImmutableList.of(NodeEnum.SUB_FLOW.getType(), NodeEnum.OUTSIDE.getType());
            // 处理子流程节点、外部节点，传递表单数据
            boolean isOutside = Objects.equals(nodeModel.getType(), NodeEnum.OUTSIDE.getType());
            if (typeList.contains(nodeModel.getType())) {
                if (StringUtils.isEmpty(taskEntity.getRejectDataId()) && CollUtil.isEmpty(flowModel.getErrorRuleUserList())) {
                    FlowMethod method = new FlowMethod();
                    method.setDeploymentId(flowModel.getDeploymentId());
                    method.setNodeCode(nodeModel.getNodeId());
                    method.setNodes(flowModel.getNodes());
                    method.setFormData(flowModel.getFormData());
                    List<NodeModel> nextApprover = flowUtil.getNextApprover(method);
                    int nextError = flowUtil.checkNextError(flowModel, nextApprover, true, true, true);
                    if (nextError == 3) {
                        errorList.addAll(flowModel.getErrorList());
                        flowModel.setErrorList(new ArrayList<>());
                        continue;
                    } else if (nextError == 2) {
                        throw new WorkFlowException(MsgCode.WF061.get());
                    }
                }
                flowModel.setOperatorEntity(operatorEntity);
                if (isOutside) {
                    //外部节点
                    String eventId = RandomUtil.uuId();
                    Map<String, List<TemplateJsonModel>> outsideOptions = nodeModel.getOutsideOptions();
                    String resultNodeCode = flowUtil.resultNodeCode(flowMethod);
                    List<TemplateJsonModel> templateJsonModelList = outsideOptions.get(resultNodeCode) != null ? outsideOptions.get(resultNodeCode) : new ArrayList<>();
                    FlowModel outsideModel = JsonUtil.getJsonToBean(flowModel, FlowModel.class);
                    outsideModel.setNodeCode(nodeModel.getNodeId());
                    TemplateNodeEntity resultNode = nodeEntityList.stream().filter(e -> Objects.equals(e.getNodeCode(), resultNodeCode)).findFirst().orElse(null);
                    if (resultNode != null) {
                        outsideModel.setFormData(flowUtil.infoData(resultNode.getFormId(), taskEntity.getId()));
                    }
                    Map<String, String> parameterData = outsideData(outsideModel, templateJsonModelList, FlowContextHolder.getAllData(), resultNodeCode, eventId);
                    //保存外部节点事件数据
                    EventLogEntity eventLog = new EventLogEntity();
                    eventLog.setId(eventId);
                    eventLog.setTaskId(taskEntity.getId());
                    eventLog.setNodeId(model.getTaskId());
                    eventLog.setNodeName(nodeModel.getNodeName());
                    eventLog.setNodeCode(nodeModel.getNodeId());
                    eventLog.setType(nodeModel.getType());
                    eventLog.setUpNode(resultNodeCode);
                    eventLog.setInterfaceId(nodeModel.getFormId());
                    eventLog.setData(JsonUtil.getObjectToString(formData));
                    eventLog.setStatus(FlowNature.LOSE);
                    if (StringUtils.isNotEmpty(operatorEntity.getHandleId())) {
                        eventLog.setCreatorUserId(operatorEntity.getHandleId());
                    }
                    eventLogMapper.create(eventLog);
                    EventModel event = JsonUtil.getJsonToBean(eventLog, EventModel.class);
                    event.setParameterData(parameterData);
                    FlowEventHolder.addOutsideEvent(event, FlowContextHolder.getAllData());
                } else {
                    //子流程
                    handleSubFlow(nodeEntity, flowModel);
                }
                continue;
            }
            dataTransfer(flowMethod);
            flowMethod.setNodeModel(nodeModel);
            flowMethod.setFormData(formData);
            flowMethod.setDeploymentId(deploymentId);
            flowMethod.setNodeCode(nodeEntity.getNodeCode());
            flowMethod.setNodes(nodes);
            // 节点自动审批
            if (taskEntity.getRejectDataId() == null) {
                flowMethod.setFormData(flowUtil.createOrUpdate(flowMethod));
                int flag = checkAuto(flowMethod);
                if (flag == 1 || flag == 2) {
                    // 判断下一级是否存在候选人，存在候选人返回false
                    List<NodeModel> nextApprover;
                    boolean mark = true;
                    try {
                        nextApprover = flowUtil.getNextApprover(flowMethod);
                    } catch (WorkFlowException e) {
                        nextApprover = null;
                        mark = false;
                    }
                    Map<String, Boolean> resMap = conditionUtil.handleCondition(flowMethod);
                    try {
                        conditionUtil.checkCondition(resMap, nodes);
                    } catch (WorkFlowException e) {
                        mark = false;
                    }
                    // 下一节点的条件判断不满足，不触发系统审批
                    if (mark) {
                        boolean isBranch = flowUtil.checkBranch(nodeModel);
                        int nextError = flowUtil.checkNextError(flowModel, nextApprover, false, false);
                        if (!isBranch && flowUtil.checkNextCandidates(nextApprover) && nextError == 0) {
                            flowModel.setHandleOpinion("系统审批");
                            flowModel.setSignImg(null);
                            flowModel.setFileList(null);
                            flowModel.setHandleStatus(flag == 1 ? FlowNature.AUDIT_COMPLETION : FlowNature.REJECT_COMPLETION);
                            flowMethod.setFlowModel(JsonUtil.getJsonToBean(flowModel, FlowModel.class));
                            flowMethod.setNodeModel(nodeModel);
                            OperatorEntity operator = saveSystemOperator(flowMethod);
                            SystemAuditModel systemAuditModel = new SystemAuditModel();
                            systemAuditModel.setOperator(operator);
                            systemAuditModel.setFlowModel(flowMethod.getFlowModel());
                            systemList.add(systemAuditModel);
                            continue;
                        }
                    }
                }
            }

            // 获取审批人
            flowMethod.setErrorRule(true);
            flowMethod.setExtraRule(true);
            List<String> userIds = flowUtil.userListAll(flowMethod);
            // 获取正常的用户
            List<UserEntity> users = serviceUtil.getUserName(userIds, true);
            // 全局异常处理
            if (users.isEmpty()) {
                // 自动通过
                Integer pass = flowMethod.getPass();
                if (pass > 0) {
                    // 自动审批 要判断候选人等
                    boolean mark = true;
                    List<NodeModel> nextApprover;
                    try {
                        nextApprover = flowUtil.getNextApprover(flowMethod);
                    } catch (WorkFlowException e) {
                        // 异常节点（默认通过）下条件不满足、候选人，走admin兜底方案
                        nextApprover = null;
                        mark = false;
                    }
                    Map<String, Boolean> resMap = conditionUtil.handleCondition(flowMethod);
                    try {
                        conditionUtil.checkCondition(resMap, nodes);
                    } catch (WorkFlowException e) {
                        mark = false;
                    }
                    if (mark && !flowUtil.checkBranch(nodeModel) && flowUtil.checkNextCandidates(nextApprover)) {
                        flowModel.setHandleOpinion(ObjectUtil.equals(nodeModel.getType(), NodeEnum.PROCESSING.getType()) ? "默认办理通过" : "默认审批通过");
                        flowModel.setSignImg(null);
                        flowModel.setFileList(null);
                        flowModel.setHandleStatus(FlowNature.AUDIT_COMPLETION);
                        flowMethod.setFlowModel(JsonUtil.getJsonToBean(flowModel, FlowModel.class));
                        flowMethod.setNodeModel(nodeModel);
                        OperatorEntity operator = saveSystemOperator(flowMethod);
                        SystemAuditModel systemAuditModel = new SystemAuditModel();
                        systemAuditModel.setOperator(operator);
                        systemAuditModel.setFlowModel(flowMethod.getFlowModel());
                        SystemAuditHolder.add(systemAuditModel);
                        continue;
                    } else {
                        String admin = serviceUtil.getAdmin();
                        UserEntity userEntity = serviceUtil.getUserInfo(admin);
                        users.add(userEntity);
                    }
                }
                // 上一节点审批人指定处理人
                Integer node = flowMethod.getNode();
                if (node > 0) {
                    flowUtil.handleErrorRule(nodeModel, errorList);
                }
                // 无法提交
                Integer notSubmit = flowMethod.getNotSubmit();
                if (notSubmit > 0) {
                    throw new WorkFlowException(MsgCode.WF061.get());
                }
            }
            userIds = users.stream().map(UserEntity::getId).collect(Collectors.toList());
            flowMethod.setNodeModel(nodeModel);
            flowMethod.setUserIds(userIds);
            flowMethod.setFlowModel(flowModel);
            List<OperatorEntity> operator = createOperator(flowMethod);
            list.addAll(operator);
            //保存逐级用户数据
            LaunchUserEntity launchUser = flowMethod.getLaunchUser();
            if (launchUser != null) {
                launchUserMapper.insert(launchUser);
            }
        }

        //保存系统用户
        for (SystemAuditModel systemAuditModel : systemList) {
            SystemAuditHolder.add(systemAuditModel);
        }

        if (!errorList.isEmpty()) {
            AuditModel model = new AuditModel();
            model.setErrorCodeList(new HashSet<>(errorList));
            throw new WorkFlowException(200, JsonUtil.getObjectToString(model));
        }
        addOperatorList(list, flowModel);
        return list;
    }

    public CandidateCheckVo checkCandidates(String id, CandidateCheckFo fo) throws WorkFlowException {
        CandidateCheckVo vo = new CandidateCheckVo();
        List<CandidateListModel> list = new ArrayList<>();

        String flowId = fo.getFlowId();
        String nodeCode = null;
        Map<String, Object> formData = fo.getFormData();
        String taskId = fo.getId();

        TemplateEntity template;
        TemplateJsonEntity jsonEntity = templateJsonMapper.selectById(flowId);
        if (null == jsonEntity) {
            template = templateMapper.getInfo(flowId);
            jsonEntity = templateJsonMapper.getInfo(template.getFlowId());
        } else {
            template = templateMapper.getInfo(jsonEntity.getTemplateId());
        }
        if (ObjectUtil.equals(id, FlowNature.PARENT_ID)) {
            if (!ObjectUtil.equals(template.getStatus(), TemplateStatueEnum.UP.getCode())) {
                throw new WorkFlowException(MsgCode.WF140.get());
            }
        } else {
            List<Integer> templateStatus = ImmutableList.of(TemplateStatueEnum.UP.getCode(), TemplateStatueEnum.DOWN_CONTINUE.getCode());
            if (!templateStatus.contains(template.getStatus())) {
                throw new WorkFlowException(MsgCode.WF140.get());
            }
        }
        if (StringUtils.isNotBlank(fo.getDelegateUser())) {
            List<String> launchPermission = serviceUtil.getPermission(fo.getDelegateUser());
            if (ObjectUtil.equals(template.getVisibleType(), FlowNature.AUTHORITY) && !launchPermission.contains(template.getId())) {
                throw new WorkFlowException(MsgCode.WF129.get());
            }
        }

        List<TemplateNodeEntity> nodeEntities = templateNodeMapper.getList(jsonEntity.getId());

        Map<String, NodeModel> nodes = new HashMap<>();
        for (TemplateNodeEntity nodeEntity : nodeEntities) {
            nodes.put(nodeEntity.getNodeCode(), JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class));
        }
        NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());

        TemplateNodeEntity currentNode;
        OperatorEntity operatorEntity = new OperatorEntity();
        // 默认获取开始节点编码
        if (StringUtils.equals(id, FlowNature.PARENT_ID)) {
            currentNode = nodeEntities.stream()
                    .filter(e -> StringUtils.equals(NodeEnum.START.getType(), e.getNodeType())).findFirst().orElse(null);
        } else {
            operatorEntity = operatorMapper.getInfo(id);
            if (operatorEntity == null) {
                throw new WorkFlowException(MsgCode.FA001.get());
            }
            // 加签的经办无需选择候选人
            if (!ObjectUtil.equals(operatorEntity.getParentId(), FlowNature.PARENT_ID)) {
                vo.setList(list);
                return vo;
            }
            String finalNodeCode = operatorEntity.getNodeCode();
            currentNode = nodeEntities.stream().filter(e -> StringUtils.equals(finalNodeCode, e.getNodeCode())).findFirst().orElse(null);
            taskId = operatorEntity.getTaskId();
        }
        RevokeEntity revokeEntity = revokeMapper.getRevokeTask(taskId);
        if (null != revokeEntity) {
            vo.setList(list);
            return vo;
        }
        if (null == currentNode) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }
        nodeCode = currentNode.getNodeCode();
        NodeModel currentNodeModel = nodes.get(nodeCode);

        UserInfo userInfo = UserProvider.getUser();
        // 判断条件
        TaskEntity taskEntity = taskMapper.selectById(taskId);
        if (taskEntity == null) {
            FlowModel flowModel = new FlowModel();
            flowModel.setFlowId(flowId);
            flowModel.setFormData(formData);
            flowModel.setDelegateUser(fo.getDelegateUser());
            flowModel.setUserInfo(userInfo);
            flowModel.setJsonEntity(jsonEntity);
            taskEntity = flowUtil.createEntity(flowModel, template, nodes);
            taskEntity.setStatus(TaskStatusEnum.TO_BE_SUBMIT.getCode());
        }
        if (ObjectUtil.isNotEmpty(taskEntity.getRejectDataId())) {
            return vo;
        }
        getGlobalParam(taskEntity, nodes.get(nodeCode), nodes.get(NodeEnum.GLOBAL.getType()), formData);
        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setDeploymentId(jsonEntity.getFlowableId());
        flowMethod.setNodeCode(nodeCode);
        flowMethod.setFormData(formData);
        flowMethod.setNodes(nodes);
        flowMethod.setTaskEntity(taskEntity);
        flowMethod.setUserInfo(userInfo);
        flowMethod.setAuditFlag(true);
        Map<String, Boolean> resMap = conditionUtil.handleCondition(flowMethod);

        CounterSignConfig counterSignConfig = currentNodeModel.getCounterSignConfig();
        boolean delay = ObjectUtil.equals(counterSignConfig.getCalculateType(), FlowNature.DELAY);

        String divideRule = currentNodeModel.getDivideRule();
        boolean branchFlow = ObjectUtil.equals(DivideRuleEnum.CHOOSE.getType(), divideRule);

        //下一个节点
        NextOrPrevFo nextOrPrevFo = new NextOrPrevFo();
        nextOrPrevFo.setDeploymentId(jsonEntity.getFlowableId());
        nextOrPrevFo.setTaskKey(currentNode.getNodeCode());
        List<FlowableNodeModel> nextModels = flowAbleUrl.getNext(nextOrPrevFo);
        List<String> nextCodes = nextModels.stream().map(FlowableNodeModel::getId).collect(Collectors.toList());
        //赋值
        operatorEntity.setHandleStatus(fo.getHandleStatus());
        flowMethod.setOperatorEntity(operatorEntity);
        flowMethod.setNodeModel(currentNodeModel);
        flowMethod.setHandleStatus(fo.getHandleStatus());
        // 判断比例
        boolean auditRes = checkAudit(flowMethod);
        //自由流程
        if (Objects.equals(template.getType(), FlowNature.FREE)) {
            if (auditRes) {
                boolean isAudit = ObjectUtil.equals(flowMethod.getHandleStatus(), FlowNature.AUDIT_COMPLETION);
                boolean isReject = !isAudit && Boolean.TRUE.equals(global.getHasContinueAfterReject());
                if (isAudit || isReject) {
                    List<NodeModel> nodeModels = new ArrayList<>(nodes.values());
                    List<NodeModel> approverList = nodeModels.stream().filter(e -> Objects.equals(e.getType(), NodeEnum.APPROVER.getType())).collect(Collectors.toList());
                    List<NodeModel> nextList = approverList.stream().filter(e -> nextCodes.contains(e.getNodeId())).collect(Collectors.toList());
                    NodeModel approverNode = approverList.isEmpty() ? new NodeModel() : approverList.get(0);
                    vo.setOneSelfEndApproval(approverNode.getOneSelfEndApproval());
                    int approvaNumber = approverNode.getApprovalNumber();
                    if (Objects.equals(TaskStatusEnum.TO_BE_SUBMIT.getCode(), taskEntity.getStatus())) {
                        vo.setType(4);
                    } else {
                        boolean isNextApprover = approvaNumber > approverList.size() && nextList.isEmpty();
                        if (isNextApprover) {
                            vo.setType(4);
                        }
                    }
                }
            }
            return vo;
        }

        // 拒绝 且 配置拒绝不允许继续流转 直接返回
        if (Boolean.FALSE.equals(global.getHasContinueAfterReject()) && ObjectUtil.equals(fo.getHandleStatus(), FlowNature.REJECT_COMPLETION)) {
            vo.setList(list);
            return vo;
        }
        // 全是false，说明没有分支可走
        if (!branchFlow && !delay) {
            conditionUtil.checkCondition(resMap, nodes);
        } else {
            // 选择分支、且不是最后一个审批人，直接返回
            // 删除原先存在的下一级候选人
            if (CollUtil.isNotEmpty(nextCodes)) {
                candidatesMapper.deleteByCodes(taskId, nextCodes);
            }
            if (!auditRes) {
                return vo;
            }
        }
        List<String> typeList = ImmutableList.of(NodeEnum.EVENT_TRIGGER.getType(), NodeEnum.TIME_TRIGGER.getType(),
                NodeEnum.NOTICE_TRIGGER.getType(), NodeEnum.WEBHOOK_TRIGGER.getType(), NodeEnum.TRIGGER.getType());
        List<String> connectList = global.getConnectList();
        List<String> flows = flowMethod.getOutgoingFlows();
        // 判断排他
        boolean isExclusive = ObjectUtil.equals(currentNodeModel.getDivideRule(), DivideRuleEnum.EXCLUSIVE.getType());
        boolean exclusive = false;
        // 根据true的分支获取节点
        for (String key : flows) {
            if (!connectList.contains(key)) {
                continue;
            }
            if (!branchFlow) {
                if (Boolean.FALSE.equals(resMap.get(key))) {
                    continue;
                }
                if (exclusive) { // 标识为true，说明排他的第一个条件为true
                    break;
                }
                if (isExclusive) {// 排他网关 变更标识
                    exclusive = true;
                }
            }
            // 获取连接线的目标节点
            List<String> nodeKey = flowAbleUrl.getTaskKeyAfterFlow(jsonEntity.getFlowableId(), key);
            if (CollUtil.isEmpty(nodeKey)) {
                continue;
            }
            NodeModel nodeModel = nodes.get(nodeKey.get(0));
            if (null == nodeModel) {
                continue;
            }
            if (typeList.contains(nodeModel.getType())) {
                continue;
            }
            //处理全局的数据传递
            Map<String, Object> dataMap = new HashMap<>();
            for (Map.Entry<String, Object> stringObjectEntry : formData.entrySet()) {
                String dataKey = stringObjectEntry.getKey();
                dataMap.put(dataKey, formData.get(dataKey));
                dataMap.put(dataKey + FlowNature.FORM_FIELD_SUFFIX, formData.get(dataKey));
            }
            FlowMethod jsonToBean = JsonUtil.getJsonToBean(flowMethod, FlowMethod.class);
            jsonToBean.setNodeModel(nodeModel);
            jsonToBean.setNodeEntityList(nodeEntities);
            jsonToBean.setDeploymentId(jsonEntity.getFlowableId());
            List<Assign> assignList = nodeModel.getAssignList().stream().filter(t -> t.getNodeId().equals(currentNode.getNodeCode())).collect(Collectors.toList());
            Map<String, Object> nodeDataAll = flowUtil.formData(dataMap, assignList, taskEntity, jsonToBean);
            getGlobalParam(taskEntity, nodeModel, nodes.get(NodeEnum.GLOBAL.getType()), nodeDataAll);
            operatorEntity.setHandleId(StringUtils.isEmpty(operatorEntity.getHandleId()) ? taskEntity.getCreatorUserId() : operatorEntity.getHandleId());
            jsonToBean.setNodeEntityList(nodeEntities);
            jsonToBean.setNodeEntity(nodeEntities.stream().filter(t -> t.getNodeCode().equals(nodeKey.get(0))).findFirst().orElse(new TemplateNodeEntity()));
            FlowModel flowModel = new FlowModel();
            flowModel.setOperatorEntity(operatorEntity);
            flowModel.setDeploymentId(jsonEntity.getFlowableId());
            flowModel.setFormData(nodeDataAll);
            jsonToBean.setFlowModel(flowModel);
            getCandidateListModel(jsonToBean, list);
            List<String> nextTypeList = ImmutableList.of(NodeEnum.SUB_FLOW.getType());
            if (nextTypeList.contains(nodeModel.getType())) {
                // 判断子流程、外部节点，获取审批节点
                jsonToBean.setNodeCode(nodeModel.getNodeId());
                jsonToBean.setNextSubFlow(true);
                List<NodeModel> nextApprover = flowUtil.getNextApprover(jsonToBean);
                for (NodeModel node : nextApprover) {
                    List<Assign> nodeAssignList = node.getAssignList().stream().filter(t -> t.getNodeId().equals(currentNode.getNodeCode())).collect(Collectors.toList());
                    flowModel.setFormData(flowUtil.formData(dataMap, nodeAssignList, taskEntity, jsonToBean));
                    getGlobalParam(taskEntity, node, nodes.get(NodeEnum.GLOBAL.getType()), nodeDataAll);
                    jsonToBean.setNodeModel(node);
                    jsonToBean.setNodeEntity(nodeEntities.stream().filter(t -> t.getNodeCode().equals(node.getNodeId())).findFirst().orElse(new TemplateNodeEntity()));
                    getCandidateListModel(jsonToBean, list);
                }
            }
        }
        // 获取已选择的候选人
        Integer counterSign = currentNodeModel.getCounterSign();
        if (StringUtils.isNotBlank(taskId) && !ObjectUtil.equals(counterSign, FlowNature.FIXED_APPROVER)) {
            for (CandidateListModel model : list) {
                List<CandidatesEntity> candidates = candidatesMapper.getList(taskId, model.getNodeCode());
                String candidateStr = candidates.stream().map(CandidatesEntity::getCandidates).collect(Collectors.joining(","));
                if (StringUtils.isNotEmpty(candidateStr)) {
                    List<String> selected = new ArrayList<>();
                    List<String> userIds = Arrays.stream(candidateStr.split(",")).distinct().collect(Collectors.toList());
                    List<UserEntity> userList = serviceUtil.getUserName(userIds);
                    for (UserEntity user : userList) {
                        selected.add(user.getRealName() + "/" + user.getAccount());
                    }
                    model.setSelected(String.join(";", selected));
                }
            }
        }
        if (branchFlow) {
            vo.setType(1);
            for (CandidateListModel model : list) {
                model.setIsBranchFlow(true);
            }
        } else {
            CandidateListModel listModel = list.stream().filter(e -> ObjectUtil.equals(e.getIsCandidates(), true)).findFirst().orElse(null);
            if (null != listModel) {
                vo.setType(2);
            }
        }

        vo.setList(list);
        return vo;
    }

    public void getCandidateListModel(FlowMethod flowMethod, List<CandidateListModel> list) throws WorkFlowException {
        NodeModel nodeModel = flowMethod.getNodeModel();
        CandidateListModel listModel = list.stream().filter(e -> ObjectUtil.equals(nodeModel.getNodeId(), e.getNodeCode())).findFirst().orElse(null);
        if (null != listModel) {
            return;
        }
        CandidateListModel model = new CandidateListModel();
        model.setNodeCode(nodeModel.getNodeId());
        model.setNodeName(nodeModel.getNodeName());
        boolean isCandidate = nodeModel.getIsCandidates();
        model.setIsCandidates(isCandidate);
        if (isCandidate) {
            List<String> userIds = flowUtil.userListAll(flowMethod);
            List<UserEntity> users = serviceUtil.getUserName(userIds, true);
            model.setHasCandidates(!users.isEmpty());
            model.setSelectIdList(users.stream().map(UserEntity::getId).collect(Collectors.toList()));
        }
        list.add(model);
    }

    //-------------------------------circulateUtil------------------------------------------------------------

    public List<CirculateEntity> circulateList(FlowMethod flowMethod) throws WorkFlowException {
        FlowModel flowModel = flowMethod.getFlowModel();
        List<CirculateEntity> circulateList = new ArrayList<>();
        TaskEntity task = flowMethod.getTaskEntity();
        OperatorEntity operator = flowMethod.getOperatorEntity();
        TemplateNodeEntity nodeEntity = flowMethod.getNodeEntity();
        NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
        List<String> userIdAll = new ArrayList<>();
        //附加规则
        if (!ObjectUtil.equals(flowMethod.getType(), RecordEnum.REJECT.getCode())) {
            userIdAll = serviceUtil.getUserListAll(nodeModel.getCirculateUser());
            flowUtil.rule(userIdAll, task.getId(), nodeModel.getExtraCopyRule());
            //抄送自己
            if (Boolean.TRUE.equals(nodeModel.getIsInitiatorCopy())) {
                userIdAll.add(task.getCreatorUserId());
            }
            //抄送表单变量
            if (Boolean.TRUE.equals(nodeModel.getIsFormFieldCopy())) {
                flowMethod.setIsAssign(false);
                Map<String, Object> dataAll = flowUtil.createOrUpdate(flowMethod);
                Object data = dataAll.get(nodeModel.getCopyFormField() + FlowNature.FORM_FIELD_SUFFIX);
                if (data != null) {
                    List<String> list = new ArrayList<>();
                    try {
                        list.addAll(JsonUtil.getJsonToList(String.valueOf(data), String.class));
                    } catch (Exception e) {
                        e.getMessage();
                    }
                    if (data instanceof List) {
                        list.addAll((List) data);
                    } else {
                        list.addAll(Arrays.asList(String.valueOf(data).split(",")));
                    }
                    List<String> id = new ArrayList<>();
                    for (String s : list) {
                        id.add(s.split("--")[0]);
                    }
                    List<UserRelationEntity> listByObjectIdAll = serviceUtil.getListByObjectIdAll(id);
                    List<String> userList = serviceUtil.getUserListAll(list);
                    List<String> userPosition = listByObjectIdAll.stream().map(UserRelationEntity::getUserId).collect(Collectors.toList());
                    List<String> handleIdAll = new ArrayList<>();
                    handleIdAll.addAll(userPosition);
                    handleIdAll.addAll(id);
                    handleIdAll.addAll(userList);
                    userIdAll.addAll(handleIdAll);
                }
            }
        }
        //指定传阅人
        userIdAll.addAll(Arrays.asList(StringUtils.isNotEmpty(flowModel.getCopyIds()) ? flowModel.getCopyIds().split(",") : new String[]{}));
        //获取最新用户
        List<UserEntity> userList = serviceUtil.getUserName(userIdAll, true);
        List<String> userIdList = circulateMapper.getNodeList(operator.getTaskId(), operator.getNodeId()).stream().map(CirculateEntity::getUserId).collect(Collectors.toList());
        for (UserEntity userEntity : userList) {
            if (userIdList.contains(userEntity.getId())) {
                continue;
            }
            CirculateEntity circulate = new CirculateEntity();
            circulate.setId(RandomUtil.uuId());
            circulate.setUserId(userEntity.getId());
            circulate.setNodeCode(nodeModel.getNodeId());
            circulate.setNodeName(nodeModel.getNodeName());
            circulate.setTaskId(task.getId());
            circulate.setCirculateRead(0);
            circulate.setOperatorId(operator.getId());
            circulate.setNodeId(operator.getNodeId());
            circulate.setCreatorTime(new Date());
            circulateList.add(circulate);
        }
        circulateMapper.insert(circulateList);
        return circulateList;
    }

    public List<RecordVo> getCirculateList(List<CirculateEntity> list) {
        List<RecordVo> vos = new ArrayList<>();
        if (CollUtil.isNotEmpty(list)) {
            List<UserEntity> userList = serviceUtil.getUserName(list.stream().map(CirculateEntity::getUserId).collect(Collectors.toList()));
            for (CirculateEntity circulate : list) {
                RecordVo vo = JsonUtil.getJsonToBean(circulate, RecordVo.class);
                UserEntity userEntity = userList.stream().filter(t -> t.getId().equals(circulate.getUserId())).findFirst().orElse(null);
                vo.setUserName(userEntity != null ? userEntity.getRealName() + "/" + userEntity.getAccount() : "");
                if (userEntity != null) {
                    vo.setHeadIcon(UploaderUtil.uploaderImg(userEntity.getHeadIcon()));
                }
                vos.add(vo);
            }
        }
        return vos;
    }

    //-------------------------------triggerUtil------------------------------------------------------------

    public List<TriggerDataModel> getTriggerDataModel(TriggerDataFo fo) {
        String modelId = fo.getModelId();
        Integer trigger = fo.getTrigger();
        List<String> dataId = fo.getDataId();
        List<Map<String, Object>> dataMap = fo.getDataMap();
        List<String> updateFields = fo.getUpdateFields();
        List<TriggerDataModel> res = new ArrayList<>();
        Map<String, NodeModel> triggerMap = this.getTriggerNode(modelId, trigger);
        if (CollUtil.isNotEmpty(triggerMap)) {
            for (Map.Entry<String, NodeModel> stringNodeModelEntry : triggerMap.entrySet()) {
                String key = stringNodeModelEntry.getKey();

                NodeModel nodeModel = triggerMap.get(key);
                // 判断指定字段修改
                if (this.checkUpdateField(trigger, nodeModel, updateFields)) {
                    continue;
                }
                if (CollUtil.isNotEmpty(dataMap)) {
                    TriggerDataModel model = new TriggerDataModel();
                    model.setData(JsonUtil.getObjectToString(dataMap));
                    model.setFlowId(key);
                    res.add(model);
                    continue;
                }
                List<Map<String, Object>> dataList = this.getDataList(nodeModel, new HashMap<>(), dataId);
                if (!dataList.isEmpty()) {
                    TriggerDataModel model = new TriggerDataModel();
                    model.setData(JsonUtil.getObjectToString(dataList));
                    model.setFlowId(key);
                    res.add(model);
                }

            }
        }
        return res;
    }

    // true表示 指定字段没有发生修改
    public boolean checkUpdateField(Integer trigger, NodeModel nodeModel, List<String> updateFields) {
        List<String> updateFieldList = nodeModel.getUpdateFieldList();
        if (CollUtil.isEmpty(updateFieldList)) {
            return false;
        }
        if (ObjectUtil.equals(nodeModel.getType(), NodeEnum.EVENT_TRIGGER.getType()) && ObjectUtil.equals(trigger, 2)) {
            // 任意字段发生修改，结果集合为空 说明指定字段（updateFieldList）不存在发生修改的字段（updateFields）中
            List<String> list = updateFieldList.stream().filter(updateFields::contains).collect(Collectors.toList());
            return CollUtil.isEmpty(list);
        }
        return false;
    }

    public Map<String, NodeModel> getTriggerNode(String formId, Integer trigger) {
        QueryWrapper<TemplateNodeEntity> queryWrapper = new QueryWrapper<>();
        List<String> typeList = ImmutableList.of(NodeEnum.EVENT_TRIGGER.getType());
        queryWrapper.lambda().in(TemplateNodeEntity::getNodeType, typeList)
                .eq(TemplateNodeEntity::getFormId, formId);
        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.selectList(queryWrapper);

        List<String> flowIds = new ArrayList<>();
        for (TemplateNodeEntity nodeEntity : nodeEntityList) {
            NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
            if (ObjectUtil.equals(nodeModel.getTriggerFormEvent(), trigger)) {
                flowIds.add(nodeEntity.getFlowId());
            }
        }
        flowIds = flowIds.stream().distinct().collect(Collectors.toList());

        // 获取启用的任务流程id，用于过滤节点
        List<String> filterFlowIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(flowIds)) {
            QueryWrapper<TemplateEntity> wrapper = new QueryWrapper<>();
            wrapper.lambda().in(TemplateEntity::getFlowId, flowIds).eq(TemplateEntity::getType, FlowNature.QUEST);
            List<TemplateEntity> templateList = templateMapper.selectList(wrapper);
            filterFlowIds = templateList.stream().map(TemplateEntity::getFlowId).collect(Collectors.toList());
        }

        // Map<flowId, 触发节点>
        Map<String, NodeModel> map = new HashMap<>();
        for (String flowId : filterFlowIds) {
            TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                    .filter(e -> ObjectUtil.equals(e.getFlowId(), flowId)).findFirst().orElse(null);
            if (null != nodeEntity) {
                map.put(flowId, JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class));
            }
        }
        return map;
    }

    public void handleTimeTrigger(TriggerModel triggerModel) throws WorkFlowException {
        String flowId = triggerModel.getId();
        UserInfo userInfo = triggerModel.getUserInfo();

        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(flowId);
        TemplateNodeEntity timeTrigger = nodeEntityList.stream()
                .filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.TIME_TRIGGER.getType())).findFirst().orElse(null);
        if (null == timeTrigger) {
            return;
        }

        ExecuteModel model = new ExecuteModel();
        model.setFlowId(flowId);
        FlowModel flowModel = new FlowModel();
        flowModel.setUserInfo(userInfo);
        model.setFlowModel(flowModel);
        this.execute(model);
    }

    public void handleTrigger(TriggerDataModel dataModel, UserInfo userInfo) throws WorkFlowException {
        List<Map<String, Object>> dataList = JsonUtil.getJsonToListMap(dataModel.getData());
        ExecuteModel model = new ExecuteModel();
        model.setFlowId(dataModel.getFlowId());
        model.setDataList(dataList);
        FlowModel flowModel = new FlowModel();
        flowModel.setUserInfo(userInfo);
        model.setFlowModel(flowModel);
        this.execute(model);
    }

    // 执行
    public void execute(ExecuteModel model) throws WorkFlowException {
        Boolean nodeRetry = model.getNodeRetry();
        String flowId = model.getFlowId();
        List<Map<String, Object>> dataList = model.getDataList();
        String parentId = model.getParentId();
        TemplateJsonEntity jsonEntity = templateJsonMapper.selectById(flowId);
        if (null != jsonEntity) {
            TemplateEntity template = templateMapper.selectById(jsonEntity.getTemplateId());
            if (template == null) {
                return;
            }
            if (model.getTaskId() == null && !ObjectUtil.equals(template.getStatus(), TemplateStatueEnum.UP.getCode())) {
                return;
            }
            TaskEntity taskEntity = StringUtils.isNotEmpty(model.getTaskId()) ? taskMapper.selectById(model.getTaskId()) : null;
            if (taskEntity != null) {
                model.setTaskEntity(taskEntity);
            }
            model.setDeploymentId(jsonEntity.getFlowableId());
            // 重试判断
            TriggerTaskEntity parentTask = triggerTaskMapper.selectById(parentId);
            boolean isParentId = parentTask != null;

            Map<String, NodeModel> nodes = new HashMap<>();
            List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(flowId);
            for (TemplateNodeEntity nodeEntity : nodeEntityList) {
                nodes.put(nodeEntity.getNodeCode(), JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class));
            }
            model.setNodes(nodes);
            if (Boolean.FALSE.equals(nodeRetry)) {
                // 保存触发任务
                TriggerTaskEntity entity = new TriggerTaskEntity();
                entity.setId(RandomUtil.uuId());
                entity.setFullName(template.getFullName());
                entity.setParentId(isParentId ? parentTask.getId() : parentId);
                entity.setParentTime(isParentId ? parentTask.getStartTime() : null);
                entity.setFlowId(flowId);
                entity.setStartTime(new Date());
                entity.setData(JsonUtil.getObjectToString(dataList));
                entity.setStatus(TaskStatusEnum.RUNNING.getCode());
                entity.setTaskId(StringUtils.isNotEmpty(model.getTaskId()) ? model.getTaskId() : null);
                entity.setNodeCode(StringUtils.isNotEmpty(model.getNodeCode()) ? model.getNodeCode() : null);
                entity.setNodeId(StringUtils.isNotEmpty(model.getNodeId()) ? model.getNodeId() : null);

                model.setTriggerTask(entity);

                String deploymentId = jsonEntity.getFlowableId();
                String instanceId = model.getInstanceId();
                if (StringUtils.isEmpty(instanceId)) {
                    TemplateNodeEntity start = nodeEntityList.stream()
                            .filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.START.getType())).findFirst().orElse(new TemplateNodeEntity());
                    Map<String, Object> variables = this.getVariables(deploymentId, start.getNodeCode(), model);
                    instanceId = flowAbleUrl.startInstance(deploymentId, variables);
                }

                entity.setInstanceId(instanceId);
                model.setInstanceId(instanceId);
                if (null != model.getIsAsync()) {
                    entity.setIsAsync(model.getIsAsync());
                }
                entity.setEngineType(1);


                FlowModel flowModel = model.getFlowModel();
                UserInfo userInfo = flowModel.getUserInfo();
                if (null != userInfo) {
                    entity.setCreatorUserId(userInfo.getUserId());
                }
                triggerTaskMapper.saveTriggerTask(entity);

                if (StringUtils.isEmpty(entity.getTaskId())) {
                    triggerRecordMapper.createStart(entity.getId());
                }

                this.globalMsg(model, 1);
            }
            // 触发节点
            List<String> typeList = ImmutableList.of(NodeEnum.EVENT_TRIGGER.getType(), NodeEnum.TIME_TRIGGER.getType(),
                    NodeEnum.NOTICE_TRIGGER.getType(), NodeEnum.WEBHOOK_TRIGGER.getType(), NodeEnum.TRIGGER.getType());

            String triggerKey = model.getTriggerKey();

            List<String> nodeCodeList = new ArrayList<>();
            if (StringUtils.isNotEmpty(triggerKey)) {
                nodeCodeList.add(triggerKey);
            }
            TemplateNodeEntity triggerNode = nodeEntityList.stream().filter(e -> typeList.contains(e.getNodeType())
                    || nodeCodeList.contains(e.getNodeCode())).findFirst().orElse(null);
            if (null != triggerNode) {
                for (Map<String, Object> map : dataList) {
                    map.put(DataInterfaceVarConst.FORM_ID, map.get(FlowFormConstant.ID));
                }
                executeNode(model);
                TriggerHolder.clear();
            }
        }
    }

    public Map<String, Object> getVariables(String deploymentId, String nodeCode) throws WorkFlowException {
        OutgoingFlowsFo flowsFo = new OutgoingFlowsFo();
        flowsFo.setDeploymentId(deploymentId);
        flowsFo.setTaskKey(nodeCode);
        List<String> outgoingFlows = flowAbleUrl.getOutgoingFlows(flowsFo);
        Map<String, Object> variables = new HashMap<>();
        for (String line : outgoingFlows) {
            variables.put(line, true);
        }
        return variables;
    }

    public Map<String, Object> getVariables(String deploymentId, String nodeCode, ExecuteModel executeModel) throws WorkFlowException {
        TaskEntity taskEntity = executeModel.getTaskEntity();
        FlowModel flowModel = executeModel.getFlowModel();
        Map<String, NodeModel> nodes = executeModel.getNodes();
        TriggerTaskEntity triggerTask = executeModel.getTriggerTask();
        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setFlowModel(flowModel);
        flowMethod.setTaskEntity(taskEntity);
        flowMethod.setDeploymentId(deploymentId);
        flowMethod.setNodeCode(nodeCode);
        flowMethod.setNodes(nodes);

        Map<String, List<Map<String, Object>>> allData = TriggerHolder.getData();
        Map<String, Object> result = getVariables(deploymentId, nodeCode);
        Map<String, Boolean> variables = new HashMap<>();
        for (String line : result.keySet()) {
            NodeModel nodeModel = nodes.get(line);
            boolean lineValue = true;
            if (nodeModel != null) {
                UserEntity user = new UserEntity();
                TaskEntity task = new TaskEntity();
                Map<String, Object> formData = new HashMap<>();
                List<ProperCond> conditions = nodeModel.getConditions();
                for (ProperCond cond : conditions) {
                    List<GroupsModel> groups = cond.getGroups();
                    for (GroupsModel groupsModel : groups) {
                        String field = groupsModel.getField();
                        String jnpfKey = groupsModel.getJnpfKey();
                        String[] fieldSplit = field.split("\\|");
                        if (fieldSplit.length > 1) {
                            List<Map<String, Object>> maps = allData.get(fieldSplit[1]);
                            if (CollUtil.isNotEmpty(maps)) {
                                Map<String, Object> map = maps.get(0);
                                Object oneData = this.getData(map, fieldSplit[0]);
                                formData.put(field, oneData);
                            }
                        }
                        if (Objects.equals(FieldEnum.FIELD.getCode(), groupsModel.getFieldValueType())) {
                            Object fieldValue = groupsModel.getFieldValue();
                            String[] fieldValueSplit = String.valueOf(fieldValue).split("\\|");
                            if (fieldValueSplit.length > 1) {
                                List<Map<String, Object>> maps = allData.get(fieldValueSplit[1]);
                                if (CollUtil.isNotEmpty(maps)) {
                                    Map<String, Object> map = maps.get(0);
                                    Object oneData = this.getData(map, fieldValueSplit[0]);
                                    formData.put(String.valueOf(fieldValue), oneData);
                                }
                            }
                        }

                        Object object = formData.get(field) != null ? formData.get(field) : "";
                        List<String> keyList = ImmutableList.of(JnpfKeyConsts.CURRORGANIZE, JnpfKeyConsts.CURRPOSITION);
                        if (keyList.contains(jnpfKey)) {
                            groupsModel.setJnpfKey(JnpfKeyConsts.COM_INPUT);
                        }
                        if (JnpfKeyConsts.CREATETIME.equals(jnpfKey)) {
                            if (object instanceof Long) {
                                task.setCreatorTime(new Date((Long) object));
                            }
                        } else if (JnpfKeyConsts.CREATEUSER.equals(jnpfKey)) {
                            task.setCreatorUserId(object != null ? object.toString() : "");
                        } else if (JnpfKeyConsts.MODIFYTIME.equals(jnpfKey)) {
                            if (object instanceof Long) {
                                task.setLastModifyTime(new Date((Long) object));
                            }
                        } else if (JnpfKeyConsts.MODIFYUSER.equals(jnpfKey)) {
                            task.setLastModifyUserId(object != null ? object.toString() : "");
                        }
                    }
                }
                flowMethod.setUserEntity(user);
                flowMethod.setTaskEntity(task);
                flowMethod.setFormData(formData);
                flowMethod.setMatchLogic(nodeModel.getMatchLogic());
                flowMethod.setConditions(conditions);
                lineValue = conditionUtil.hasCondition(flowMethod) || conditions.isEmpty();
            }
            variables.put(line, lineValue);
        }
        // 判断节点的线的条件
        conditionUtil.checkCondition(variables, nodes);
        String taskId = StringUtils.isNotEmpty(triggerTask.getTaskId()) ? triggerTask.getTaskId() : triggerTask.getId();
        taskLineMapper.create(taskId, variables);
        return new HashMap<>(variables);
    }

    public void complete(String id, Map<String, Object> variables) throws WorkFlowException {
        CompleteFo fo = new CompleteFo();
        fo.setTaskId(id);
        fo.setVariables(variables);
        flowAbleUrl.complete(fo);
    }

    public void executeNode(ExecuteModel model) throws WorkFlowException {
        String deploymentId = model.getDeploymentId();
        String instanceId = model.getInstanceId();
        Map<String, NodeModel> nodes = model.getNodes();
        TriggerTaskEntity triggerTask = model.getTriggerTask();

        String groupId = model.getGroupId() != null ? model.getGroupId() : "";
        String taskId = model.getTaskId();

        FlowableInstanceModel instance = flowAbleUrl.getInstance(instanceId);
        if (null != instance && null != instance.getEndTime()) {
            triggerRecordMapper.createEnd(triggerTask.getId());
            triggerTask.setStatus(TaskStatusEnum.PASSED.getCode());
            triggerTaskMapper.updateTriggerTask(triggerTask);
            return;
        }

        Map<String, List<Map<String, Object>>> dataMap = TriggerHolder.getData();
        Map<String, List<Map<String, Object>>> nodeDataMap = model.getNodeDataMap() != null ? model.getNodeDataMap() : new HashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> stringListEntry : nodeDataMap.entrySet()) {
            String nodeCode = stringListEntry.getKey();
            List<Map<String, Object>> dataList = dataMap.get(nodeCode);
            if (dataList == null) {
                List<Map<String, Object>> list = nodeDataMap.get(nodeCode);
                if (list != null) {
                    TriggerHolder.addData(nodeCode, list);
                }
            }
        }


        // 任务流程的状态为终止，则结束
        TriggerTaskEntity entity = triggerTaskMapper.selectById(triggerTask.getId());
        if (null != entity && ObjectUtil.equals(entity.getStatus(), TaskStatusEnum.CANCEL.getCode())) {
            return;
        }

        List<String> nodeTypes = ImmutableList.of(NodeEnum.GET_DATA.getType(), NodeEnum.ADD_DATA.getType(), NodeEnum.UPDATE_DATA.getType(), NodeEnum.DELETE_DATA.getType()
                , NodeEnum.TRIGGER.getType(), NodeEnum.EVENT_TRIGGER.getType(), NodeEnum.TIME_TRIGGER.getType(), NodeEnum.NOTICE_TRIGGER.getType(), NodeEnum.WEBHOOK_TRIGGER.getType()
                , NodeEnum.DATA_INTERFACE.getType(), NodeEnum.MESSAGE.getType(), NodeEnum.LAUNCH_FLOW.getType(), NodeEnum.SCHEDULE.getType());

        Map<String, List<NodeModel>> groupMap = model.getGroupMap();

        if (groupMap.isEmpty()) {
            List<NodeModel> nodeList = new ArrayList<>(model.getNodes().values()).stream().filter(e -> nodeTypes.contains(e.getType())).collect(Collectors.toList());
            for (NodeModel nodeModel : nodeList) {
                String nodeGroupId = nodeModel.getGroupId() != null ? nodeModel.getGroupId() : "";
                List<NodeModel> list = groupMap.get(nodeGroupId) != null ? groupMap.get(nodeGroupId) : new ArrayList<>();
                list.add(nodeModel);
                groupMap.put(nodeGroupId, list);
            }
        }

        List<FlowableTaskModel> taskModelListAll = flowAbleUrl.getCurrentTask(instanceId);
        List<NodeModel> list = groupMap.get(groupId) != null ? groupMap.get(groupId) : new ArrayList<>();
        if (list.isEmpty()) {
            return;
        }

        List<String> nodeCodeList = list.stream().map(NodeModel::getNodeId).collect(Collectors.toList());
        List<FlowableTaskModel> taskModelList = taskModelListAll.stream().filter(e -> nodeCodeList.contains(e.getTaskKey())).collect(Collectors.toList());

        if (CollUtil.isNotEmpty(taskModelList)) {
            for (FlowableTaskModel taskModel : taskModelList) {
                String taskKey = taskModel.getTaskKey();
                List<TriggerRecordEntity> recordList = triggerRecordMapper.getList(triggerTask.getId());
                TriggerRecordEntity triggerRecord = recordList.stream().filter(e -> Objects.equals(e.getNodeCode(), taskModel.getTaskKey())).findFirst().orElse(null);
                boolean isRecord = triggerRecord == null;
                NodeModel nodeModel = nodes.get(taskKey);
                TriggerRecordEntity triggerRecordEntity = !isRecord ? triggerRecord : new TriggerRecordEntity();

                //防止重复调用
                if (triggerRecordEntity.getEndTime() != null) {
                    continue;
                }

                if (isRecord) {
                    triggerRecordEntity.setId(RandomUtil.uuId());
                    triggerRecordEntity.setStartTime(new Date());
                    triggerRecordEntity.setStatus(TriggerRecordEnum.WAIT.getCode());
                    triggerRecordEntity.setNodeId(taskModel.getTaskId());
                    triggerRecordEntity.setNodeCode(taskModel.getTaskKey());
                    triggerRecordEntity.setNodeName(nodeModel.getNodeName());
                    triggerRecordEntity.setTriggerId(triggerTask.getId());
                    if (StringUtils.isNotEmpty(triggerTask.getTaskId())) {
                        triggerRecordEntity.setTaskId(triggerTask.getTaskId());
                    }
                    triggerRecordMapper.create(triggerRecordEntity);
                }

                model.setNodeModel(nodeModel);
                model.setCurrentNodeId(taskModel.getTaskId());
                model.setRecordId(triggerRecordEntity.getId());

                NodeEnum node = NodeEnum.getNode(nodeModel.getType());
                try {
                    if (isRecord) {
                        switch (node) {
                            case TRIGGER:
                            case EVENT_TRIGGER:
                            case TIME_TRIGGER:
                            case NOTICE_TRIGGER:
                            case WEBHOOK_TRIGGER:
                                List<Map<String, Object>> dataList = model.getDataList();
                                TriggerHolder.addData(taskKey, dataList);
                                break;
                            case GET_DATA:
                                List<Map<String, Object>> data = this.getData(model);
                                TriggerHolder.addData(taskKey, data);
                                break;
                            case ADD_DATA:
                                this.addData(model);
                                break;
                            case UPDATE_DATA:
                                this.updateData(model);
                                break;
                            case DELETE_DATA:
                                this.deleteData(model);
                                break;
                            case MESSAGE:
                                this.message(model);
                                break;
                            case LAUNCH_FLOW:
                                this.launchFlow(model);
                                break;
                            case DATA_INTERFACE:
                                this.dataInterface(model);
                                break;
                            case SCHEDULE:
                                this.createSchedule(model);
                                break;
                            default:
                                break;
                        }
                    }
                    triggerRecordEntity.setStatus(TriggerRecordEnum.PASSED.getCode());
                    List<TriggerLaunchflowEntity> triggerList = triggerLaunchflowMapper.getTriggerList(ImmutableList.of(triggerTask.getId()));
                    List<String> launchFlow = triggerList.stream().map(TriggerLaunchflowEntity::getNodeCode).collect(Collectors.toList());
                    if (launchFlow.contains(taskModel.getTaskKey())) {
                        continue;
                    }
                } catch (Exception e) {
                    String msg = this.getErrMsg(e);
                    String errorMsg = msg;
                    if (e instanceof WorkFlowException) {
                        WorkFlowException workException = ((WorkFlowException) e);
                        errorMsg = workException.getDataMsg() != null ? workException.getDataMsg() : errorMsg;
                    }
                    triggerRecordEntity.setErrorTip(msg);
                    triggerRecordEntity.setErrorData(errorMsg);
                    triggerRecordEntity.setStatus(TriggerRecordEnum.EXCEPTION.getCode());
                }
                triggerRecordEntity.setEndTime(new Date());
                boolean isException = ObjectUtil.equals(triggerRecordEntity.getStatus(), TriggerRecordEnum.EXCEPTION.getCode());
                if (!isException) {
                    if (StringUtils.isBlank(taskId)) {
                        try {
                            Map<String, Object> variables = this.getVariables(deploymentId, taskModel.getTaskKey(), model);
                            this.complete(taskModel.getTaskId(), variables);
                        } catch (WorkFlowException e) {
                            String msg = this.getErrMsg(new WorkFlowException(MsgCode.WF159.get()));
                            String errorMsg = e.getDataMsg() != null ? e.getDataMsg() : msg;
                            triggerRecordEntity.setErrorTip(msg);
                            triggerRecordEntity.setErrorData(errorMsg);
                            triggerRecordEntity.setStatus(TriggerRecordEnum.EXCEPTION.getCode());
                        }
                    } else {
                        // 嵌入的任务流程，最后的执行节点不能通过，否则退回会有问题
                        NextOrPrevFo fo = new NextOrPrevFo();
                        fo.setDeploymentId(deploymentId);
                        fo.setTaskKey(taskModel.getTaskKey());
                        List<FlowableNodeModel> next = flowAbleUrl.getNext(fo);
                        if (CollUtil.isNotEmpty(next)) {
                            try {
                                Map<String, Object> variables = this.getVariables(deploymentId, taskModel.getTaskKey(), model);
                                this.complete(taskModel.getTaskId(), variables);
                            } catch (WorkFlowException e) {
                                String msg = this.getErrMsg(new WorkFlowException(MsgCode.WF159.get()));
                                String errorMsg = e.getDataMsg() != null ? e.getDataMsg() : msg;
                                triggerRecordEntity.setErrorTip(msg);
                                triggerRecordEntity.setErrorData(errorMsg);
                                triggerRecordEntity.setStatus(TriggerRecordEnum.EXCEPTION.getCode());
                            }
                        }
                    }
                }
                triggerRecordMapper.updateById(triggerRecordEntity);
                if (ObjectUtil.equals(triggerRecordEntity.getStatus(), TriggerRecordEnum.EXCEPTION.getCode())) {
                    this.globalMsg(model, 2);
                    if (!ObjectUtil.equals(triggerTask.getStatus(), TaskStatusEnum.EXCEPTION.getCode())) {
                        triggerTask.setStatus(TaskStatusEnum.EXCEPTION.getCode());
                        triggerTaskMapper.updateTriggerTask(triggerTask);
                        model.setTriggerTask(triggerTask);
                    }
                    return;
                }
                this.executeNode(model);
            }
        }
    }

    private String getErrMsg(Exception e) {
        ActionResult<Object> result = new ActionResult<>();
        result.setCode(400);
        result.setMsg(e.getMessage());
        return JsonUtil.getObjectToString(result);
    }

    // -----------------------------------------------------------------------------
    // 获取数据
    public List<Map<String, Object>> getData(ExecuteModel model) throws WorkFlowException {
        List<Map<String, Object>> dataList = new ArrayList<>();

        NodeModel nodeModel = model.getNodeModel();
        Integer formType = nodeModel.getFormType();

        switch (formType) {
            case 1:
            case 2:
                List<Map<String, Object>> list = this.getDataList(nodeModel, new HashMap<>(), new ArrayList<>());
                dataList.addAll(list);
                break;
            case 3:
                List<ActionResult<Object>> resultList = this.interfaceTemplateJson(model, new HashMap<>());
                List<String> errList = new ArrayList<>();
                for (ActionResult<Object> result : resultList) {
                    if (!ObjectUtil.equals(result.getCode(), 200)) {
                        errList.add(result.getMsg());
                        continue;
                    }
                    if (result.getData() instanceof List) {
                        List<Map<String, Object>> maps = this.castListMap(result.getData());
                        if (CollUtil.isNotEmpty(maps)) {
                            dataList.addAll(maps);
                        }
                    }
                }
                if (CollUtil.isNotEmpty(errList)) {
                    throw new WorkFlowException(errList.get(0));
                }
                break;
            case 4:
                List<Map<String, Object>> data = this.getDataList(nodeModel, new HashMap<>(), new ArrayList<>(), true);
                String subTable = nodeModel.getSubTable();
                for (Map<String, Object> map : data) {
                    Object obj = map.get(subTable);
                    if (null != obj) {
                        List<Map<String, Object>> mapList = this.castListMap(obj);
                        List<Map<String, Object>> resList = new ArrayList<>();
                        for (Map<String, Object> objectMap : mapList) {
                            Map<String, Object> resMap = new HashMap<>();
                            for (Map.Entry<String, Object> stringObjectEntry : objectMap.entrySet()) {
                                String key = stringObjectEntry.getKey();
                                if (ObjectUtil.equals(key, FlowFormConstant.ID)) {
                                    resMap.put(key, objectMap.get(key));
                                } else {
                                    resMap.put(subTable + "-" + key, objectMap.get(key));
                                }
                            }

                            resMap.put(FlowNature.SUB_TABLE, subTable);
                            resList.add(resMap);
                        }
                        dataList.addAll(resList);
                    }
                }
                break;
            default:
                break;
        }
        for (Map<String, Object> map : dataList) {
            map.put(DataInterfaceVarConst.FORM_ID, map.get(FlowFormConstant.ID));
        }
        return dataList;
    }

    public List<Map<String, Object>> castListMap(Object obj) {
        return this.castListMap(obj, String.class, Object.class);
    }

    // 对象转Map类型的List
    public <K, V> List<Map<K, V>> castListMap(Object obj, Class<K> kClazz, Class<V> vClazz) {
        List<Map<K, V>> result = new ArrayList<>();
        if (obj instanceof List<?>) {
            for (Object o : (List<?>) obj) {
                if (o instanceof Map<?, ?>) {
                    Map<K, V> map = new HashMap<>(16);
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) o).entrySet()) {
                        map.put(kClazz.cast(entry.getKey()), vClazz.cast(entry.getValue()));
                    }
                    result.add(map);
                }
            }
        }
        return result;
    }

    // 接口获取数据
    public List<ActionResult<Object>> interfaceTemplateJson(ExecuteModel executeModel, Map<String, Object> data) throws WorkFlowException {
        NodeModel nodeModel = executeModel.getNodeModel();
        TaskEntity taskEntity = executeModel.getTaskEntity();
        String globalParameter = taskEntity.getGlobalParameter();
        Map<String, Object> parameterData = StringUtils.isNotEmpty(globalParameter) ? JsonUtil.stringToMap(globalParameter) : new HashMap<>();
        String interId = nodeModel.getFormId();
        List<IntegrateTplModel> templateJson = nodeModel.getTemplateJson();
        boolean isGetData = ObjectUtil.equals(nodeModel.getType(), NodeEnum.GET_DATA.getType());
        if (isGetData) {
            templateJson = nodeModel.getInterfaceTemplateJson();
        }
        Map<String, String> parameterMap = new HashMap<>();
        Set<String> msg = new HashSet<>();
        Map<String, List<Map<String, Object>>> allData = TriggerHolder.getData();
        String dataSourceForm = nodeModel.getDataSourceForm();
        List<Map<String, Object>> maps = allData.get(dataSourceForm);
        List<ActionResult<Object>> resultList = new ArrayList<>();
        if (StringUtils.isBlank(dataSourceForm)) {
            maps = new ArrayList<>();
            maps.add(new HashMap<>());
        }
        if (CollUtil.isNotEmpty(maps)) {
            List<String> paramList = ImmutableList.of(
                    DataInterfaceVarConst.USER,
                    DataInterfaceVarConst.USERANDSUB,
                    DataInterfaceVarConst.USERANDPROGENY,
                    DataInterfaceVarConst.ORG,
                    DataInterfaceVarConst.ORGANDSUB,
                    DataInterfaceVarConst.ORGANIZEANDPROGENY,
                    DataInterfaceVarConst.POSITIONID,
                    DataInterfaceVarConst.POSITIONANDSUB,
                    DataInterfaceVarConst.POSITIONANDPROGENY);
            Map<String, String> paramDataMap = serviceUtil.getSystemFieldValue();
            for (Map<String, Object> map : maps) {
                Map<String, Object> tempMap = new HashMap<>(data);
                tempMap.putAll(map);
                for (IntegrateTplModel tpl : templateJson) {
                    String fieldId = tpl.getField();
                    Boolean required = tpl.getRequired();
                    Integer sourceType = tpl.getSourceType();
                    String relationField = tpl.getRelationField();
                    Map<String, Object> dataMap = new HashMap<>(tempMap);
                    String[] split = relationField.split("\\|");
                    String dataValue;
                    if (isGetData) {
                        dataValue = this.getFieldValue(relationField, FieldEnum.FIELD.getCode(), allData);
                    } else {
                        dataValue = this.getStrData(dataMap, split[0]);
                    }
                    if (DataInterfaceVarConst.FORM_ID.equals(relationField)) {
                        dataValue = String.valueOf(dataMap.get(FlowFormConstant.ID));
                    } else if (paramList.contains(relationField)) {
                        List<String> dataList = new ArrayList<>();
                        String value = paramDataMap.get(relationField);
                        if (StringUtils.isNotEmpty(value)) {
                            try {
                                List<String> list = JsonUtil.getJsonToList(value, String.class);
                                dataList.addAll(list);
                            } catch (Exception e) {
                                dataList.add(value);
                            }
                        }
                        dataValue = String.join(",", dataList);
                    }
                    String dataFieldValue = split[0];
                    String parameterValue = parameterData.get(relationField) != null ? String.valueOf(parameterData.get(relationField)) : null;
                    String s = Objects.equals(FieldEnum.GLOBAL.getCode(), sourceType) ? parameterValue : dataValue;
                    String dataJson = !ObjectUtil.equals(tpl.getSourceType(), FieldEnum.CUSTOM.getCode()) ? s : dataFieldValue;
                    String[] model = StringUtils.isNotEmpty(split[0]) ? split[0].split("-") : new String[]{};
                    if (model.length > 1) {
                        Object dataList = dataMap.get(model[0]);
                        if (dataList instanceof List) {
                            List<Map<String, Object>> listAll = (List<Map<String, Object>>) dataList;
                            List<Object> dataListAll = new ArrayList<>();
                            for (Map<String, Object> objectMap : listAll) {
                                dataListAll.add(objectMap.get(model[1] + FlowNature.FORM_FIELD_SUFFIX));
                            }
                            if (Boolean.TRUE.equals(required) && ObjectUtil.isEmpty(dataListAll)) {
                                msg.add(fieldId);
                            }
                            dataJson = String.valueOf(dataListAll);
                        } else {
                            // 子表数据的处理
                            dataJson = this.getStrData(dataMap, split[0]);
                        }
                    }
                    if (Boolean.TRUE.equals(required) && ObjectUtil.isEmpty(dataJson)) {
                        msg.add(fieldId);
                    }
                    parameterMap.put(fieldId, dataJson);
                }
                this.errRequiredMsg(msg);
                ActionResult<Object> result = serviceUtil.infoToId(interId, parameterMap);
                if (!ObjectUtil.equals(result.getCode(), 200)) {
                    resultList.add(result);
                    return resultList;
                }
                if (result.getData() instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) result.getData();
                    Integer code = (Integer) dataMap.get("code");
                    if (ObjectUtil.equals(code, 400)) {
                        ActionResult<Object> ar = new ActionResult<>();
                        ar.setCode(code);
                        ar.setMsg((String) dataMap.get("msg"));
                        resultList.add(ar);
                        return resultList;
                    }
                }
                resultList.add(result);
            }
        }
        return resultList;
    }

    private void errRequiredMsg(Set<String> msg) throws WorkFlowException {
        if (!msg.isEmpty()) {
            throw new WorkFlowException(new ArrayList<>(msg).get(0) + MsgCode.VS015.get());
        }
    }

    public List<Map<String, Object>> getDataList(NodeModel nodeModel, Map<String, Object> data, List<String> dataId) {
        return this.getDataList(nodeModel, data, dataId, false);
    }

    private List<Map<String, Object>> getDataList(NodeModel nodeModel, Map<String, Object> data, List<String> dataId, Boolean isSub) {
        boolean idDelete = Objects.equals(nodeModel.getType(), NodeEnum.DELETE_DATA.getType());
        String formId = nodeModel.getFormId();
        List<SuperQueryJsonModel> ruleList = nodeModel.getRuleList();
        String ruleMatchLogic = nodeModel.getRuleMatchLogic();
        String flowId = "";
        boolean isFlow = false;
        if (ObjectUtil.equals(nodeModel.getFormType(), 2)) {
            isFlow = true;
            flowId = formId;
            TemplateEntity template = templateMapper.selectById(flowId);
            if (null != template) {
                List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(template.getFlowId());
                TemplateNodeEntity startNode = nodeEntityList.stream()
                        .filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.START.getType())).findFirst().orElse(null);
                if (null != startNode) {
                    formId = startNode.getFormId();
                }
            }
        }
        VisualdevEntity visualdevEntity = serviceUtil.getReleaseInfo(formId);
        if (Objects.equals(visualdevEntity.getType(), 2)) {
            return new ArrayList<>();
        }

        // 条件选择
        for (SuperQueryJsonModel superQueryJsonModel : ruleList) {
            List<FieLdsModel> groups = superQueryJsonModel.getGroups();
            for (FieLdsModel fieLdsModel : groups) {
                if (ObjectUtil.equals(fieLdsModel.getFieldValueType(), String.valueOf(FieldEnum.FIELD.getCode()))
                        && null != fieLdsModel.getFieldValue()) {
                    // 字段名 | 节点编码

                    String[] split = fieLdsModel.getFieldValue().split("\\|");
                    if (split.length > 1) {
                        if (CollUtil.isNotEmpty(data)) {
                            String obj = this.getStrData(data, split[0]);
                            if (null != obj) {
                                fieLdsModel.setFieldValue(obj);
                            }
                        } else {
                            Map<String, List<Map<String, Object>>> allData = TriggerHolder.getData();
                            List<Map<String, Object>> maps = allData.get(split[1]);
                            if (CollUtil.isNotEmpty(maps)) {
                                Map<String, Object> map = maps.get(0);
                                String oneData = this.getStrData(map, split[0]);
                                fieLdsModel.setFieldValue(oneData);
                            }
                        }
                    }

                }

                //表单id
                if (Objects.equals(DataInterfaceVarConst.FORM_ID, fieLdsModel.getField())) {
                    List<TableModel> tableModelList = JsonUtil.getJsonToList(visualdevEntity.getVisualTables(), TableModel.class);
                    TableModel mainTable = tableModelList.stream().filter(t -> t.getTypeId().equals("1")).findFirst().orElse(null);
                    if (mainTable != null) {
                        TableFields tableFields = mainTable.getFields().stream().filter(t -> Objects.equals(t.getPrimaryKey(), 1)
                                && !t.getField().toLowerCase().contains(TableFeildsEnum.TENANTID.getField())).findFirst().orElse(null);
                        if (tableFields != null) {
                            String field = tableFields.getField();
                            String table = mainTable.getTable();
                            ConfigModel config = new ConfigModel();
                            config.setTableName(table);
                            fieLdsModel.setVModel(field);
                            fieLdsModel.setConfig(config);
                        }
                    }
                }
            }
        }

        SuperJsonModel superJsonModel = new SuperJsonModel();
        superJsonModel.setConditionList(ruleList);
        superJsonModel.setMatchLogic(StringUtils.isNotEmpty(ruleMatchLogic) ? ruleMatchLogic : superJsonModel.getMatchLogic());
        PaginationModel paginationModel = new PaginationModel();
        paginationModel.setDataType("1");
        paginationModel.setSuperQueryJson(!ruleList.isEmpty() ? JsonUtil.getObjectToString(superJsonModel) : "");
        String sidx = this.handleSort(nodeModel.getSortList());
        paginationModel.setSidx(sidx);

        VisualDevJsonModel visualJsonModel = this.getVisualJsonModel(visualdevEntity);
        List<String> idAll = new ArrayList<>();
        List<String> idList = new ArrayList<>();
        List<Map<String, Object>> dataList = new ArrayList<>();
        try {
            if (!ruleList.isEmpty()) {
                visualJsonModel.setSuperQuery(superJsonModel);
            }
            if (isFlow) {
                List<String> flowVersionIds = templateJsonMapper.getListByTemplateIds(ImmutableList.of(flowId)).stream().map(TemplateJsonEntity::getId).collect(Collectors.toList());
                visualJsonModel.setFlowVersionIds(flowVersionIds);
                visualJsonModel.setEnableFlow(!flowVersionIds.isEmpty());
            }
            dataList.addAll(serviceUtil.getListWithTableList(visualJsonModel, paginationModel, UserProvider.getUser()));
            if (Boolean.TRUE.equals(isSub)) {
                return dataList;
            }
            idList.addAll(dataList.stream().map(t -> String.valueOf(t.get(FlowFormConstant.ID))).collect(Collectors.toList()));
        } catch (Exception e) {
            OperatorUtil.log.info("数据获取异常:" + e);
        }
        List<String> intersection = idList.stream().filter(dataId::contains).collect(Collectors.toList());
        if (!dataId.isEmpty()) {
            idAll.addAll(intersection);
        } else {
            idAll.addAll(idList);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String id : idAll) {
            List<Map<String, Object>> collect = dataList.stream().filter(t -> Objects.equals(String.valueOf(t.get(FlowFormConstant.ID)), id)).collect(Collectors.toList());
            for (Map<String, Object> map : collect) {
                boolean isAdd = true;
                if (isFlow && !idDelete) {
                    TaskEntity infoSubmit = taskMapper.getInfoSubmit(String.valueOf(map.get(FlowFormConstant.FLOWTASKID)), TaskEntity::getEndTime);
                    isAdd = infoSubmit != null && infoSubmit.getEndTime() != null;
                }
                if (isAdd) {
                    result.add(map);
                }
            }
        }
        return result;
    }

    public VisualDevJsonModel getVisualJsonModel(VisualdevEntity entity) {
        VisualDevJsonModel jsonModel = new VisualDevJsonModel();
        if (entity == null) {
            return jsonModel;
        }
        if (entity.getColumnData() != null) {
            jsonModel.setColumnData(JsonUtil.getJsonToBean(entity.getColumnData(), ColumnDataModel.class));
        }
        if (entity.getAppColumnData() != null) {
            jsonModel.setAppColumnData(JsonUtil.getJsonToBean(entity.getAppColumnData(), ColumnDataModel.class));
        }
        FormDataModel formDataModel = JsonUtil.getJsonToBean(entity.getFormData(), FormDataModel.class);
        jsonModel.setFormData(formDataModel);
        if (!VisualWebTypeEnum.DATA_VIEW.getType().equals(entity.getWebType())) {
            jsonModel.setFormListModels(JsonUtil.getJsonToList(formDataModel.getFields(), FieLdsModel.class));
        }
        jsonModel.setVisualTables(JsonUtil.getJsonToList(entity.getVisualTables(), TableModel.class));
        jsonModel.setId(entity.getId());
        jsonModel.setDbLinkId(entity.getDbLinkId());
        jsonModel.setFullName(entity.getFullName());
        jsonModel.setType(entity.getType());
        jsonModel.setWebType(entity.getWebType());
        return jsonModel;
    }

    private String handleSort(List<SortModel> sortList) {
        List<String> list = new ArrayList<>();
        for (SortModel sortModel : sortList) {
            String field = sortModel.getField();
            String sortType = sortModel.getSortType();
            if (!ObjectUtil.equals(sortType, "asc")) {
                field = "-" + field;
            }
            list.add(field);
        }
        return String.join(",", list);
    }

    public String getStrData(Map<String, Object> map, String key) {
        Object oneData = getData(map, key);
        if (null != oneData) {
            if (oneData instanceof String) {
                return oneData.toString();
            } else {
                return JsonUtil.getObjectToString(oneData);
            }
        }
        return null;
    }

    public Object getData(Map<String, Object> map, String key) {
        return flowUtil.getOneData(map, key);
    }

    // 数据接口  --------------------------
    public List<Map<String, Object>> dataInterface(ExecuteModel model) throws WorkFlowException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        List<ActionResult<Object>> resultList = this.interfaceTemplateJson(model, new HashMap<>());
        List<String> errList = new ArrayList<>();
        for (ActionResult<Object> result : resultList) {
            if (!ObjectUtil.equals(result.getCode(), 200)) {
                errList.add(result.getMsg());
                continue;
            }
            if (result.getData() instanceof List) {
                List<Map<String, Object>> maps = this.castListMap(result.getData());
                if (CollUtil.isNotEmpty(maps)) {
                    dataList.addAll(maps);
                }
            }
        }
        if (CollUtil.isNotEmpty(errList)) {
            throw new WorkFlowException(errList.get(0));
        }
        return dataList;
    }

    // 消息  -----------------------------
    public void message(ExecuteModel model) throws WorkFlowException {
        NodeModel nodeModel = model.getNodeModel();
        if (null != nodeModel) {
            String msgId = nodeModel.getMsgId();
            List<IntegrateTplModel> templateJson = nodeModel.getMsgTemplateJson();
            FlowModel flowModel = model.getFlowModel();
            UserInfo userInfo = flowModel.getUserInfo();
            Map<String, List<Map<String, Object>>> allData = TriggerHolder.getData();
            Map<String, Object> parameterMap = this.templateJson(templateJson, allData, userInfo);

            List<String> userIds = this.getUserList(nodeModel.getMsgUserIds(), nodeModel.getMsgUserIdsSourceType(), allData);
            this.message(msgId, userIds, parameterMap, userInfo);
        }
    }

    private void message(String msgId, List<String> userIdList, Map<String, Object> parameterMap, UserInfo userInfo) throws WorkFlowException {
        List<SentMessageForm> formList = new ArrayList<>();
        SentMessageForm sentMessageForm = new SentMessageForm();
        sentMessageForm.setUserInfo(userInfo);
        sentMessageForm.setTemplateId(msgId);
        sentMessageForm.setToUserIds(userIdList);
        sentMessageForm.setParameterMap(parameterMap);
        sentMessageForm.setType(3);
        sentMessageForm.setContentMsg(new HashMap<>());
        sentMessageForm.setId(msgId);
        sentMessageForm.setSysMessage(true);
        formList.add(sentMessageForm);
        List<String> errList = serviceUtil.sendDelegateMsg(formList);
        if (CollUtil.isNotEmpty(errList)) {
            throw new WorkFlowException(errList.get(0));
        }
    }

    private Map<String, Object> templateJson(List<IntegrateTplModel> templateJson, Map<String, List<Map<String, Object>>> data, UserInfo userInfo) throws WorkFlowException {
        Map<String, Object> parameterMap = new HashMap<>();
        Set<String> msg = new HashSet<>();

        for (IntegrateTplModel tplModel : templateJson) {
            List<IntegrateParamModel> paramJson = tplModel.getParamJson();
            for (IntegrateParamModel paramModel : paramJson) {
                String field = paramModel.getField();
                String relationField = paramModel.getRelationField() == null ? "" : paramModel.getRelationField();
                if (ObjectUtil.equals(paramModel.getSourceType(), FieldEnum.CUSTOM.getCode())) {
                    // 参数来源为自定义
                    parameterMap.put(paramModel.getMsgTemplateId() + field, relationField);
                    continue;
                }
                Boolean required = paramModel.getRequired();
                String[] split = relationField.split("\\|");
                if (split.length < 2) {
                    continue;
                }
                List<Map<String, Object>> mapList = data.get(split[1]);
                Map<String, Object> map = CollUtil.isNotEmpty(mapList) ? mapList.get(0) : new HashMap<>();
                if (map == null) {
                    map = new HashMap<>();
                }
                String dataValue = "";
                if (DataInterfaceVarConst.FORM_ID.equals(split[0])) {
                    dataValue = map.get(FlowFormConstant.ID) != null ? String.valueOf(map.get(FlowFormConstant.ID)) : "";
                } else {
                    if (map.get(split[0]) != null) {
                        dataValue = String.valueOf(map.get(split[0]));
                    }
                }
                if (Boolean.TRUE.equals(required) && ObjectUtil.isEmpty(dataValue)) {
                    msg.add(field);
                }
                parameterMap.put(paramModel.getMsgTemplateId() + field, dataValue);
            }
            Map<String, String> paramMap = ImmutableMap.of(FlowConstant.CREATORUSERNAME, userInfo.getUserName(), FlowConstant.SENDTIME, DateUtil.getNow().substring(11));
            for (Map.Entry<String, String> stringStringEntry : paramMap.entrySet()) {
                String key = stringStringEntry.getKey();
                parameterMap.put(tplModel.getId() + key, paramMap.get(key));
            }


        }
        this.errRequiredMsg(msg);
        return parameterMap;
    }

    // 新增数据 -------------------
    public void addData(ExecuteModel model) throws WorkFlowException {
        NodeModel nodeModel = model.getNodeModel();
        if (null == nodeModel) {
            return;
        }
        String formId = nodeModel.getFormId();
        List<TemplateJsonModel> transferList = nodeModel.getTransferList();
        String dataSourceForm = nodeModel.getDataSourceForm();
        List<SuperQueryJsonModel> ruleList = nodeModel.getRuleList();

        VisualdevEntity visualdevEntity = serviceUtil.getReleaseInfo(formId);
        if (StringUtils.isBlank(dataSourceForm)) {
            // 数据源没有数据时，将字段设置中的自定义添加到数据中
            transferList = transferList.stream().filter(e -> ObjectUtil.equals(e.getSourceType(), 2)).collect(Collectors.toList());
            Map<String, Object> data = new HashMap<>();
            for (TemplateJsonModel TemplateJsonModel : transferList) {
                data.put(TemplateJsonModel.getTargetField(), TemplateJsonModel.getSourceValue());
            }
            if (CollUtil.isNotEmpty(data)) {
                serviceUtil.visualCreate(visualdevEntity, data);
            }
            return;
        }

        // 数据源的数据
        Map<String, List<Map<String, Object>>> allData = TriggerHolder.getData();
        List<Map<String, Object>> maps = allData.get(dataSourceForm) != null ? allData.get(dataSourceForm) : new ArrayList<>();

        for (Map<String, Object> map : maps) {
            List<Map<String, Object>> dataList = new ArrayList<>();
            if (CollUtil.isNotEmpty(ruleList)) {
                dataList = this.getDataList(nodeModel, map, new ArrayList<>());
            }
            if (dataList.isEmpty()) {
                List<Map<String, Object>> list = this.handleAddData(map, transferList);
                if (CollUtil.isNotEmpty(list)) {
                    for (Map<String, Object> saveData : list) {
                        serviceUtil.visualCreate(visualdevEntity, saveData);
                    }
                }
            }
        }
    }

    public List<Map<String, Object>> handleAddData(Map<String, Object> data, List<TemplateJsonModel> transferList) {
        return this.handleData(data, new ArrayList<>(), transferList);
    }

    // 将后缀为_jnpfId的值赋给原字段
    public Map<String, Object> setData(Map<String, Object> data) {
        data = CollUtil.isNotEmpty(data) ? data : new HashMap<>();
        for (Map.Entry<String, Object> stringObjectEntry : data.entrySet()) {
            String key = stringObjectEntry.getKey();

            Object o = data.get(key);
            Object o1 = data.get(key + FlowNature.FORM_FIELD_SUFFIX);
            if (o != null && o1 != null) {
                data.put(key, o1);
            }
            if (o != null && key.toLowerCase().startsWith(JnpfKeyConsts.CHILD_TABLE_PREFIX) && o instanceof List) {
                List<Map<String, Object>> subList = (List<Map<String, Object>>) o;
                for (Map<String, Object> map : subList) {
                    this.setData(map);
                }
            }


        }

        return data;
    }

    /**
     * 字段设置
     *
     * @param data         数据源
     * @param dataList     目标数据
     * @param transferList 传递规则
     */
    public List<Map<String, Object>> handleData(Map<String, Object> data, List<Map<String, Object>> dataList, List<TemplateJsonModel> transferList) {
        boolean isAdd = CollUtil.isEmpty(dataList);
        List<Map<String, Object>> list = new ArrayList<>(dataList);
        Map<String, Object> oldData = new HashMap<>(data != null ? data : new HashMap<>());

        int max = 1;
        for (TemplateJsonModel TemplateJsonModel : transferList) {
            Integer sourceType = TemplateJsonModel.getSourceType();
            String sourceValue = TemplateJsonModel.getSourceValue();
            String targetField = TemplateJsonModel.getTargetField();
            if (ObjectUtil.equals(sourceType, FieldEnum.FIELD.getCode())) {
                String[] sourceFieldList = sourceValue.split("-");
                String[] targetFieldList = targetField.split("-");
                if (sourceFieldList.length > 1 && targetFieldList.length == 1 && oldData.get(sourceFieldList[0]) instanceof List) {
                    List<Map<String, Object>> parentList = (List<Map<String, Object>>) oldData.get(sourceFieldList[0]);
                    if (parentList.size() > max) {
                        max = parentList.size();
                    }
                }

            }
        }

        // 更新或新增的数据条数 小于 结果的最大数
        int size = dataList.size();
        if (size < max) {
            for (int i = 0; i < max - size; i++) {
                list.add(new HashMap<>());
            }
        }

        if (isAdd) {
            for (TemplateJsonModel TemplateJsonModel : transferList) {
                Integer sourceType = TemplateJsonModel.getSourceType();
                String sourceValue = TemplateJsonModel.getSourceValue();
                String targetField = TemplateJsonModel.getTargetField();
                if (ObjectUtil.equals(sourceType, FieldEnum.FIELD.getCode())) {
                    String[] sourceFieldList = sourceValue.split("-");
                    String[] targetFieldList = targetField.split("-");
                    if (sourceFieldList.length > 1 && targetFieldList.length > 1) {
                        int diff = 0;
                        // 源数据
                        if (oldData.get(sourceFieldList[0]) instanceof List) {
                            List<Map<String, Object>> sourceList = (List<Map<String, Object>>) oldData.get(sourceFieldList[0]);
                            diff = sourceList.size();
                        }
                        for (Map<String, Object> map : list) {
                            if (map.get(targetFieldList[0]) instanceof List) {
                                List<Map<String, Object>> targetList = (List<Map<String, Object>>) map.get(targetFieldList[0]);
                                int max1 = targetList.size() - diff;
                                for (int i = 0; i < max1; i++) {
                                    targetList.add(new HashMap<>());
                                }
                            } else {
                                List<Map<String, Object>> childList = new ArrayList<>();
                                for (int i = 0; i < diff; i++) {
                                    childList.add(new HashMap<>());
                                }
                                map.put(targetFieldList[0], childList);
                            }
                        }
                    }

                }
            }
        }


        for (TemplateJsonModel TemplateJsonModel : transferList) {
            Integer sourceType = TemplateJsonModel.getSourceType();
            String[] split = TemplateJsonModel.getSourceValue().split("\\|");
            String sourceValue = split.length > 0 ? split[0] : TemplateJsonModel.getSourceValue();

            boolean isData = ObjectUtil.equals(sourceType, FieldEnum.CUSTOM.getCode());
            String[] sourceFieldList = isData ? new String[]{sourceValue} : sourceValue.split("-");

            Object o = flowUtil.getOneData(data, sourceValue);
            String targetField = TemplateJsonModel.getTargetField();
            String[] targetFieldList = targetField.split("-");
            Object object = DataInterfaceVarConst.FORM_ID.equals(sourceValue)
                    ? data.get(FlowFormConstant.ID) : o;
            Object childData = isData ? sourceValue : object;

            // 目标字段为子表
            if (targetFieldList.length > 1) {
                if (sourceFieldList.length > 1) {
                    // 来源是子表、目标是子表
                    if (oldData.get(sourceFieldList[0]) instanceof List) {
                        // 源数据
                        List<Map<String, Object>> parentList = (List<Map<String, Object>>) oldData.get(sourceFieldList[0]);

                        for (int i = 0; i < list.size(); i++) {
                            Map<String, Object> targetMap = list.get(i);
                            List<Map<String, Object>> childList = (List<Map<String, Object>>) targetMap.get(targetFieldList[0]);
                            if (CollUtil.isNotEmpty(childList)) {
                                for (int j = 0; j < parentList.size(); j++) {
                                    Map<String, Object> sourceMap = parentList.get(j);
                                    if (childList.size() > j) {
                                        Map<String, Object> map = childList.get(j);
                                        Object oneData = flowUtil.getOneData(sourceMap, sourceFieldList[1]);
                                        map.put(targetFieldList[1], oneData);
                                    }
                                }
                                targetMap.put(targetFieldList[0], childList);
                            }
                        }
                    } else if (oldData.get(FlowNature.SUB_TABLE) != null) {
                        for (Map<String, Object> map : list) {
                            Object obj = flowUtil.getOneData(oldData, split[0]);
                            if (map.get(targetFieldList[0]) instanceof List) {
                                List<Map<String, Object>> childList = (List<Map<String, Object>>) map.get(targetFieldList[0]);
                                if (CollUtil.isEmpty(childList)) {
                                    childList.add(new HashMap<>());
                                }
                                for (Map<String, Object> objectMap : childList) {
                                    objectMap.put(targetFieldList[1], obj);
                                }
                                map.put(targetFieldList[0], childList);
                            }
                        }
                    }
                } else {
                    // 来源不是子表、目标是子表
                    for (Map<String, Object> map : list) {
                        if (null == map.get(targetFieldList[0])) {
                            List<Map<String, Object>> childList = new ArrayList<>();
                            Map<String, Object> child = new HashMap<>();
                            child.put(targetFieldList[1], childData);
                            childList.add(child);
                            map.put(targetFieldList[0], childList);
                            continue;
                        }
                        if (map.get(targetFieldList[0]) instanceof List) {
                            // 目标子表数据进行赋值
                            List<Map<String, Object>> childList = (List<Map<String, Object>>) map.get(targetFieldList[0]);
                            for (Map<String, Object> childMap : childList) {
                                childMap.put(targetFieldList[1], childData);
                            }
                        }
                    }
                }
            } else {
                if (sourceFieldList.length > 1) {
                    // 来源是子表、目标不是子表
                    if (oldData.get(sourceFieldList[0]) instanceof List) {
                        List<Map<String, Object>> parentList = (List<Map<String, Object>>) oldData.get(sourceFieldList[0]);
                        // 新增时，一一对应；更新时，取子表第一条数据
                        if (isAdd) {
                            for (int i = 0; i < parentList.size(); i++) {
                                if (list.size() > i) {
                                    Map<String, Object> parentMap = parentList.get(i);
                                    Map<String, Object> map = list.get(i);
                                    Object oneData = flowUtil.getOneData(parentMap, sourceFieldList[1]);
                                    map.put(targetField, oneData);
                                }
                            }
                        } else {
                            if (CollUtil.isNotEmpty(parentList)) {
                                Map<String, Object> parentMap = parentList.get(0);
                                for (Map<String, Object> map : list) {
                                    Object oneData = flowUtil.getOneData(parentMap, sourceFieldList[1]);
                                    map.put(targetField, oneData);
                                }
                            }
                        }
                    } else if (oldData.get(FlowNature.SUB_TABLE) != null) {
                        for (Map<String, Object> map : list) {
                            Object obj = flowUtil.getOneData(oldData, split[0]);
                            map.put(targetFieldList[0], obj);
                        }
                    }
                } else {
                    // 来源不是子表、目标不是子表
                    for (Map<String, Object> map : list) {
                        map.put(targetField, childData);
                    }
                }
            }
        }
        return list;
    }

    // 更新数据 -------------------------------
    public void updateData(ExecuteModel model) throws WorkFlowException {
        NodeModel nodeModel = model.getNodeModel();
        String formId = nodeModel.getFormId();
        VisualdevEntity visualdevEntity = serviceUtil.getReleaseInfo(formId);
        String dataSourceForm = nodeModel.getDataSourceForm();
        List<TemplateJsonModel> transferList = nodeModel.getTransferList();

        // 没有可修改的数据时，向对应表单中新增一条数据
        boolean unFoundRule = nodeModel.getUnFoundRule();

        Map<String, List<Map<String, Object>>> allData = TriggerHolder.getData();
        List<Map<String, Object>> maps = allData.get(dataSourceForm);

        // 数据源可以不选，不选则直接更新目标表单的某些数据
        if (StringUtils.isBlank(dataSourceForm)) {
            transferList = transferList.stream().filter(e -> ObjectUtil.equals(e.getSourceType(), 2)).collect(Collectors.toList());
            Map<String, Object> map = new HashMap<>();
            for (TemplateJsonModel TemplateJsonModel : transferList) {
                map.put(TemplateJsonModel.getTargetField(), TemplateJsonModel.getSourceValue());
            }
            maps = new ArrayList<>();
            maps.add(map);
        }

        // 更新数据
        if (CollUtil.isNotEmpty(maps)) {
            for (Map<String, Object> map : maps) {
                List<Map<String, Object>> tempList = this.getDataList(nodeModel, map, new ArrayList<>());
                for (Map<String, Object> tempMap : tempList) {
                    this.setData(tempMap);
                }
                List<Map<String, Object>> list = this.handleData(map, tempList, transferList);
                if (CollUtil.isNotEmpty(list)) {
                    for (Map<String, Object> data : list) {
                        if (null != data.get(FlowFormConstant.ID)) {
                            String id = data.get(FlowFormConstant.ID).toString();
                            serviceUtil.visualUpdate(visualdevEntity, data, id);
                        } else {
                            if (unFoundRule) {
                                serviceUtil.visualCreate(visualdevEntity, data);
                            }
                        }
                    }
                }
            }
        }
    }

    // 删除数据 -------------------------------
    public void deleteData(ExecuteModel model) throws WorkFlowException, SQLException {
        NodeModel nodeModel = model.getNodeModel();
        Integer deleteType = nodeModel.getDeleteType();

        String formId = nodeModel.getFormId();
        VisualdevEntity visualdevEntity = serviceUtil.getReleaseInfo(formId);

        // 直接删除表
        if (ObjectUtil.equals(deleteType, 0)) {
            Integer tableType = nodeModel.getTableType();
            if (ObjectUtil.equals(tableType, 0)) {
                // 删除主表
                List<Map<String, Object>> dataList = this.getDataList(nodeModel, new HashMap<>(), new ArrayList<>());
                if (CollUtil.isNotEmpty(dataList)) {
                    serviceUtil.visualDelete(visualdevEntity, dataList);
                }
            } else {
                // 删除子表
                FlowFormDataModel formDataModel = new FlowFormDataModel();
                formDataModel.setTableName(nodeModel.getSubTable());
                formDataModel.setFormId(nodeModel.getFormId());
                formDataModel.setRuleMatchLogic(nodeModel.getRuleMatchLogic());
                List<SuperQueryJsonModel> ruleList = nodeModel.getRuleList();
                List<Map<String, Object>> list = JsonUtil.getJsonToListMap(JsonUtil.getObjectToString(ruleList));
                formDataModel.setRuleList(list);
                serviceUtil.deleteSubTable(formDataModel);
            }
        } else {
            // 按节点删除
            Integer condition = nodeModel.getDeleteCondition();
            String dataSourceForm = nodeModel.getDataSourceForm();

            Map<String, List<Map<String, Object>>> allData = TriggerHolder.getData();
            List<Map<String, Object>> maps = allData.get(dataSourceForm) != null ? allData.get(dataSourceForm) : new ArrayList<>();

            // 获取条件过滤的目标数据
            List<Map<String, Object>> dataList = new ArrayList<>();
            for (Map<String, Object> map : maps) {
                List<Map<String, Object>> data = this.getDataList(nodeModel, map, new ArrayList<>());
                dataList.addAll(data);
            }

            // 全部目标数据
            nodeModel.setRuleList(new ArrayList<>());
            List<Map<String, Object>> targetData = this.getDataList(nodeModel, new HashMap<>(), new ArrayList<>());

            List<Map<String, Object>> intersection;
            List<Map<String, Object>> differentSet = new ArrayList<>();

            if (ObjectUtil.equals(condition, 1)) {
                // 存在，删除交集
                intersection = dataList;
                if (CollUtil.isNotEmpty(intersection)) {
                    serviceUtil.visualDelete(visualdevEntity, intersection);
                }
            } else {
                // 不存在，删除差集
                for (Map<String, Object> map : targetData) {
                    String sourceId = map.get(FlowFormConstant.ID).toString();
                    for (Map<String, Object> data : dataList) {
                        String targetId = data.get(FlowFormConstant.ID).toString();
                        if (!ObjectUtil.equals(sourceId, targetId)) {
                            differentSet.add(map);
                        }
                    }
                }
                if (CollUtil.isNotEmpty(differentSet)) {
                    serviceUtil.visualDelete(visualdevEntity, differentSet);
                }
            }
        }
    }

    // 发起审批 -------------------------------
    public void launchFlow(ExecuteModel model) throws WorkFlowException {
        NodeModel nodeModel = model.getNodeModel();
        TriggerTaskEntity triggerTask = model.getTriggerTask();
        String flowId = nodeModel.getFlowId();
        List<TemplateJsonModel> transferList = nodeModel.getTransferList();

        TemplateEntity template = templateMapper.selectById(flowId);
        if (null == template) {
            return;
        }
        if (!ObjectUtil.equals(template.getStatus(), TemplateStatueEnum.UP.getCode())) {
            throw new WorkFlowException(MsgCode.WF140.get());
        }
        TemplateJsonEntity jsonEntity = templateJsonMapper.selectById(template.getFlowId());
        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(jsonEntity.getId());
        Map<String, NodeModel> nodes = new HashMap<>();
        for (TemplateNodeEntity node : nodeEntityList) {
            nodes.put(node.getNodeCode(), JsonUtil.getJsonToBean(node.getNodeJson(), NodeModel.class));
        }
        FlowMethod method = new FlowMethod();
        method.setDeploymentId(jsonEntity.getFlowableId());
        TemplateNodeEntity startNode = nodeEntityList.stream()
                .filter(e -> StringUtils.equals(NodeEnum.START.getType(), e.getNodeType())).findFirst().orElse(new TemplateNodeEntity());
        if (flowUtil.checkBranch(startNode)) {
            throw new WorkFlowException(MsgCode.WF147.get());
        }
        method.setNodeCode(startNode.getNodeCode());
        method.setNodes(nodes);
        method.setNodeEntityList(nodeEntityList);
        method.setTaskEntity(model.getTaskEntity() != null ? model.getTaskEntity() : new TaskEntity());
        Map<String, Boolean> resMap = conditionUtil.handleCondition(method);
        // 判断条件、候选人
        try {
            conditionUtil.checkCondition(resMap, nodes);
        } catch (WorkFlowException e) {
            throw new WorkFlowException(MsgCode.WF133.get());
        }
        method.setNextSubFlow(true);
        List<NodeModel> nextApprover = flowUtil.getNextApprover(method);
        if (!flowUtil.checkNextCandidates(nextApprover)) {
            throw new WorkFlowException(MsgCode.WF134.get());
        }

        // 数据源数据
        String dataSourceForm = nodeModel.getDataSourceForm();
        Map<String, List<Map<String, Object>>> allData = TriggerHolder.getData();
        List<Map<String, Object>> maps = allData.get(dataSourceForm) != null ? allData.get(dataSourceForm) : new ArrayList<>();

        List<Map<String, Object>> mapList = new ArrayList<>();

        for (Map<String, Object> map : maps) {
            List<Map<String, Object>> list = this.handleAddData(map, transferList);
            mapList.addAll(list);
        }
        // 不选数据源的处理
        if (StringUtils.isEmpty(dataSourceForm)) {
            transferList = transferList.stream().filter(e -> ObjectUtil.equals(e.getSourceType(), FieldEnum.CUSTOM.getCode())).collect(Collectors.toList());
            Map<String, Object> map = new HashMap<>();
            for (TemplateJsonModel TemplateJsonModel : transferList) {
                map.put(TemplateJsonModel.getTargetField(), TemplateJsonModel.getSourceValue());
            }
            mapList = new ArrayList<>();
            mapList.add(map);
        }
        if (CollUtil.isEmpty(mapList)) {
            return;
        }
        Set<String> userListAll = new HashSet<>();
        if (OperatorEnum.NOMINATOR.getCode().equals(nodeModel.getAssigneeType())) {
            userListAll.addAll(serviceUtil.getUserListAll(nodeModel.getInitiator()));
        } else {
            //表单数据用户
            for (Map<String, Object> map : maps) {
                Object data = flowUtil.getOneData(map, nodeModel.getFormField());
                userListAll.addAll(flowUtil.getUserId(data));
            }
        }

        // 判断流程权限
        FlowFormModel formIdAndFlowId = flowUtil.getFormIdAndFlowId(new ArrayList<>(userListAll), flowId);
        List<UserEntity> userList = serviceUtil.getUserName(Boolean.TRUE.equals(nodeModel.getHasPermission()) ? formIdAndFlowId.getUserId() : formIdAndFlowId.getUserIdAll(), true);
        if (CollUtil.isEmpty(userList)) {
            throw new WorkFlowException(MsgCode.WF136.get());
        }
        List<String> taskId = new ArrayList<>();

        for (UserEntity user : userList) {
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(user.getId());
            userInfo.setUserName(user.getRealName());

            FlowModel flowModel = new FlowModel();
            flowModel.setUserInfo(userInfo);
            flowModel.setFlowId(flowId);
            flowModel.setStatus(TaskStatusEnum.RUNNING.getCode());
            flowModel.setDeploymentId(jsonEntity.getFlowableId());
            flowModel.setNodeEntityList(nodeEntityList);
            for (Map<String, Object> map : mapList) {
                flowModel.setFormData(map);
                if (flowUtil.checkNextError(flowModel, nextApprover, false, false) != 0) {
                    throw new WorkFlowException(MsgCode.WF135.get());
                }
                batchSaveOrSubmit(flowModel);
                TaskEntity taskEntity = flowModel.getTaskEntity();
                if (taskEntity.getRejectDataId() == null) {
                    autoAudit(flowModel);
                    handleEvent();
                }
                handleTaskStatus();
                taskId.add(taskEntity.getId());
            }
        }
        Integer isAsync = nodeModel.getIsAsync();
        if (Objects.equals(FlowNature.CHILD_SYNC, isAsync)) {
            List<TaskEntity> taskList = taskMapper.getInfosSubmit(taskId.toArray(new String[0]), TaskEntity::getId, TaskEntity::getEndTime, TaskEntity::getStatus);
            boolean isFlowEnd = taskList.stream().anyMatch(e -> e.getEndTime() == null);
            if (isFlowEnd) {
                TriggerLaunchflowEntity launchflow = new TriggerLaunchflowEntity();
                launchflow.setId(RandomUtil.uuId());
                launchflow.setTriggerId(triggerTask.getId());
                launchflow.setTaskId(triggerTask.getTaskId());
                launchflow.setNodeId(model.getCurrentNodeId());
                launchflow.setRecordId(model.getRecordId());
                launchflow.setNodeCode(nodeModel.getNodeId());
                launchflow.setNodeName(nodeModel.getNodeName());
                Map<String, List<Map<String, Object>>> nodeDataMap = new HashMap<>();
                Map<String, List<Map<String, Object>>> dataMap = TriggerHolder.getData();
                for (Map.Entry<String, List<Map<String, Object>>> stringListEntry : dataMap.entrySet()) {
                    String key = stringListEntry.getKey();
                    List<Map<String, Object>> dataList = dataMap.get(key) != null ? dataMap.get(key) : new ArrayList<>();
                    List<Map<String, Object>> list = new ArrayList<>();
                    for (Map<String, Object> map : dataList) {
                        Map<String, Object> data = new HashMap<>(map);
                        list.add(data);
                    }
                    nodeDataMap.put(key, list);
                }

                model.setNodeDataMap(nodeDataMap);
                model.setId(launchflow.getId());
                ExtraData extraData = getExtraData(model);
                launchflow.setExtraData(JsonUtil.getObjectToString(extraData));
                launchflow.setTaskIds(JsonUtil.getObjectToString(taskId));
                launchflow.setGroupId(nodeModel.getGroupId());
                launchflow.setRatio(nodeModel.getCompleteRatio());

                //存事件和消息
                TriggerEventModel eventModel = new TriggerEventModel();
                launchflow.setEventModel(JsonUtil.getObjectToString(eventModel));
                triggerLaunchflowMapper.insert(launchflow);
            }
        }
    }

    public ExtraData getExtraData(ExecuteModel executeModel) {
        FlowModel flowModel = executeModel.getFlowModel();
        FlowModel model = JsonUtil.getJsonToBean(flowModel, FlowModel.class);
        model.setRecordEntity(null);
        model.setTaskEntity(null);
        model.setOperatorEntity(null);
        model.setNodeEntity(null);
        model.setNodeEntityList(null);
        model.setNodes(null);
        model.setJsonEntity(null);
        model.setTemplateEntity(null);
        model.setFileList(null);
        model.setBranchList(new ArrayList<>());
        model.setCandidateList(new HashMap<>());
        model.setErrorRuleUserList(new HashMap<>());
        model.setFormDataList(new ArrayList<>());
        ExtraData extraData = JsonUtil.getJsonToBean(executeModel, ExtraData.class);
        TriggerTaskEntity triggerTask = executeModel.getTriggerTask();
        extraData.setTriggerId(triggerTask.getId());
        Map<String, FlowModel> systemMap = new HashMap<>();
        for (SystemAuditModel auditModel : executeModel.getSystemList()) {
            OperatorEntity operator = auditModel.getOperator();
            FlowModel systemModel = JsonUtil.getJsonToBean(auditModel.getFlowModel(), FlowModel.class);
            if (operator != null) {
                systemModel.setRecordEntity(null);
                systemModel.setTaskEntity(null);
                systemModel.setOperatorEntity(null);
                systemModel.setNodeEntity(null);
                systemModel.setNodeEntityList(null);
                systemModel.setNodes(null);
                systemModel.setJsonEntity(null);
                systemModel.setTemplateEntity(null);
                systemMap.put(operator.getId(), systemModel);
            }
        }
        extraData.setSystemMap(systemMap);
        List<String> operatorIdList = executeModel.getOperatorList().stream().map(OperatorEntity::getId).collect(Collectors.toList());
        extraData.setOperatorIdList(operatorIdList);
        List<String> subTaskIdList = executeModel.getSubTaskList().stream().map(TaskEntity::getId).collect(Collectors.toList());
        extraData.setSubTaskIdList(subTaskIdList);
        extraData.setFlowModel(model);
        return extraData;
    }

    public ExecuteModel getExecuteModel(ExtraData extraData) throws WorkFlowException {
        String flowId = extraData.getFlowId();
        String triggerId = extraData.getTriggerId();
        TemplateJsonEntity jsonEntity = templateJsonMapper.selectById(flowId);
        if (jsonEntity != null) {
            ExecuteModel executeModel = JsonUtil.getJsonToBean(extraData, ExecuteModel.class);
            FlowModel flowModel = executeModel.getFlowModel();
            String taskId = extraData.getTaskId();
            Map<String, NodeModel> nodes = new HashMap<>();
            if (StringUtils.isNotEmpty(taskId)) {
                flowUtil.setFlowModel(taskId, flowModel);
                nodes = flowModel.getNodes();
            } else {
                List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(flowId);
                for (TemplateNodeEntity nodeEntity : nodeEntityList) {
                    nodes.put(nodeEntity.getNodeCode(), JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class));
                }
            }
            executeModel.setTaskEntity(flowModel.getTaskEntity());
            executeModel.setNodes(nodes);
            TriggerTaskEntity triggerTask = triggerTaskMapper.selectById(triggerId);
            executeModel.setTriggerTask(triggerTask);
            return executeModel;
        }
        return null;
    }


    // 创建日程 --------------------------
    public void createSchedule(ExecuteModel model) throws WorkFlowException {
        ScheduleNewCrForm fo = new ScheduleNewCrForm();
        NodeModel nodeModel = model.getNodeModel();

        Map<String, List<Map<String, Object>>> allData = TriggerHolder.getData();

        List<String> list = ImmutableList.of(nodeModel.getCreatorUserId());
        List<String> creatorUserIds = this.getUserList(list, nodeModel.getCreatorUserIdSourceType(), allData);
        if (CollUtil.isEmpty(creatorUserIds)) {
            throw new WorkFlowException("找不到创建人");
        }

        Integer titleSourceType = nodeModel.getTitleSourceType();
        String title = this.getFieldValue(nodeModel.getTitle(), titleSourceType, allData);
        fo.setTitle(title);

        String contents = this.getFieldValue(nodeModel.getContents(), nodeModel.getContentsSourceType(), allData);
        fo.setContent(contents);

        String startDay = this.getFieldValue(nodeModel.getStartDay(), nodeModel.getStartDaySourceType(), allData);
        if (StringUtils.isBlank(startDay)) {
            throw new WorkFlowException("开始时间不能为空");
        }
        fo.setStartDay(Long.valueOf(StringUtils.isBlank(startDay) ? new Date().getTime() + "" : startDay));
        fo.setStartTime(nodeModel.getStartTime());

        String endDay = this.getFieldValue(nodeModel.getEndDay(), nodeModel.getEndDaySourceType(), allData);

        if ((ObjectUtil.equals(nodeModel.getDuration(), -1) || ObjectUtil.equals(nodeModel.getAllDay(), 1)) && StringUtils.isBlank(endDay)) {
            throw new WorkFlowException("结束时间不能为空");
        }

        fo.setEndDay(Long.valueOf(StringUtils.isBlank(endDay) ? new Date().getTime() + "" : endDay));
        fo.setEndTime(nodeModel.getEndTime());
        String category = nodeModel.getCategory();
        fo.setCategory(StringUtils.isNotEmpty(category) ? category : "391233231405462789");

        fo.setDuration(nodeModel.getDuration());
        fo.setAllDay(nodeModel.getAllDay());
        fo.setFiles(nodeModel.getFiles());

        List<String> value = this.getUserList(nodeModel.getToUserIds(), nodeModel.getToUserIdsSourceType(), allData);
        fo.setToUserIds(value);
        fo.setColor(nodeModel.getColor());
        fo.setReminderType(nodeModel.getReminderType());
        fo.setReminderTime(nodeModel.getReminderTime());
        fo.setSend(nodeModel.getSend());
        fo.setSendName(nodeModel.getSendName());
        fo.setRepetition(nodeModel.getRepetition());
        fo.setRepeatTime(nodeModel.getRepeatTime());

        for (String creatorUserId : creatorUserIds) {
            ScheduleNewCrForm jsonToBean = JsonUtil.getJsonToBean(fo, ScheduleNewCrForm.class);
            jsonToBean.setCreatorUserId(creatorUserId);
            serviceUtil.createSchedule(jsonToBean);
        }
    }

    public String getFieldValue(String value, Integer sourceType, Map<String, List<Map<String, Object>>> allData) {
        if (ObjectUtil.equals(sourceType, FieldEnum.FIELD.getCode())) {
            String[] split = value.split("\\|");
            if (split.length > 1) {
                List<Map<String, Object>> data = allData.get(split[1]);
                if (CollUtil.isNotEmpty(data)) {
                    Map<String, Object> map = data.get(0);
                    if (ObjectUtil.equals(split[0], DataInterfaceVarConst.FORM_ID)) {
                        value = map.get(FlowFormConstant.ID) + "";
                    } else {
                        value = this.getStrData(map, split[0]);
                    }
                    return value;
                }
            }
            return null;
        }
        return value;
    }

    public List<String> getUserList(List<String> value, Integer sourceType, Map<String, List<Map<String, Object>>> allData) {
        if (ObjectUtil.equals(sourceType, FieldEnum.FIELD.getCode()) && CollUtil.isNotEmpty(value)) {
            List<String> userIdList = new ArrayList<>();
            String str = value.get(0);
            String[] split = str.split("\\|");
            if (split.length > 1) {
                List<Map<String, Object>> dataList = allData.get(split[1]);
                if (ObjectUtil.isNotEmpty(dataList)) {
                    Object obj = flowUtil.getOneData(dataList.get(0), split[0]);
                    userIdList.addAll(flowUtil.getUserId(obj));
                }
            }
            return serviceUtil.getUserName(userIdList, true).stream().map(UserEntity::getId).collect(Collectors.toList());
        }
        return value == null ? new ArrayList<>() : serviceUtil.getUserListAll(value);
    }

    // webhook触发--------------------------------------------
//    @DSTransactional
    public void handleWebhookTrigger(String id, String tenantId, Map<String, Object> body) throws WorkFlowException {
        TemplateJsonEntity jsonEntity = templateJsonMapper.selectById(id);
        if (null == jsonEntity) {
            throw new WorkFlowException(MsgCode.VS016.get());
        }
        if (!ObjectUtil.equals(jsonEntity.getState(), TemplateJsonStatueEnum.START.getCode())) {
            throw new WorkFlowException("版本未启用");
        }
        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(jsonEntity.getId());
        TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                .filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.WEBHOOK_TRIGGER.getType())).findFirst().orElse(null);
        if (null != nodeEntity) {
            NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
            List<Map<String, Object>> dataList = new ArrayList<>();
            Map<String, Object> map = new HashMap<>();
            List<FieLdsModel> formFieldList = nodeModel.getFormFieldList();
            for (FieLdsModel fieLdsModel : formFieldList) {
                map.put(fieLdsModel.getId(), body.get(fieLdsModel.getId()));
            }
            dataList.add(map);
            String token = AuthUtil.loginTempUser(jsonEntity.getCreatorUserId(), tenantId);
            UserInfo userInfo = UserProvider.getUser(token);
            UserProvider.setLoginUser(userInfo);
            UserProvider.setLocalLoginUser(userInfo);
            ExecuteModel model = new ExecuteModel();
            model.setFlowId(jsonEntity.getId());
            model.setDataList(dataList);
            FlowModel flowModel = new FlowModel();
            flowModel.setUserInfo(userInfo);
            model.setFlowModel(flowModel);
            this.execute(model);
        }
    }

    // 通知触发---------------------------------------------------------

    public void msgTrigger(TemplateNodeEntity triggerNode, UserInfo userInfo) throws WorkFlowException {
        String flowId = triggerNode.getFlowId();
        ExecuteModel model = new ExecuteModel();
        model.setFlowId(flowId);
        FlowModel flowModel = new FlowModel();
        flowModel.setUserInfo(userInfo);
        model.setFlowModel(flowModel);
        this.execute(model);
    }

    // 全局属性配置的消息通知，mark 1.开始执行 2.执行失败
    public void globalMsg(ExecuteModel model, Integer mark) throws WorkFlowException {
        TriggerTaskEntity triggerTask = model.getTriggerTask();
        FlowModel flowModel = model.getFlowModel();
        UserInfo userInfo = flowModel.getUserInfo();
        Map<String, NodeModel> nodes = model.getNodes();

        NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
        if (null == global) {
            return;
        }

        String code = ObjectUtil.equals(mark, 1) ? "PZXTJC002" : "PZXTJC001";
        MsgConfig msgConfig;
        if (ObjectUtil.equals(mark, 1)) {
            msgConfig = global.getStartMsgConfig();
        } else {
            msgConfig = global.getFailMsgConfig();
        }
        Integer on = msgConfig.getOn();
        boolean acquiesce = ObjectUtil.equals(on, 3);
        String s = acquiesce ? code : msgConfig.getMsgId();
        String msgId = ObjectUtil.equals(on, 0) ? "" : s;

        List<String> msgUserType = global.getMsgUserType();
        List<String> msgUserIds = global.getMsgUserIds();

        Set<String> userIdList = new HashSet<>();
        for (String type : msgUserType) {
            switch (type) {
                case "1":
                    userIdList.add(triggerTask.getCreatorUserId());
                    break;
                case "2":
                    userIdList.add(serviceUtil.getAdmin());
                    break;
                case "3":
                    List<String> userList = serviceUtil.getUserListAll(msgUserIds);
                    userIdList.addAll(userList);
                    break;
                default:
                    break;
            }
        }

        Map<String, Object> dataMap = ImmutableMap.of(FlowConstant.TITLE, triggerTask.getFullName(), FlowConstant.CREATORUSERNAME, "");

        List<SendConfigJson> templateJson = msgConfig.getTemplateJson();
        List<IntegrateTplModel> jsonToList = JsonUtil.getJsonToList(templateJson, IntegrateTplModel.class);

        if (StringUtils.isNotEmpty(msgId) && CollUtil.isNotEmpty(userIdList)) {
            Map<String, Object> map = acquiesce ? dataMap : this.templateJson(jsonToList, new HashMap<>(), userInfo);
            try {
                this.message(msgId, new ArrayList<>(userIdList), map, userInfo);
            } catch (WorkFlowException e) {
                e.printStackTrace();
            }
        }
    }


}

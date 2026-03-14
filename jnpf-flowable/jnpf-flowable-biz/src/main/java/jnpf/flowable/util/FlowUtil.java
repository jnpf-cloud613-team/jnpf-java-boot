package jnpf.flowable.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.ImmutableList;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.base.model.systemconfig.SysConfigModel;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.emnus.TemplateEnum;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.*;
import jnpf.flowable.mapper.*;
import jnpf.flowable.model.flowable.FlowAbleUrl;
import jnpf.flowable.model.flowable.FlowableNodeModel;
import jnpf.flowable.model.flowable.NextOrPrevFo;
import jnpf.flowable.model.flowable.OutgoingFlowsFo;
import jnpf.flowable.model.message.DelegateModel;
import jnpf.flowable.model.message.FlowEventModel;
import jnpf.flowable.model.message.FlowMsgModel;
import jnpf.flowable.model.operator.OperatorVo;
import jnpf.flowable.model.task.*;
import jnpf.flowable.model.templatejson.FlowFormModel;
import jnpf.flowable.model.templatejson.TemplateJsonInfoVO;
import jnpf.flowable.model.templatenode.FlowErrorModel;
import jnpf.flowable.model.templatenode.TemplateNodeUpFrom;
import jnpf.flowable.model.templatenode.nodejson.*;
import jnpf.flowable.model.util.*;
import jnpf.permission.entity.*;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlowUtil {


    private final  ConditionUtil conditionUtil;

    private final  FlowAbleUrl flowAbleUrl;

    private final  ServiceUtil serviceUtil;


    private final  CandidatesMapper candidatesMapper;

    private final  OperatorMapper operatorMapper;

    private final  LaunchUserMapper launchUserMapper;

    private final  TemplateMapper templateMapper;

    private final  RecordMapper recordMapper;

    private final DelegateMapper delegateMapper;

    private final  DelegateInfoMapper delegateInfoMapper;

    private final  CommentMapper commentMapper;

    private final  TaskMapper taskMapper;

    private final  CirculateMapper circulateMapper;

    private final  TemplateJsonMapper templateJsonMapper;

    private final  TemplateNodeMapper templateNodeMapper;

    private final TriggerLaunchflowMapper triggerLaunchflowMapper;



    //-------------------------------taskUtil------------------------------------------------------------

    /**
     * 判断任务是否在挂起状态
     *
     * @param taskEntity 任务
     */
    public void isSuspend(TaskEntity taskEntity) throws WorkFlowException {
        Integer status = taskEntity != null ? taskEntity.getStatus() : null;
        if (TaskStatusEnum.PAUSED.getCode().equals(status)) {
            throw new WorkFlowException(MsgCode.WF114.get());
        }
    }

    /**
     * 判断任务是否终止状态
     *
     * @param taskEntity 任务
     */
    public void isCancel(TaskEntity taskEntity) throws WorkFlowException {
        Integer status = taskEntity != null ? taskEntity.getStatus() : null;
        if (ObjectUtil.equals(TaskStatusEnum.CANCEL.getCode(), status)) {
            throw new WorkFlowException(MsgCode.WF123.get());
        }
    }

    public void isTrigger(TaskEntity taskEntity) throws WorkFlowException {
        isTrigger(taskEntity, null);
    }

    public void isTrigger(TaskEntity taskEntity, List<String> nodeCode) throws WorkFlowException {
        String taskId = taskEntity != null ? taskEntity.getId() : "";
        if (StringUtil.isNotEmpty(taskId)) {
            List<TriggerLaunchflowEntity> taskList = triggerLaunchflowMapper.getTaskList(ImmutableList.of(taskId), nodeCode);
            if (!taskList.isEmpty()) {
                throw new WorkFlowException(MsgCode.WF158.get());
            }
        }
    }

    // true为选择分支
    public boolean checkBranch(TemplateNodeEntity nodeEntity) {
        NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
        return this.checkBranch(nodeModel);
    }

    public boolean checkBranch(NodeModel nodeModel) {
        String divideRule = nodeModel.getDivideRule();
        return ObjectUtil.equals(DivideRuleEnum.CHOOSE.getType(), divideRule);
    }

    /**
     * 获取下一级审批节点
     */
    public List<NodeModel> getNextApprover(FlowMethod flowMethod) throws WorkFlowException {
        String deploymentId = flowMethod.getDeploymentId();
        String nodeCode = flowMethod.getNodeCode();
        Map<String, NodeModel> nodes = flowMethod.getNodes();
        List<NodeModel> nextNode = flowMethod.getNextNode();

        // 获取下一级节点
        NextOrPrevFo fo = new NextOrPrevFo();
        fo.setDeploymentId(deploymentId);
        fo.setTaskKey(nodeCode);
        List<FlowableNodeModel> nextList = flowAbleUrl.getNext(fo);

        if (CollUtil.isNotEmpty(nextList)) {
            OutgoingFlowsFo flowsFo = new OutgoingFlowsFo();
            flowsFo.setDeploymentId(deploymentId);
            flowsFo.setTaskKey(nodeCode);
            List<String> outgoingFlows = flowAbleUrl.getOutgoingFlows(flowsFo);
            Map<String, Boolean> res = new HashMap<>();

            flowMethod.setOutgoingFlows(outgoingFlows);
            flowMethod.setResMap(res);
            conditionUtil.getConditionResult(flowMethod);
            conditionUtil.checkCondition(res, nodes);

            for (FlowableNodeModel next : nextList) {
                // 判断条件，子流程的出线 条件都为false 则跳过
                Map<String, Boolean> resMap = new HashMap<>();
                flowMethod.setOutgoingFlows(next.getIncomingList());
                flowMethod.setResMap(resMap);
                flowMethod.setNodeCode(next.getId());
                conditionUtil.getConditionResult(flowMethod);
                try {
                    conditionUtil.checkCondition(resMap, nodes);
                } catch (WorkFlowException e) {
                    continue;
                }
                NodeModel nodeModel = nodes.get(next.getId());
                if (null != nodeModel) {
                    // 子流程、外部节点往下递归
                    List<String> typeList = ImmutableList.of(NodeEnum.SUB_FLOW.getType(), NodeEnum.OUTSIDE.getType());
                    if (typeList.contains(nodeModel.getType())) {
                        if (Boolean.TRUE.equals(flowMethod.getNextSubFlow())) {
                            nextNode.add(nodeModel);
                        }
                        getNextApprover(flowMethod);
                    } else {
                        nextNode.add(nodeModel);
                    }
                }
            }
        }
        return nextNode;
    }

    public Object getOneData(Map<String, Object> map, String key) {
        Object obj = null;
        if (null != map) {
            obj = map.get(key + FlowNature.FORM_FIELD_SUFFIX);
            if (null == obj) {
                obj = map.get(key);
            }
        }
        return obj;
    }

    //字段的值获取用户
    public List<String> getUserId(Object data) {
        List<String> userIdList = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(data)) {
            List<String> list = new ArrayList<>();
            try {
                list.addAll(JsonUtil.getJsonToList(String.valueOf(data), String.class));
            } catch (Exception e) {
                log.info(e.getMessage());
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
            List<UserRelationEntity> userRelationList = serviceUtil.getListByObjectIdAll(id);
            List<RoleRelationEntity> roleRelationList = serviceUtil.getListByRoleId(id);
            List<String> userList = serviceUtil.getUserListAll(list);
            List<String> userRelation = userRelationList.stream().map(UserRelationEntity::getUserId).collect(Collectors.toList());
            List<String> roleRelation = roleRelationList.stream().map(RoleRelationEntity::getObjectId).collect(Collectors.toList());
            Set<String> handleIdAll = new HashSet<>();
            handleIdAll.addAll(userRelation);
            handleIdAll.addAll(roleRelation);
            handleIdAll.addAll(id);
            handleIdAll.addAll(userList);
            userIdList.addAll(handleIdAll);
        }
        return userIdList;
    }

    // false为候选人
    public boolean checkNextCandidates(List<NodeModel> nodeList) {
        if (CollUtil.isNotEmpty(nodeList)) {
            for (NodeModel node : nodeList) {
                if (Boolean.TRUE.equals(node.getIsCandidates())) {
                    return false;
                }
            }
        }
        return true;
    }


    public int checkNextError(FlowModel flowModel, List<NodeModel> nextApprover, boolean nodeFlag, boolean notSubmitFlag) throws WorkFlowException {
        return this.checkNextError(flowModel, nextApprover, nodeFlag, notSubmitFlag, false);
    }

    // 判断异常处理是否是  上一节点审批人指定处理人、无法提交
    public int checkNextError(FlowModel flowModel, List<NodeModel> nextApprover, boolean nodeFlag, boolean notSubmitFlag, boolean errorFlag) throws WorkFlowException {
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        TaskEntity taskEntity = flowModel.getTaskEntity();
        List<FlowErrorModel> errorList = new ArrayList<>();
        for (NodeModel nodeModel : nextApprover) {
            // 候选人节点跳过
            if (Boolean.TRUE.equals(nodeModel.getIsCandidates())) {
                continue;
            }
            TemplateNodeEntity nodeEntity = nodeEntityList.stream().filter(e -> e.getNodeCode().equals(nodeModel.getNodeId())).findFirst().orElse(null);
            // 获取审批人
            FlowMethod flowMethod = new FlowMethod();
            flowMethod.setFlowModel(flowModel);
            flowMethod.setTaskEntity(taskEntity);
            flowMethod.setNodeEntity(nodeEntity);
            flowMethod.setNodeEntityList(nodeEntityList);
            flowMethod.setErrorRule(true);
            flowMethod.setExtraRule(true);

            Integer node = flowMethod.getNode();
            Integer notSubmit = flowMethod.getNotSubmit();

            if (node > 0) {
                if (nodeFlag) {
                    handleErrorRule(nodeModel, errorList);
                } else {
                    return 1;
                }
            }
            if (notSubmit > 0) {
                if (notSubmitFlag) {
                    throw new WorkFlowException(MsgCode.WF061.get());
                }
                return 2;
            }
        }
        if (errorFlag && CollUtil.isNotEmpty(errorList)) {
            flowModel.setErrorList(errorList);
            return 3;
        }
        if (!errorList.isEmpty()) {
            AuditModel model = new AuditModel();
            model.setErrorCodeList(new HashSet<>(errorList));
            throw new WorkFlowException(200, JsonUtil.getObjectToString(model));
        }
        return 0;
    }

    /**
     * 获取审批人
     *
     * @param flowMethod
     * @return
     */
    public List<String> userListAll(FlowMethod flowMethod) throws WorkFlowException {
        List<TemplateNodeEntity> nodeEntityList = flowMethod.getNodeEntityList();
        TemplateNodeEntity nodeEntity = flowMethod.getNodeEntity();
        NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
        Optional<TemplateNodeEntity> first = nodeEntityList.stream().filter(t -> NodeEnum.GLOBAL.getType().equals(t.getNodeType())).findFirst();
        TemplateNodeEntity globalEntity = new TemplateNodeEntity();
        if (first.isPresent()) {
            globalEntity = first.get();
        }

        NodeModel global = JsonUtil.getJsonToBean(globalEntity.getNodeJson(), NodeModel.class);
        Integer errRule = global.getErrorRule();
        List<String> errRuleUser = global.getErrorRuleUser();
        // 子流程去获取指定成员、异常处理
        if (StringUtils.equals(nodeModel.getType(), NodeEnum.SUB_FLOW.getType())) {
            errRule = nodeModel.getErrorRule();
            errRuleUser = nodeModel.getErrorRuleUser();
        }

        Boolean errorRule = flowMethod.getErrorRule();
        int pass = 0;
        int notSubmit = 0;
        int node = 0;
        Boolean extraRule = flowMethod.getExtraRule();
        TaskEntity taskEntity = flowMethod.getTaskEntity();

        List<String> userIdAll = user(flowMethod, nodeModel.getAssigneeType());
         //附加规则
        if (Boolean.TRUE.equals(extraRule) && Boolean.TRUE.equals(!nodeModel.getIsCandidates())) {
            rule(userIdAll, taskEntity.getId(), nodeModel.getExtraRule());
        }
        //获取最新用户
        List<UserEntity> userList = serviceUtil.getUserName(userIdAll, true);

        // 子流程权限
        if (StringUtils.equals(nodeModel.getType(), NodeEnum.SUB_FLOW.getType())) {
            String flowId = nodeModel.getFlowId();
            TemplateEntity template = templateMapper.selectById(flowId);
            if (template != null && template.getVisibleType().equals(FlowNature.AUTHORITY) && ObjectUtil.equals(template.getStatus(), TemplateStatueEnum.UP.getCode())) {
                List<UserEntity> list = new ArrayList<>(userList);
                if (Boolean.TRUE.equals(nodeModel.getHasPermission())) {
                    for (UserEntity user : list) {
                        if (serviceUtil.isCommonUser(user.getId())) {
                            List<String> flowIds = serviceUtil.getPermission(user.getId());
                            // 权限中不存在 该流程版本
                            if (!flowIds.contains(flowId)) {
                                UserEntity userEntity = list.stream().filter(e -> e.getId().equals(user.getId())).findFirst().orElse(null);
                                if (null != userEntity) {
                                    userList.remove(user);
                                }
                            }
                        }
                    }
                }
            }
        }
        userIdAll = userList.stream().map(UserEntity::getId).collect(Collectors.toList());

        //异常规则
        if (Boolean.TRUE.equals(errorRule) && userList.isEmpty()) {
            //异常处理规则
            switch (ErrorRuleEnum.getByCode(errRule)) {
                case ADMINISTRATOR:
                    userIdAll.add(serviceUtil.getAdmin());
                    break;
                case INITIATOR:
                    List<UserEntity> errorRuleUser = serviceUtil.getUserName(errRuleUser, true);
                    if (!errorRuleUser.isEmpty()) {
                        userIdAll.addAll(errorRuleUser.stream().map(UserEntity::getId).collect(Collectors.toList()));
                    } else {
                        userIdAll.add(serviceUtil.getAdmin());
                    }
                    break;
                case NODE:
                    String nodeId = nodeEntity.getNodeCode();
                    List<String> userId = new ArrayList<>();
                    List<CandidatesEntity> list = candidatesMapper.getList(taskEntity.getId(), nodeId);
                    for (CandidatesEntity t : list) {
                        List<String> candidates = StringUtil.isNotEmpty(t.getCandidates()) ? Arrays.stream(t.getCandidates().split(",")).collect(Collectors.toList()) : new ArrayList<>();
                        userId.addAll(candidates);
                    }
                    if (!list.isEmpty()) {
                        List<UserEntity> errorRuleUserList = serviceUtil.getUserName(userId, true);
                        if (!errorRuleUserList.isEmpty()) {
                            userIdAll.addAll(errorRuleUserList.stream().map(UserEntity::getId).collect(Collectors.toList()));
                        } else {
                            userIdAll.add(serviceUtil.getAdmin());
                        }
                    }
                    node++;
                    break;
                case PASS:
                    pass++;
                    break;
                case NOT_SUBMIT:
                    notSubmit++;
                    break;
                case CREATOR_USER_ID:
                    userIdAll.add(taskEntity.getCreatorUserId());
                    break;
                default:
                    break;
            }
        }
        flowMethod.setPass(pass);
        flowMethod.setNotSubmit(notSubmit);
        flowMethod.setNode(node);
        return userIdAll;
    }

    /**
     * 附加条件
     */
    public void rule(List<String> userIdAll, String taskId, int rule) {
        LaunchUserEntity flowUser = launchUserMapper.getInfoByTask(taskId);
        if (flowUser != null) {
            rule(userIdAll, flowUser, rule);
        }
    }

    // 附加条件过滤
    public void rule(List<String> userIdAll, LaunchUserEntity flowUser, int rule) {
        List<Integer> ruleList = ImmutableList.of(ExtraRuleEnum.ORGANIZE.getCode(), ExtraRuleEnum.POSITION.getCode(),
                ExtraRuleEnum.MANAGER.getCode(), ExtraRuleEnum.SUBORDINATE.getCode(), ExtraRuleEnum.DEPARTMENT.getCode());
        if (ruleList.contains(rule) && flowUser != null) {
                List<String> organizeList = flowUser.getOrganizeId() != null ? Arrays.asList(flowUser.getOrganizeId().split(",")) : new ArrayList<>();
                List<String> positionList = flowUser.getPositionId() != null ? Arrays.asList(flowUser.getPositionId().split(",")) : new ArrayList<>();
                List<String> managerList = flowUser.getManagerId() != null ? Arrays.asList(flowUser.getManagerId().split(",")) : new ArrayList<>();
                List<String> subordinateList = flowUser.getSubordinate() != null ? Arrays.asList(flowUser.getSubordinate().split(",")) : new ArrayList<>();
                List<UserEntity> userList = serviceUtil.getUserName(userIdAll, true);
                List<String> userListAll = userList.stream().map(UserEntity::getId).collect(Collectors.toList());

                //同一部门、公司的用户
                Map<String, List<String>> userMap = new HashMap<>();

                Map<String, List<UserRelationEntity>> relationUserList = serviceUtil.getListByUserIdAll(userListAll).stream().filter(t -> StringUtil.isNotEmpty(t.getObjectId())).collect(Collectors.groupingBy(UserRelationEntity::getObjectId));
                //附加条件
                switch (ExtraRuleEnum.getByCode(rule)) {
                    case ORGANIZE:
                        for (String organizeId : organizeList) {
                            OrganizeEntity organizeInfo = serviceUtil.getOrganizeInfo(organizeId);
                            if (null != organizeInfo && Objects.equals(organizeInfo.getCategory(), PermissionConst.DEPARTMENT)) {
                                List<String> userId = userMap.get(organizeId) != null ? userMap.get(organizeId) : new ArrayList<>();
                                if (relationUserList.get(organizeId) != null) {
                                    userId.addAll(relationUserList.get(organizeId).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
                                }
                                userMap.put(organizeId, userId);
                            }
                        }
                        List<String> userIdList = new ArrayList<>();
                        for (Map.Entry<String, List<String>> stringListEntry : userMap.entrySet()) {
                            String key = stringListEntry.getKey();
                            List<String> id = userMap.get(key);
                            if (ObjectUtil.isEmpty(id)) {
                                continue;
                            }
                            userIdList.addAll(id);
                        }
                        userIdAll.retainAll(userIdList);
                        break;
                    case POSITION:
                        List<String> position = new ArrayList<>();
                        for (String positionId : positionList) {
                            if (relationUserList.get(positionId) != null) {
                                position.addAll(relationUserList.get(positionId).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
                            }
                        }
                        userIdAll.retainAll(position);
                        break;
                    case MANAGER:
                        List<String> manager = new ArrayList<>();
                        manager.addAll(managerList);
                        userIdAll.retainAll(manager);
                        break;
                    case SUBORDINATE:
                        List<String> subordinate = new ArrayList<>();
                        for (String subordinateId : subordinateList) {
                            if (StringUtil.isNotEmpty(subordinateId)) {
                                subordinate.addAll(new ArrayList<>(Arrays.asList(subordinateId.split(","))));
                            }
                        }
                        userIdAll.retainAll(subordinate);
                        break;
                    case DEPARTMENT:
                        List<String> categoryList = ImmutableList.of("agency", "office");
                        for (String organizeId : organizeList) {
                            OrganizeEntity organizeInfo = serviceUtil.getOrganizeInfo(organizeId);
                            if (organizeInfo == null || categoryList.contains(organizeInfo.getCategory())) {
                                continue;
                            }
                            List<String> orgList = serviceUtil.getDepartmentAll(organizeId).stream().map(OrganizeEntity::getId).collect(Collectors.toList());
                            List<String> departmentAll = serviceUtil.getListByOrgIds(orgList).stream().map(PositionEntity::getId).collect(Collectors.toList());
                            for (String id : departmentAll) {
                                List<String> userId = userMap.get(id) != null ? userMap.get(id) : new ArrayList<>();
                                if (relationUserList.get(id) != null) {
                                    userId.addAll(relationUserList.get(id).stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
                                }
                                userMap.put(id, userId);
                            }
                        }
                        if (!userMap.isEmpty()) {
                            List<String> userId = new ArrayList<>();
                            for (Map.Entry<String, List<String>> stringListEntry : userMap.entrySet()) {
                                String key = stringListEntry.getKey();
                                List<String> id = userMap.get(key);
                                if (ObjectUtil.isEmpty(id)) {
                                    continue;
                                }
                                userId.addAll(id);
                            }
                            userIdAll.retainAll(userId);
                        }
                        break;
                    default:
                        break;
                }
            }

    }

    /**
     * 获取上级节点，不过滤子流程节点
     *
     * @param deploymentId   部署id
     * @param nodeCode       节点编码
     * @param nodeEntityList 节点集合
     * @param nodeCodeList   上一级节点编码集合
     */
    public void prevNode(String deploymentId, String nodeCode, List<TemplateNodeEntity> nodeEntityList, List<String> nodeCodeList) throws WorkFlowException {
        NextOrPrevFo fo = new NextOrPrevFo();
        fo.setDeploymentId(deploymentId);
        fo.setTaskKey(nodeCode);
        List<String> prevList = flowAbleUrl.getPrev(fo);
        if (CollUtil.isNotEmpty(prevList)) {
            for (String prev : prevList) {
                TemplateNodeEntity nodeEntity = nodeEntityList.stream().filter(e -> e.getNodeCode().equals(prev)).findFirst().orElse(null);
                if (nodeEntity != null) {
                    nodeCodeList.add(nodeEntity.getNodeCode());
                }
            }
        }
    }

    // 根据经办获取上一审批节点的审批人
    public List<String> getHandleIds(String taskId, OperatorEntity operator, boolean autoAudit) {
        List<String> handleIds = new ArrayList<>();
        List<OperatorEntity> operatorList = operatorMapper.getList(taskId);
        if (CollUtil.isNotEmpty(operatorList)) {
            List<OperatorEntity> list = operatorList.stream()
                    .filter(e -> e.getHandleTime() != null && ObjectUtil.equals(e.getNodeId(), operator.getNodeId())).sorted(Comparator.comparing(OperatorEntity::getHandleTime).reversed()).collect(Collectors.toList());
            handleIds = list.stream().map(OperatorEntity::getHandleId).collect(Collectors.toList());
        }
        if (CollUtil.isEmpty(handleIds) && autoAudit) {
            handleIds.add(operator.getHandleId());
        }
        return handleIds;
    }

    public void createLaunchUser(String taskId, String userId) {
        LaunchUserEntity launchUserEntity = launchUserMapper.getInfoByTask(taskId);
        if (null != launchUserEntity) {
            return;
        }
        UserEntity user = serviceUtil.getUserInfo(userId);
        if (null != user) {
            LaunchUserEntity entity = new LaunchUserEntity();
            entity.setId(RandomUtil.uuId());
            entity.setTaskId(taskId);
            entity.setType(FlowNature.TASK_INITIATION);
            launchUser(entity, user);
            // Department用到时，再递归获取 serviceUtil.getDepartmentAll
            launchUserMapper.insert(entity);
        }
    }

    /**
     * 用户的信息
     */
    public void launchUser(LaunchUserEntity flowUser, UserEntity userEntity) {
        if (userEntity != null) {
            //全部岗位
            List<UserRelationEntity> userPositionList = serviceUtil.getListByUserIdAll(ImmutableList.of(userEntity.getId())).stream()
                    .filter(t -> PermissionConst.POSITION.equals(t.getObjectType())).collect(Collectors.toList());
            List<String> pos = new ArrayList<>();
            List<String> org = new ArrayList<>();
            List<String> managerId = new ArrayList<>();
            List<String> subordinateList = new ArrayList<>();
            for (UserRelationEntity relation : userPositionList) {
                PositionEntity positionInfo = serviceUtil.getPositionInfo(relation.getObjectId());
                if (positionInfo != null) {
                    userEntity.setPositionId(positionInfo.getId());
                    pos.add(positionInfo.getId());
                    OrganizeEntity organizeInfo = serviceUtil.getOrganizeInfo(positionInfo.getOrganizeId());
                    if (organizeInfo != null) {
                        org.add(organizeInfo.getId());
                    }
                    String managerByLevel = getManagerByLevel(userEntity, 1);
                    if (StringUtil.isNotEmpty(managerByLevel)) {
                        managerId.add(managerByLevel);
                    }
                    List<PositionEntity> childPosition = serviceUtil.getChildPosition(userEntity.getPositionId());
                    List<String> positionList = childPosition.stream().map(PositionEntity::getId).collect(Collectors.toList());
                    positionList.remove(userEntity.getPositionId());
                    List<UserRelationEntity> userRelationList = serviceUtil.getListByObjectIdAll(positionList);
                    subordinateList.addAll(userRelationList.stream().map(UserRelationEntity::getUserId).collect(Collectors.toList()));
                }
            }
            flowUser.setPositionId(String.join(",", pos));
            flowUser.setOrganizeId(String.join(",", org));
            flowUser.setManagerId(String.join(",", managerId));
            String subordinate = serviceUtil.getUserName(subordinateList, true).stream().map(UserEntity::getId).collect(Collectors.joining(","));
            flowUser.setSubordinate(subordinate);
        }
    }

    /**
     * 递归主管
     */
    public String getManagerByLevel(UserEntity userEntity, int level) {
        return getManagerByLevel(userEntity, level, false);
    }

    public String getManagerByLevel(UserEntity userEntity, int level, boolean isOrganize) {
        String managerUser = "";
        if (userEntity == null) {
            return managerUser;
        }
        String positionId = "";
        if (isOrganize) {
            PositionEntity positionEntity = serviceUtil.getPositionInfo(userEntity.getPositionId());
            if (positionEntity != null) {
                OrganizeEntity organizeInfo = serviceUtil.getOrganizeInfo(positionEntity.getOrganizeId());
                positionId = getManageOrg(organizeInfo, level);
            }
        } else {
            PositionEntity positionEntity = serviceUtil.getPositionInfo(userEntity.getPositionId());
            positionId = getManagerPos(positionEntity, level);
        }
        PositionEntity position = serviceUtil.getPositionInfo(positionId);
        return position != null && StringUtil.isNotEmpty(position.getDutyUser()) ? position.getDutyUser() : "";
    }

    /**
     * 查询岗位
     */
    public String getManagerPos(PositionEntity position, int level) {
        String positionId = "";
        if (position != null) {
            String[] tree = position.getPositionIdTree().split(",");
            positionId = position.getId();
            for (int i = tree.length - 1; i >= 0; i--) {
                String id = tree[i];
                if (Objects.equals(id, position.getId())) {
                    continue;
                }
                --level;
                positionId = id;
                if (level == 0) {
                    break;
                }
            }
            if (level > 0) {
                PositionEntity positionInfo = serviceUtil.getPositionInfo(positionId);
                if (positionInfo != null) {
                    OrganizeEntity organizeInfo = serviceUtil.getOrganizeInfo(positionInfo.getOrganizeId());
                    return getManageOrg(organizeInfo, level);
                }
            }
        }
        return positionId;
    }

    /**
     * 查询组织
     */
    private String getManageOrg(OrganizeEntity organize, long level) {
        List<String> organizeList = new ArrayList<>();
        organizeList.add(organize.getId());
        String[] tree = organize.getOrganizeIdTree().split(",");
        for (int i = tree.length - 1; i >= 0; i--) {
            String id = tree[i];
            if (Objects.equals(id, organize.getId())) {
                continue;
            }
            --level;
            organizeList.add(id);
            if (level == 0) {
                break;
            }
        }
        OrganizeEntity entity = serviceUtil.getOrganizeInfo(organizeList.get(organizeList.size() - 1));
        return level == 0 && entity != null ? entity.getDutyPosition() : "";
    }

    /**
     * 查询表单值
     *
     * @return
     */
    public Map<String, Object> infoData(String formId, String taskId)  {
        Map<String, Map<String, Object>> allData = FlowContextHolder.getAllData();
        return infoData(formId, taskId, allData);
    }

    /**
     * 查询表单值
     *
     * @return
     */
    public Map<String, Object> infoData(String formId, String taskId, Map<String, Map<String, Object>> allData)  {
        return allData.get(taskId + JnpfConst.SIDE_MARK + formId) != null ? allData.get(taskId + JnpfConst.SIDE_MARK + formId) : serviceUtil.infoData(formId, taskId);
    }

    /**
     * 保存、更新数据，仅处理数据
     */
    public Map<String, Object> createOrUpdate(FlowMethod flowMethod) throws WorkFlowException {
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        TemplateNodeEntity nodeEntity = flowMethod.getNodeEntity();
        NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
        FlowModel flowModel = flowMethod.getFlowModel();
        Map<String, Object> formData = flowModel.getFormData();
        Map<String, Object> data = new HashMap<>();
        //保存数据的表单
        String formId = nodeEntity.getFormId();
        // 子流程选择的流程版本id
        String taskId = taskEntity.getId();
        String resultNodeCode = resultNodeCode(flowMethod);
        flowMethod.setResultNodeCode(resultNodeCode);
        List<Assign> assignList = nodeModel.getAssignList().stream().filter(e -> e.getNodeId().equals(resultNodeCode)).collect(Collectors.toList());
        //获取当前表单
        Map<String, Object> thisNodeData = infoData(formId, taskId);
        data.putAll(thisNodeData);
        data.putAll(formData(formData, assignList, taskEntity, flowMethod));
        data.put(TableFeildsEnum.VERSION.getField(), thisNodeData.get(TableFeildsEnum.VERSION.getField()));
        return data;
    }

    /**
     * 获取节点的最后一个审批节点
     */
    public String resultNodeCode(FlowMethod flowMethod) throws WorkFlowException {
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        TemplateNodeEntity nodeEntity = flowMethod.getNodeEntity();

        List<TemplateNodeEntity> nodeEntityList = flowMethod.getNodeEntityList();

        FlowModel flowModel = flowMethod.getFlowModel();
        Map<String, Object> formData = flowModel.getFormData();
        // 子流程选择的流程版本id
        String taskId = taskEntity.getId();

        List<Integer> handleStatus = ImmutableList.of(RecordEnum.AUDIT.getCode(), RecordEnum.SUBMIT.getCode());
        //递归
        List<String> nodeIdList = new ArrayList<>();
        prevNodeList(flowModel.getDeploymentId(), nodeEntity.getNodeCode(), nodeEntityList, nodeIdList);
        List<RecordEntity> list = recordMapper.getRecordList(taskId, handleStatus).stream().filter(t -> nodeIdList.contains(t.getNodeCode())).sorted(Comparator.comparing(RecordEntity::getHandleTime).reversed()).collect(Collectors.toList());
        List<String> recordNodeIdList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                recordNodeIdList.add(list.get(i).getNodeCode());
            }
        }
        List<TemplateNodeEntity> taskNodeEntity = nodeEntityList.stream().filter(t -> nodeIdList.contains(t.getNodeCode())).collect(Collectors.toList());
        if (taskNodeEntity.isEmpty()) {
            taskNodeEntity.addAll(nodeEntityList.stream().filter(t -> recordNodeIdList.contains(t.getNodeCode())).collect(Collectors.toList()));
        }
        String nodeCode = "";
        if (CollUtil.isNotEmpty(list)) {
            nodeCode = list.get(0).getNodeCode();
        } else {
            if (!taskNodeEntity.isEmpty()) {
                nodeCode = taskNodeEntity.get(0).getNodeCode();
            }
        }
        if (!taskNodeEntity.isEmpty()) {
            formData.put(FlowConstant.PREV_NODE_FORM_ID, taskNodeEntity.get(0).getFormId());
        }
        return nodeCode;
    }

    /**
     * 获取上级节点
     *
     * @param deploymentId   部署id
     * @param nodeCode       节点编码
     * @param nodeEntityList 节点集合
     * @param nodeCodeList   上一级节点编码集合
     */
    public void prevNodeList(String deploymentId, String nodeCode, List<TemplateNodeEntity> nodeEntityList, List<String> nodeCodeList) throws WorkFlowException {
        NextOrPrevFo fo = new NextOrPrevFo();
        fo.setDeploymentId(deploymentId);
        fo.setTaskKey(nodeCode);
        List<String> prevList = flowAbleUrl.getPrev(fo);
        if (CollUtil.isNotEmpty(prevList)) {
            for (String prev : prevList) {
                TemplateNodeEntity nodeEntity = nodeEntityList.stream().filter(e -> e.getNodeCode().equals(prev)).findFirst().orElse(null);
                if (nodeEntity != null) {
                    // 获取 不是子流程类型的节点
                    if (nodeEntity.getNodeType().equals(NodeEnum.SUB_FLOW.getType())) {
                        prevNodeList(deploymentId, nodeEntity.getNodeCode(), nodeEntityList, nodeCodeList);
                    } else {
                        nodeCodeList.add(nodeEntity.getNodeCode());
                    }
                }
            }
        }
    }


    // 表单赋值
    public Map<String, Object> formData(Map<String, Object> formData, List<Assign> assignListAll, TaskEntity taskEntity, FlowMethod flowMethod)  {
        String taskId = taskEntity.getId();
        List<TemplateNodeEntity> nodeEntityList = flowMethod.getNodeEntityList();
        TemplateNodeEntity startNode = nodeEntityList.stream().filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.START.getType())).findFirst().orElse(null);
        if (startNode != null) {
            formData.put(FlowConstant.START_NODE_FORM_ID, startNode.getFormId());
        }

        // 全局参数
        String globalParameter = taskEntity.getGlobalParameter();
        Map<String, Object> taskMap = globalParameter != null ? JsonUtil.stringToMap(globalParameter) : new HashMap<>();
        formData.putAll(taskMap);

        Map<String, Object> result = new HashMap<>(formData);
        Map<String, Object> oldData = new HashMap<>(formData);

        Map<String, Map<String, Object>> map = new HashMap<>();

        for (Assign assignModel : assignListAll) {
            List<AssignRule> ruleList = this.handleAssignRule(assignModel.getRuleList());
            for (AssignRule assignMode : ruleList) {
                String parentField = assignMode.getParentField();
                String[] parentFieldList = parentField.split("-");
                String childField = assignMode.getChildField();
                String[] childFieldList = childField.split("-");

                if (childFieldList.length > 1 && parentFieldList.length > 1) {
                    int end = 0;
                    if (oldData.get(parentFieldList[0]) instanceof List) {
                        List<Map<String, Object>> parentList = (List<Map<String, Object>>) oldData.get(parentFieldList[0]);
                        end = parentList.size();
                    }
                    if (result.get(childFieldList[0]) instanceof List) {
                        List<Map<String, Object>> childList = (List<Map<String, Object>>) result.get(childFieldList[0]);
                        for (int i = 0; i < childList.size() - end; i++) {
                            childList.add(new HashMap<>());
                        }
                    } else {
                        List<Map<String, Object>> childList = new ArrayList<>();
                        for (int i = 0; i < end; i++) {
                            childList.add(new HashMap<>());
                        }
                        result.put(childFieldList[0], childList);
                        result.put(childFieldList[0] + FlowNature.FORM_FIELD_SUFFIX, childList);
                    }
                }
            }
        }

        for (Assign assignModel : assignListAll) {
            List<AssignRule> ruleList = this.handleAssignRule(assignModel.getRuleList());
            for (AssignRule assignMode : ruleList) {
                String formId = assignMode.getFormId();
                if (StringUtils.equals(formId, FlowNature.GLOBAL_PARAMETER)) {
                    oldData.putAll(taskMap);
                } else {
                    if (StringUtil.isNotEmpty(formId)) {
                        Map<String, Object> mapData = map.get(formId);
                        if (CollUtil.isEmpty(mapData)) {
                            Map<String, Object> data = serviceUtil.infoData(formId, taskId);
                            map.put(formId, data);
                            oldData.putAll(data);
                        }
                    } else {
                        oldData.putAll(formData);
                    }
                }
                //子表处理规则
                String parentField = assignMode.getParentField();
                String[] parentFieldList = parentField.split("-");
                String childField = assignMode.getChildField();
                String[] childFieldList = childField.split("-");
                Object childData = formData.get(parentField);
                if (childFieldList.length > 1) {
                    List<Map<String, Object>> childMapAll = new ArrayList<>();
                    if (result.get(childFieldList[0]) instanceof List) {
                        List<Map<String, Object>> childList = (List<Map<String, Object>>) result.get(childFieldList[0]);
                        for (Map<String, Object> objectMap : childList) {
                            Map<String, Object> childMap = new HashMap<>(objectMap);
                            childMapAll.add(childMap);
                        }
                    }
                    if (parentFieldList.length > 1) {
                        if (oldData.get(parentFieldList[0]) instanceof List) {
                            List<Map<String, Object>> parentList = (List<Map<String, Object>>) oldData.get(parentFieldList[0]);
                            int num = parentList.size() - childMapAll.size();
                            for (int i = 0; i < num; i++) {
                                childMapAll.add(new HashMap<>());
                            }
                            for (int i = 0; i < parentList.size(); i++) {
                                Map<String, Object> parentMap = parentList.get(i);
                                Map<String, Object> childMap = childMapAll.get(i);
                                childMap.put(childFieldList[1], parentMap.get(parentFieldList[1]));
                            }
                        }
                    } else {
                        // 主传子
                        if (childMapAll.isEmpty()) {
                            childMapAll.add(new HashMap<>());
                        }
                        for (Map<String, Object> childMap : childMapAll) {
                            childMap.put(childFieldList[1], childData);
                        }
                    }
                    result.put(childFieldList[0], childMapAll);
                    result.put(childFieldList[0] + FlowNature.FORM_FIELD_SUFFIX, childMapAll);
                } else {
                    if (parentFieldList.length > 1 && oldData.get(parentFieldList[0]) instanceof List) {
                            List<Map<String, Object>> parentList = (List<Map<String, Object>>) oldData.get(parentFieldList[0]);
                            for (int i = 0; i < parentList.size(); i++) {
                                Map<String, Object> parentMap = parentList.get(i);
                                if (i == 0) {
                                    childData = parentMap.get(parentFieldList[1]);
                                }
                            }
                        }

                    result.put(childField, childData);
                    result.put(childField + FlowNature.FORM_FIELD_SUFFIX, childData);
                }
            }
        }
        return result;
    }


    // 截取数据传递字段，获取表单id
    public List<AssignRule> handleAssignRule(List<AssignRule> ruleList) {
        List<AssignRule> list = new ArrayList<>();
        if (CollUtil.isNotEmpty(ruleList)) {
            for (AssignRule assignRule : ruleList) {
                AssignRule rule = JsonUtil.getJsonToBean(assignRule, AssignRule.class);
                String parentField = assignRule.getParentField();
                int index = parentField.lastIndexOf("|");
                if (index != -1) {
                    String field = parentField.substring(0, index);
                    rule.setParentField(field);
                    String substring = parentField.substring(index + 1);
                    rule.setFormId(substring);
                }
                list.add(rule);
            }
        }
        return list;
    }

    /**
     * 组装接口数据
     *
     * @return
     */
    public Map<String, String> parameterMap(FlowModel flowModel, List<TemplateJsonModel> templateJsonModelList) {
        TaskEntity task = flowModel.getTaskEntity();
        List<String> creator = ImmutableList.of(
                FlowConstant.LAUNCH_USER_NAME,
                FlowConstant.CREATORUSERNAME,
                FlowConstant.MANDATOR
        );
        List<String> mandatary = ImmutableList.of(FlowConstant.MANDATARY);
        List<TemplateJsonModel> templateList = templateJsonModelList.stream().filter(e -> Objects.equals(e.getSourceType(), TemplateEnum.SYSTEM.getCode())).collect(Collectors.toList());
        boolean isCreator = templateList.stream().anyMatch(e -> creator.contains(e.getRelationField()));
        UserEntity createUser = null;
        if (isCreator) {
            createUser = serviceUtil.getUserInfo(task.getCreatorUserId());
        }
        boolean isMandatary = templateList.stream().anyMatch(e -> mandatary.contains(e.getRelationField()));
        UserEntity delegate = null;
        if (isMandatary) {
            delegate = StringUtil.isNotEmpty(task.getDelegateUserId()) ? serviceUtil.getUserInfo(task.getDelegateUserId()) : null;
        }
        return parameterMap(flowModel, templateJsonModelList, createUser, delegate);
    }

    /**
     * 组装接口数据
     *
     * @return
     */
    public static Map<String, String> parameterMap(FlowModel flowModel, List<TemplateJsonModel> templateJsonModelList, UserEntity createUser, UserEntity delegate) {
        Map<String, Object> data = flowModel.getFormData();
        TaskEntity task = flowModel.getTaskEntity();
        RecordEntity recordEntity = flowModel.getRecordEntity();
        Map<String, String> parameterMap = new HashMap<>();
        for (TemplateJsonModel templateJsonModel : templateJsonModelList) {
            String fieldId = templateJsonModel.getField();
            String msgTemplateId = templateJsonModel.getMsgTemplateId();
            String relationField = templateJsonModel.getRelationField();
            Integer sourceType = templateJsonModel.getSourceType();
            Map<String, Object> parameterData = task.getGlobalParameter() != null ? JsonUtil.stringToMap(task.getGlobalParameter()) : new HashMap<>();
            String parameterValue = parameterData.get(relationField) != null ? String.valueOf(parameterData.get(relationField)) : null;
            boolean isList = data.get(relationField) instanceof List;
            String string = isList ? JsonUtil.getObjectToString(data.get(relationField)) : String.valueOf(data.get(relationField));
            String dataValue = data.get(relationField) != null ? string : null;
            String s = Objects.equals(FieldEnum.GLOBAL.getCode(), sourceType) ? parameterValue : relationField;
            String dataJson = Objects.equals(FieldEnum.FIELD.getCode(), sourceType) ? dataValue : s;
            FlowEventModel eventModel = FlowEventModel.builder().data(data).dataJson(dataJson).recordEntity(recordEntity).templateJson(templateJsonModel).taskEntity(task).createUser(createUser).delegate(delegate).build();
            dataJson = data(eventModel);
            parameterMap.put((StringUtil.isNotEmpty(msgTemplateId) ? msgTemplateId : "") + fieldId, dataJson);
        }
        return parameterMap;
    }

    /**
     * @return
     */
    public static String data(FlowEventModel eventModel) {
        RecordEntity recordEntity = eventModel.getRecordEntity();
        TemplateJsonModel templateJson = eventModel.getTemplateJson();
        String relationField = StringUtil.isNotEmpty(templateJson.getRelationField()) ? templateJson.getRelationField() : "";
        List<Integer> typeList = ImmutableList.of(FieldEnum.FIELD.getCode(), FieldEnum.SYSTEM.getCode());
        boolean isType = typeList.contains(templateJson.getSourceType());
        String dataJson = eventModel.getDataJson();
        Map<String, Object> data = eventModel.getData();
        UserInfo userInfo = UserProvider.getUser();
        String userId = userInfo.getUserId();
        String userName = userInfo.getUserName() != null ? userInfo.getUserName() : "";
        String value = dataJson;
        TaskEntity taskEntity = eventModel.getTaskEntity();
        if (isType) {
            switch (relationField) {
                case FlowConstant.FLOW_ID:
                    value = taskEntity.getFlowId();
                    break;
                case FlowConstant.TASK_ID:
                    value = taskEntity.getId();
                    break;
                case FlowConstant.EVENT_ID:
                    //外部节点才有
                    value = recordEntity.getNodeId();
                    break;
                case FlowConstant.TASK_NODE_ID:
                    value = recordEntity.getNodeCode();
                    break;
                case FlowConstant.FLOW_FULL_NAME:
                    value = taskEntity.getFlowName();
                    break;
                case FlowConstant.TASK_FULL_NAME:
                    value = taskEntity.getFullName();
                    break;
                case FlowConstant.FORMDATA:
                    value = new JSONObject(data).toJSONString();
                    break;
                case FlowConstant.LAUNCH_USER_ID:
                    value = taskEntity.getCreatorUserId();
                    break;
                case FlowConstant.LAUNCH_USER_NAME:
                case FlowConstant.CREATORUSERNAME:
                case FlowConstant.MANDATOR:

                    UserEntity createUser = eventModel.getCreateUser();
                    value = createUser != null ? createUser.getRealName() : "";
                    break;
                case FlowConstant.FLOW_OPERATOR_USER_ID:
                    value = userId;
                    break;
                case FlowConstant.FLOW_OPERATOR_USER_NAME:
                    value = userName;
                    break;
                case FlowConstant.SENDTIME:
                    value = DateUtil.getNow();
                    break;
                case FlowConstant.MANDATARY:

                    UserEntity delegate = eventModel.getDelegate();
                    value = delegate != null ? delegate.getRealName() : "";
                    break;
                default:
                    String[] model = StringUtil.isNotEmpty(relationField) ? relationField.split("-") : new String[]{};
                    if (model.length > 1) {
                        Object dataList = data.get(model[0]);
                        if (dataList instanceof List) {
                            List<Map<String, Object>> listAll = (List<Map<String, Object>>) dataList;
                            List<Object> list = new ArrayList<>();
                            for (Map<String, Object> objectMap : listAll) {
                                list.add(objectMap.get(model[1]));
                            }
                            value = String.valueOf(list);
                        }
                    }
                    break;
            }
        }
        return value;
    }

    public List<String> getToUser(String userId, String flowId) {
        Date thisTime = DateUtil.getNowDate();
        QueryWrapper<DelegateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DelegateEntity::getType, 1);
        queryWrapper.lambda().le(DelegateEntity::getStartTime, thisTime).ge(DelegateEntity::getEndTime, thisTime);
        if (StringUtil.isNotEmpty(userId)) {
            queryWrapper.lambda().eq(DelegateEntity::getUserId, userId);
        }
        List<DelegateEntity> list = delegateMapper.selectList(queryWrapper);
        List<DelegateEntity> listRes = new ArrayList<>();
        if (StringUtil.isNotEmpty(flowId)) {
            for (DelegateEntity item : list) {
                if (StringUtil.isNotEmpty(item.getFlowId())) {
                    String[] split = item.getFlowId().split(",");
                    if (Arrays.asList(split).contains(flowId)) {
                        listRes.add(item);
                    }
                } else {//为空是全部流程
                    listRes.add(item);
                }
            }
        } else {
            listRes = list;
        }
        List<String> toUser = new ArrayList<>();
        if (CollUtil.isNotEmpty(listRes)) {
            List<String> ids = listRes.stream().map(DelegateEntity::getId).distinct().collect(Collectors.toList());
            List<DelegateInfoEntity> infoList = delegateInfoMapper.getList(ids);
            toUser = infoList.stream().filter(e -> ObjectUtil.equals(e.getStatus(), 1))
                    .map(DelegateInfoEntity::getCreatorUserId).collect(Collectors.toList());
        }
        return toUser;
    }


    // 事件
    public void event(FlowModel flowModel, Integer status) {
        event(flowModel, status, FlowContextHolder.getAllData());
    }

    // 事件
    public void event(FlowModel flowModel, Integer status, Map<String, Map<String, Object>> allData)  {
        FlowModel model = JsonUtil.getJsonToBean(flowModel, FlowModel.class);
        TemplateNodeEntity nodeEntity = model.getNodeEntity();
        NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
        RecordEntity recordEntity = new RecordEntity();
        recordEntity.setNodeCode(nodeModel.getNodeId());
        // 结束事件
        if (ObjectUtil.equals(status, EventEnum.END.getStatus())) {
            List<TemplateNodeEntity> nodeEntityList = model.getNodeEntityList();
            TemplateNodeEntity start = nodeEntityList.stream()
                    .filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.START.getType())).findFirst().orElse(null);
            if (null != start) {
                nodeModel = JsonUtil.getJsonToBean(start.getNodeJson(), NodeModel.class);
            }
        }
        TaskEntity taskEntity = model.getTaskEntity();
        String formId = nodeEntity.getFormId();
        String taskId = taskEntity.getId();
        Map<String, Object> map = allData.get(taskId + JnpfConst.SIDE_MARK + formId) != null ? allData.get(taskId + JnpfConst.SIDE_MARK + formId) : serviceUtil.infoData(formId, taskId);
        model.setFormData(map);
        event(status, nodeModel, recordEntity, model);
    }

    /**
     * 流程事件
     *
     * @param status    事件状态 1.发起 2.结束 3.发起撤回 4同意 5拒绝 6节点撤回 7 超时 8提醒 9退回
     * @param nodeModel 节点数据
     * @param recordEntity    审批数据
     */
    public void event(Integer status, NodeModel nodeModel, RecordEntity recordEntity, FlowModel flowModel) {
        if (nodeModel != null) {
            FuncConfig config = new FuncConfig();
            EventEnum eventStatus = EventEnum.getEventStatus(status);
            switch (eventStatus) {
                case INIT:
                    config = nodeModel.getInitFuncConfig();
                    break;
                case END:
                    config = nodeModel.getEndFuncConfig();
                    break;
                case FLOW_RECALL:
                    config = nodeModel.getFlowRecallFuncConfig();
                    break;
                case APPROVE:
                    config = nodeModel.getApproveFuncConfig();
                    break;
                case REJECT:
                    config = nodeModel.getRejectFuncConfig();
                    break;
                case RECALL:
                    config = nodeModel.getRecallFuncConfig();
                    break;
                case OVERTIME:
                    config = nodeModel.getOvertimeFuncConfig();
                    break;
                case NOTICE:
                    config = nodeModel.getNoticeFuncConfig();
                    break;
                case BACK:
                    config = nodeModel.getBackFuncConfig();
                    break;
                default:
                    break;
            }
            boolean on = config.getOn();
            String interId = config.getInterfaceId();
            List<TemplateJsonModel> templateJsonModelList = config.getTemplateJson();
            if (on && StringUtil.isNotEmpty(interId)) {
                Map<String, Object> data = flowModel.getFormData();
                TaskEntity taskEntity = flowModel.getTaskEntity();
                FlowModel parameterModel = new FlowModel();
                parameterModel.setFormData(data);
                parameterModel.setRecordEntity(recordEntity);
                parameterModel.setTaskEntity(taskEntity);
                Map<String, String> parameterMap = parameterMap(parameterModel, templateJsonModelList);
                ActionResult<Object> result = serviceUtil.infoToId(interId, parameterMap);
                if (null == result || Objects.equals(400, result.getCode())) {
                    FlowUtil.log.info("接口调用失败: " + result);
                }
            }
        }
    }


    public void create(FlowModel fo, String taskId, List<TemplateNodeEntity> nodeEntityList, OperatorEntity operator) {
        UserInfo userInfo = UserProvider.getUser();
        // 候选人
        Map<String, List<String>> candidateList = fo.getCandidateList();
        for (Map.Entry<String, List<String>> stringListEntry : candidateList.entrySet()) {
            String key = stringListEntry.getKey();
            TemplateNodeEntity nodeEntity = nodeEntityList.stream().filter(e -> e.getNodeCode().equals(key)).findFirst().orElse(null);
            if (null == nodeEntity) {
                continue;
            }
            List<String> list = candidateList.get(key);
            this.create(taskId, nodeEntity, operator, userInfo, list, FlowNature.CANDIDATES);
        }
        // 异常人
        Map<String, List<String>> errorRuleUserList = fo.getErrorRuleUserList();
        for (Map.Entry<String, List<String>> stringListEntry : errorRuleUserList.entrySet()) {
            String key = stringListEntry.getKey();
            TemplateNodeEntity nodeEntity = nodeEntityList.stream().filter(e -> e.getNodeCode().equals(key)).findFirst().orElse(null);
            if (null == nodeEntity) {
                continue;
            }
            List<String> list = errorRuleUserList.get(key);
            this.create(taskId, nodeEntity, operator, userInfo, list, FlowNature.CANDIDATES_ERROR);
        }
    }

    public void create(String taskId, TemplateNodeEntity nodeEntity, OperatorEntity operator, UserInfo userInfo, List<String> list, Integer type) {
        CandidatesEntity entity = new CandidatesEntity();
        entity.setId(RandomUtil.uuId());
        entity.setTaskId(taskId);
        entity.setNodeCode(nodeEntity.getNodeCode());

        String nodeId = "";
        if (operator != null) {
            entity.setOperatorId(operator.getId());
            nodeId = operator.getNodeId();
        } else {
            entity.setOperatorId(FlowNature.PARENT_ID);
        }
        List<CandidatesEntity> entityList = candidatesMapper.getListByCode(taskId, nodeEntity.getNodeCode());
        if (CollUtil.isNotEmpty(entityList)) {
            QueryWrapper<OperatorEntity> operatorWrapper = new QueryWrapper<>();
            operatorWrapper.lambda().eq(OperatorEntity::getTaskId, taskId).eq(OperatorEntity::getNodeId, nodeId);
            List<OperatorEntity> opList = operatorMapper.selectList(operatorWrapper);
            if (CollUtil.isNotEmpty(opList)) {
                List<String> opIds = opList.stream().map(OperatorEntity::getId).collect(Collectors.toList());
                List<CandidatesEntity> deleteList = entityList.stream()
                        .filter(e -> !opIds.contains(e.getOperatorId())).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(deleteList)) {
                    candidatesMapper.deleteByIds(deleteList);
                }
            }
        }

        entity.setAccount(userInfo.getUserAccount());
        entity.setHandleId(userInfo.getUserId());
        entity.setType(type);

        entity.setCandidates(String.join(",", list));
        candidatesMapper.insert(entity);
    }

    public FlowMsgModel sendMsg(CommentEntity entity) throws WorkFlowException {
        FlowMsgModel flowMsgModel = new FlowMsgModel();
        flowMsgModel.setWait(false);
        FlowModel flowModel = new FlowModel();
        setFlowModel(entity.getTaskId(), flowModel);
        TaskEntity taskEntity = flowModel.getTaskEntity();
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        TemplateNodeEntity start = nodeEntityList.stream()
                .filter(e -> e.getNodeType().equals(NodeEnum.START.getType())).findFirst().orElse(null);
        if (null == start) {
            return flowMsgModel;
        }
        // 开始节点的评论配置
        NodeModel startNode = JsonUtil.getJsonToBean(start.getNodeJson(), NodeModel.class);
        MsgConfig commentMsgConfig = startNode.getCommentMsgConfig();
        if (commentMsgConfig.getOn() != 1 && commentMsgConfig.getOn() != 3) {
            return flowMsgModel;
        }

        String text = entity.getText();

        // 不包含@{ 且 回复ID为空（不发消息）
        if (StringUtils.isBlank(text) || !text.contains("@{") && StringUtils.isBlank(entity.getReplyId())) {
            return flowMsgModel;
        }

        List<String> userIds = new ArrayList<>();

        // 回复的人
        if (StringUtil.isNotEmpty(entity.getReplyId())) {
            CommentEntity reply = commentMapper.getInfo(entity.getReplyId());
            if (reply != null) {
                userIds.add(reply.getCreatorUserId());
            }
        }

        String regex = "@\\{([^}]*)\\}";

        Pattern pattern = Pattern.compile(regex);

        List<String> userNameList = new ArrayList<>();

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String group = matcher.group();
            userNameList.add(group.substring(2, group.length() - 1));
        }

        // 获取@的人的account
        List<String> accountList = new ArrayList<>();
        if (CollUtil.isNotEmpty(userNameList)) {
            for (String userName : userNameList) {
                String[] split = userName.split("/");
                if (split.length >= 2) {
                    String account = split[split.length - 1];
                    accountList.add(account);
                }
            }
        }

        List<UserEntity> userList = serviceUtil.getUserByAccount(accountList.stream().distinct().collect(Collectors.toList()));

        List<String> ids = userList.stream().map(UserEntity::getId).collect(Collectors.toList());
        userIds.addAll(ids);

        // 过滤自己
        String userId = UserProvider.getLoginUserId();
        userIds = userIds.stream().filter(e -> !e.equals(userId)).distinct().collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return flowMsgModel;
        }

        List<OperatorEntity> operatorList = new ArrayList<>();
        List<CirculateEntity> circulateList = new ArrayList<>();

        List<String> userIdList = new ArrayList<>(userIds);

        TaskUserListModel model = getTaskUserList(taskEntity.getId());

        // 经办
        for (OperatorEntity operator : model.getOperatorList()) {
            String handleId = operator.getHandleId();
            if (userIdList.contains(handleId)) {
                operatorList.add(operator);
                userIdList.remove(handleId);
            }
        }

        // 抄送
        if (!userIdList.isEmpty()) {
            for (CirculateEntity circulate : model.getCirculateList()) {
                String circulateUserId = circulate.getUserId();
                if (userIdList.contains(circulateUserId)) {
                    circulateList.add(circulate);
                    userIdList.remove(circulateUserId);
                }
            }
        }

        // 发起人
        boolean startHandleId = false;
        if (!userIdList.isEmpty() && userIdList.contains(taskEntity.getCreatorUserId())) {
                startHandleId = true;
            }


        // 消息
        flowMsgModel.setNodeList(flowModel.getNodeEntityList());
        flowMsgModel.setCirculateList(circulateList);
        flowMsgModel.setUserInfo(flowModel.getUserInfo());
        flowMsgModel.setTaskEntity(taskEntity);
        flowMsgModel.setOperatorList(operatorList);
        flowMsgModel.setStartHandId(startHandleId);
        flowMsgModel.setComment(true);
        return flowMsgModel;
    }


    public List<TemplateEntity> getListByFlowIds(List<String> flowIds) {
        List<TemplateEntity> list = new ArrayList<>();
        List<TemplateJsonEntity> jsonEntityList = templateJsonMapper.selectByIds(flowIds);
        if (CollUtil.isNotEmpty(jsonEntityList)) {
            List<String> templateIds = jsonEntityList.stream().map(TemplateJsonEntity::getTemplateId).distinct().collect(Collectors.toList());
            if (CollUtil.isNotEmpty(templateIds)) {
                QueryWrapper<TemplateEntity> wrapper = new QueryWrapper<>();
                wrapper.lambda().in(TemplateEntity::getId, templateIds).eq(TemplateEntity::getStatus, TemplateStatueEnum.UP.getCode());
                list = templateMapper.selectList(wrapper);
            }
        }
        return list;
    }

    public TaskUserListModel getTaskUserList(String taskId) {
        Set<String> userIdSet = new HashSet<>();

        // 发起人
        TaskEntity taskEntity = taskMapper.selectById(taskId);
        if (null != taskEntity) {
            userIdSet.add(taskEntity.getCreatorUserId());
        }

        // 审批人
        List<OperatorEntity> operatorList = operatorMapper.getList(taskId);
        if (CollUtil.isNotEmpty(operatorList)) {
            userIdSet.addAll(operatorList.stream().map(OperatorEntity::getHandleId).collect(Collectors.toList()));
        }

        // 抄送
        List<CirculateEntity> circulateList = circulateMapper.getList(taskId);
        if (CollUtil.isNotEmpty(circulateList)) {
            userIdSet.addAll(circulateList.stream().map(CirculateEntity::getUserId).collect(Collectors.toList()));
        }

        List<RecordEntity> recordList = recordMapper.getList(taskId);
        if (CollUtil.isNotEmpty(recordList)) {
            userIdSet.addAll(recordList.stream().map(RecordEntity::getHandleId).collect(Collectors.toList()));
        }

        TaskUserListModel model = new TaskUserListModel();
        model.setAllUserIdList(new ArrayList<>(userIdSet));
        model.setFlowTask(taskEntity);
        model.setOperatorList(operatorList);
        model.setCirculateList(circulateList);
        model.setOperatorRecordList(recordList);

        return model;
    }

    public DelegateModel create(List<String> toUserIdList, DelegateEntity delegateEntity) {
        DelegateModel model = new DelegateModel();
        if (CollUtil.isNotEmpty(toUserIdList)) {
            Integer type = delegateEntity.getType();
            // 获取全局配置，是否确认
            SysConfigModel sysConfig = serviceUtil.getSysConfig();
            Integer ack = ObjectUtil.equals(type, 0) ? sysConfig.getDelegateAck() : sysConfig.getProxyAck();
            List<UserEntity> userList = serviceUtil.getUserName(toUserIdList);
            List<DelegateInfoEntity> list = new ArrayList<>();
            for (String toUserId : toUserIdList) {
                DelegateInfoEntity entity = new DelegateInfoEntity();
                entity.setId(RandomUtil.uuId());
                entity.setStatus(0);
                if (ObjectUtil.equals(ack, 0)) {
                    entity.setStatus(1);
                }
                entity.setDelegateId(delegateEntity.getId());
                entity.setToUserId(toUserId);
                UserEntity user = userList.stream().filter(e -> ObjectUtil.equals(e.getId(), toUserId)).findFirst().orElse(null);
                if (null != user) {
                    entity.setToUserName(user.getRealName() + "/" + user.getAccount());
                }
                list.add(entity);
            }
            delegateInfoMapper.insert(list);

            // 委托消息
            UserInfo userInfo = UserProvider.getUser();
            model.setToUserIds(toUserIdList);
            model.setType(type);
            model.setUserInfo(userInfo);
            model.setDelegate(ObjectUtil.equals(type, 0));
        }
        return model;
    }

    public List<String> update(List<String> toUserIdList, DelegateEntity delegateEntity) {
        List<String> createList = new ArrayList<>();
        String delegateId = delegateEntity.getId();
        if (CollUtil.isNotEmpty(toUserIdList)) {
            List<String> deleteList = new ArrayList<>();
            List<DelegateInfoEntity> list = delegateInfoMapper.getList(delegateId);

            for (String userId : toUserIdList) {
                DelegateInfoEntity infoEntity = list.stream()
                        .filter(e -> ObjectUtil.equals(e.getToUserId(), userId)).findFirst().orElse(null);
                if (null == infoEntity) {
                    createList.add(userId);
                }
            }
            for (DelegateInfoEntity entity : list) {
                String userId = toUserIdList.stream()
                        .filter(e -> ObjectUtil.equals(entity.getToUserId(), e)).findFirst().orElse(null);
                if (StringUtil.isEmpty(userId)) {
                    deleteList.add(entity.getToUserId());
                }
            }
            if (CollUtil.isNotEmpty(deleteList)) {
                QueryWrapper<DelegateInfoEntity> wrapper = new QueryWrapper<>();
                wrapper.lambda().eq(DelegateInfoEntity::getDelegateId, delegateId)
                        .in(DelegateInfoEntity::getToUserId, deleteList);
                delegateInfoMapper.delete(wrapper);
            }
        }
        return createList;
    }

    public List<DelegateEntity> getByToUserId(String toUserId) {
        return this.getByToUserId(toUserId, 1);
    }

    public List<DelegateEntity> getByToUserId(String toUserId, Integer type) {
        List<DelegateInfoEntity> infoList = delegateInfoMapper.getByToUserId(toUserId);
        List<String> ids = infoList.stream().filter(e -> ObjectUtil.equals(e.getStatus(), 1))
                .map(DelegateInfoEntity::getDelegateId).distinct().collect(Collectors.toList());
        List<DelegateEntity> list = new ArrayList<>();
        if (CollUtil.isNotEmpty(ids)) {
            Date thisTime = DateUtil.getNowDate();
            QueryWrapper<DelegateEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(DelegateEntity::getId, ids).eq(DelegateEntity::getType, type)
                    .le(DelegateEntity::getStartTime, thisTime).ge(DelegateEntity::getEndTime, thisTime);
            list = delegateMapper.selectList(queryWrapper);
        }
        return list;
    }

    public String getEnCode(TemplateEntity entity) {
        String code = serviceUtil.getCode();
        boolean existByEnCode = templateMapper.isExistByEnCode(code, entity.getId(), null);
        if (existByEnCode) {
            return getEnCode(entity);
        }
        return code;
    }

    public void create(TemplateNodeUpFrom from) {
        Map<String, Map<String, Object>> flowNodes = from.getFlowNodes();
        String flowXml = from.getFlowXml();
        String templateJsonId = from.getFlowId();
        Boolean isAddVersion = from.getIsAddVersion();
        String id = from.getId();
        List<TemplateJsonEntity> jsonList = templateJsonMapper.getList(id).stream().filter(e -> ObjectUtil.isNotEmpty(e.getVersion())).collect(Collectors.toList());
        int version = jsonList.stream().map(TemplateJsonEntity::getVersion).mapToInt(Integer::parseInt).max().orElse(0) + 1;
        TemplateJsonEntity entity = new TemplateJsonEntity();
        entity.setId(StringUtil.isNotEmpty(templateJsonId) ? templateJsonId : RandomUtil.uuId());
        entity.setTemplateId(id);
        entity.setFlowXml(flowXml);
        NodeModel startNode = JsonUtil.getJsonToList(new ArrayList<>(flowNodes.values()), NodeModel.class).stream().filter(t -> NodeEnum.START.getType().equals(t.getType())).findFirst().orElse(null);
        if (Boolean.TRUE.equals(isAddVersion)) {
            entity.setVersion(String.valueOf(version));
        }
        entity.setState(TemplateJsonStatueEnum.DESIGN.getCode());
        entity.setSortCode(0L);
        String formId = null != startNode ? startNode.getFormId() : null;
        for (Map.Entry<String, Map<String, Object>> stringMapEntry : flowNodes.entrySet()) {
            String key = stringMapEntry.getKey();
            NodeModel nodeModel = JsonUtil.getJsonToBean(flowNodes.get(key), NodeModel.class);
            TemplateNodeEntity nodeEntity = new TemplateNodeEntity();
            nodeEntity.setId(RandomUtil.uuId());
            nodeEntity.setFlowId(entity.getId());
            nodeEntity.setNodeCode(key);
            nodeEntity.setNodeJson(JsonUtil.getObjectToString(flowNodes.get(key)));
            nodeEntity.setNodeType(nodeModel.getType());
            nodeEntity.setFormId(StringUtils.isBlank(nodeModel.getFormId()) ? formId : nodeModel.getFormId());
            templateNodeMapper.setIgnoreLogicDelete().deleteById(nodeEntity.getId());
            templateNodeMapper.setIgnoreLogicDelete().insertOrUpdate(nodeEntity);
            templateNodeMapper.clearIgnoreLogicDelete();
        }
        templateJsonMapper.setIgnoreLogicDelete().deleteById(entity.getId());
        templateJsonMapper.setIgnoreLogicDelete().insertOrUpdate(entity);
        templateJsonMapper.clearIgnoreLogicDelete();
    }

    public void craeteNodeModel(Map<String, Map<String, Object>> flowNodes, String flowId) {
        NodeModel startNode = JsonUtil.getJsonToList(new ArrayList<>(flowNodes.values()), NodeModel.class).stream().filter(t -> NodeEnum.START.getType().equals(t.getType())).findFirst().orElse(null);
        String formId = null != startNode ? startNode.getFormId() : null;
        for (Map.Entry<String, Map<String, Object>> stringMapEntry : flowNodes.entrySet()) {
            String key = stringMapEntry.getKey();
            NodeModel nodeModel = JsonUtil.getJsonToBean(flowNodes.get(key), NodeModel.class);
            TemplateNodeEntity nodeEntity = new TemplateNodeEntity();
            nodeEntity.setId(RandomUtil.uuId());
            nodeEntity.setFlowId(flowId);
            nodeEntity.setNodeCode(key);
            nodeEntity.setNodeJson(JsonUtil.getObjectToString(flowNodes.get(key)));
            nodeEntity.setNodeType(nodeModel.getType());
            nodeEntity.setFormId(StringUtils.isBlank(nodeModel.getFormId()) ? formId : nodeModel.getFormId());
            templateNodeMapper.insert(nodeEntity);
        }
    }

    public TemplateJsonInfoVO getInfoVo(String id) throws WorkFlowException {
        TemplateJsonEntity jsonEntity = templateJsonMapper.getInfo(id);
        TemplateEntity entity = templateMapper.getInfo(jsonEntity.getTemplateId());
        TemplateJsonInfoVO vo = JsonUtil.getJsonToBean(entity, TemplateJsonInfoVO.class);
        vo.setFlowXml(jsonEntity.getFlowXml());
        List<TemplateNodeEntity> templateNodeList = templateNodeMapper.getList(jsonEntity.getId());
        Map<String, Map<String, Object>> flowNodes = new HashMap<>();
        for (TemplateNodeEntity nodeEntity : templateNodeList) {
            flowNodes.put(nodeEntity.getNodeCode(), JsonUtil.stringToMap(nodeEntity.getNodeJson()));
        }
        vo.setFlowableId(jsonEntity.getFlowableId());
        vo.setFlowNodes(flowNodes);
        vo.setFlowId(jsonEntity.getId());
        return vo;
    }

    public List<OperatorVo> getOperatorList(TaskPagination pagination) {
        String loginUserId = StringUtil.isNotEmpty(pagination.getUserId()) ? pagination.getUserId() : UserProvider.getLoginUserId();
        // 是否委托
        Boolean delegateType = pagination.getDelegateType();
        List<DelegateEntity> delegateList = Boolean.TRUE.equals(delegateType) ? getByToUserId(loginUserId) : new ArrayList<>();
        return operatorMapper.getList(pagination, delegateList);
    }

    public List<OperatorVo> getRecordList(TaskPagination pagination) {
        String loginUserId = StringUtil.isNotEmpty(pagination.getUserId()) ? pagination.getUserId() : UserProvider.getLoginUserId();
        List<DelegateEntity> delegateList = getByToUserId(loginUserId);
        List<RecordEntity> recordList = recordMapper.getList(pagination, delegateList);
        if (recordList.isEmpty()) {
            return new ArrayList<>();
        }
        return taskMapper.getlist(pagination, recordList);
    }

    /**
     * 设置参数
     */
    public void setFlowModel(String taskId, FlowModel flowModel) throws WorkFlowException {
        if (flowModel.getUserInfo() == null) {
            flowModel.setUserInfo(UserProvider.getUser());
        }
        TaskEntity taskEntity = taskMapper.getInfo(taskId);
        if (null == taskEntity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        TemplateJsonEntity jsonEntity = templateJsonMapper.getInfo(taskEntity.getFlowId());
        flowModel.setJsonEntity(jsonEntity);
        flowModel.setDeploymentId(jsonEntity.getFlowableId());
        flowModel.setTaskEntity(taskEntity);
        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(taskEntity.getFlowId());
        flowModel.setNodeEntityList(nodeEntityList);
        Map<String, NodeModel> nodes = new HashMap<>();
        for (TemplateNodeEntity nodeEntity : nodeEntityList) {
            nodes.put(nodeEntity.getNodeCode(), JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class));
        }
        flowModel.setNodes(nodes);
    }

    public FlowFormModel getFormIdAndFlowId(List<String> userIdAll, String templateId) throws WorkFlowException {
        List<String> userList = new ArrayList<>();
        TemplateEntity template = templateMapper.getInfo(templateId);
        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(template.getFlowId());
        // 判断权限
        for (String userId : userIdAll) {
            userList.add(userId);
            if (ObjectUtil.equals(template.getVisibleType(), FlowNature.AUTHORITY)) {
                boolean commonUser = serviceUtil.isCommonUser(userId);
                if (commonUser) {
                    List<String> flowIds = serviceUtil.getPermission(userId);
                    if (!flowIds.contains(template.getId())) {
                        userList.remove(userId);
                    }
                }
            }
        }
        TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                .filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.START.getType())).findFirst().orElse(null);
        if (null == nodeEntity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        FlowFormModel model = new FlowFormModel();
        model.setFormId(nodeEntity.getFormId());
        model.setFlowId(template.getFlowId());
        model.setUserId(userList);
        model.setUserIdAll(userIdAll);
        return model;
    }

    public TaskEntity createEntity(FlowModel fo, TemplateEntity templateEntity, Map<String, NodeModel> nodes)  {
        UserInfo user = UserProvider.getUser();
        String userName = user.getUserName();
        String userId = user.getUserId();
        UserInfo userInfo = fo.getUserInfo();
        if (null != userInfo) {
            if (StringUtils.isBlank(fo.getUserId())) {
                fo.setUserId(userInfo.getUserId());
            }
            userName = userInfo.getUserName();
            userId = userInfo.getUserId();
        }
        Map<String, Object> data = fo.getFormData() == null ? new HashMap<>() : fo.getFormData();
        data.put(FlowConstant.FLOW_FULL_NAME, templateEntity.getFullName());
        data.put(FlowConstant.FLOW_FULL_CODE, templateEntity.getEnCode());
        data.put(FlowConstant.LAUNCH_USER_NAME, userName);
        data.put(FlowConstant.LAUNCH_TIME, DateUtil.daFormat(new Date()));
        data.put(FlowConstant.USER_NAME, userName);
        NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
        String titleContent = !FlowNature.TITLE_TYPE.equals(global.getTitleType()) ? global.getTitleContent() : global.getDefaultContent();
        if (StringUtil.isNotEmpty(fo.getId())) {
            TaskEntity taskEntity = taskMapper.selectById(fo.getId());
            if (null != taskEntity) {
                data.put(FlowConstant.FLOW_NAME, taskEntity.getFullName());
                if (StringUtils.isNotBlank(fo.getDelegateUser())) {
                    taskEntity.setDelegateUserId(fo.getDelegateUser());
                }
                taskEntity.setCreatorUserId(StringUtils.isNotBlank(fo.getUserId()) ? fo.getUserId() : userId);
                // 流程名称
                if (ObjectUtil.equals(TaskStatusEnum.RUNNING.getCode(), fo.getStatus())
                        && ObjectUtil.equals(taskEntity.getStatus(), TaskStatusEnum.TO_BE_SUBMIT.getCode())) {
                    String fullName = FlowJsonUtil.field(titleContent, data, true);
                    taskEntity.setFullName(fullName);
                    if (Objects.equals(templateEntity.getType(), FlowNature.FREE)) {
                        taskEntity.setFlowId(templateEntity.getFlowId());
                    }
                }
                taskEntity.setUrgent(fo.getFlowUrgent());
                return taskEntity;
            }
        }
        TaskEntity entity = new TaskEntity();

        entity.setType(fo.getIsFlow());
        if (StringUtil.isNotEmpty(fo.getId())) {
            entity.setId(fo.getId());
        } else {
            entity.setId(RandomUtil.uuId());
        }
        entity.setUrgent(fo.getFlowUrgent());
        entity.setSortCode(0L);

        if (StringUtils.isNotBlank(fo.getDelegateUser())) {
            entity.setDelegateUserId(fo.getDelegateUser());
        }

        if (StringUtil.isNotEmpty(fo.getFreeCode())) {
            entity.setFreeCode(fo.getFreeCode());
        }

        // 发起人就是创建人
        entity.setCreatorUserId(StringUtils.isNotBlank(fo.getUserId()) ? fo.getUserId() : userId);
        entity.setCreatorTime(new Date());

        FileConfig fileConfig = global.getFileConfig();
        entity.setIsFile(Boolean.TRUE.equals(fileConfig.getOn()) ? 0 : null);
        // 流程名称
        String fullName = templateEntity.getFullName();
        if (!ObjectUtil.equals(fo.getStatus(), TaskStatusEnum.TO_BE_SUBMIT.getCode())) {
            data.put(FlowConstant.FLOW_NAME, fullName);
            fullName = FlowJsonUtil.field(titleContent, data, true);
        }
        entity.setParentId(fo.getParentId());
        if (!StringUtils.equals(FlowNature.PARENT_ID, fo.getParentId())) {
            fullName += "(子流程)";
            entity.setSubCode(fo.getSubCode());
            entity.setSubParameter(JsonUtil.getObjectToString(fo.getSubParameter()));
            entity.setIsAsync(fo.getIsAsync());
        }
        entity.setFullName(fullName);

        entity.setDelegateUserId(fo.getDelegateUser());
        entity.setFlowCode(templateEntity.getEnCode());
        entity.setFlowName(templateEntity.getFullName());
        entity.setFlowCategory(templateEntity.getCategory());
        TemplateJsonEntity jsonEntity = fo.getJsonEntity();
        if (null != jsonEntity && StringUtils.isNotBlank(jsonEntity.getVersion())) {
            entity.setFlowVersion(jsonEntity.getVersion());
        } else {
            entity.setFlowVersion(templateEntity.getVersion());
        }
        entity.setTemplateId(templateEntity.getId());
        entity.setFlowId(templateEntity.getFlowId());
        // entity.setIsBatch(1);// 0：否，1：是

        if (templateEntity.getType() != null) {
            entity.setFlowType(templateEntity.getType());
        }
        return entity;
    }


    //-------------------------------operatorUtil------------------------------------------------------------
    // 设置异常处理
    public void handleErrorRule(NodeModel nodeModel, List<FlowErrorModel> errorList) {
        if (null == nodeModel) {
            return;
        }
        FlowErrorModel errorModel = new FlowErrorModel();
        errorModel.setNodeCode(nodeModel.getNodeId());
        errorModel.setNodeName(nodeModel.getNodeName());
        errorList.add(errorModel);
    }

    /**
     * 封装查询审批人
     */
    private List<String> user(FlowMethod flowMethod, int type) throws WorkFlowException {
        List<String> userIdAll = new ArrayList<>();
        Boolean auditFlag = flowMethod.getAuditFlag();
        TemplateNodeEntity nodeEntity = flowMethod.getNodeEntity();
        NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        String createUserId = taskEntity.getCreatorUserId();
        FlowModel flowModel = flowMethod.getFlowModel();
        OperatorEntity operator = flowModel.getOperatorEntity();
        String deploymentId = flowModel.getDeploymentId();
        Boolean autoAudit = flowModel.getAutoAudit();
        List<TemplateNodeEntity> nodeEntityList = flowMethod.getNodeEntityList();
        List<String> prevList = new ArrayList<>();
        TemplateNodeEntity startNode = nodeEntityList.stream().filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.START.getType())).findFirst().orElse(null);
        boolean prevIsStart = false;
        if (Boolean.TRUE.equals(nodeModel.getIsCandidates()) && Boolean.FALSE.equals(auditFlag)) {
                String nodeCode = nodeEntity.getNodeCode();
                List<CandidatesEntity> candidatesList = candidatesMapper.getList(taskEntity.getId(), nodeCode);
                if (CollUtil.isNotEmpty(candidatesList)) {
                    List<String> opIds = candidatesList.stream().map(CandidatesEntity::getOperatorId).collect(Collectors.toList());
                    List<OperatorEntity> operators = operatorMapper.selectByIds(opIds);
                    for (CandidatesEntity t : candidatesList) {
                        OperatorEntity op = operators.stream().filter(e -> ObjectUtil.equals(e.getId(), t.getOperatorId())).findFirst().orElse(null);
                        if (null != op && null == op.getHandleStatus() && null != op.getHandleParameter()) {
                            continue;
                        }
                        List<String> candidates = StringUtil.isNotEmpty(t.getCandidates()) ? Arrays.stream(t.getCandidates().split(",")).collect(Collectors.toList()) : new ArrayList<>();
                        userIdAll.addAll(candidates);
                    }
                }
                return userIdAll;
            }
        //发起者【发起者主管】
        if (OperatorEnum.LAUNCH_CHARGE.getCode().equals(type)) {
            this.prevNode(deploymentId, nodeEntity.getNodeCode(), nodeEntityList, prevList);
            if (null != startNode) {
                prevIsStart = prevList.contains(startNode.getNodeCode());
            }
            List<String> userId = new ArrayList<>();
            // 判断 发起人、上一节点
            Integer approverType = nodeModel.getApproverType();
            if (ObjectUtil.equals(approverType, FlowNature.PREVIOUSLY) && !prevIsStart) {
                List<String> handleIds = new ArrayList<>();
                if (Boolean.TRUE.equals(!auditFlag)) {
                    handleIds.addAll(this.getHandleIds(taskEntity.getId(), operator, autoAudit));
                } else {
                    if (prevList.contains(operator.getNodeCode())) {
                        handleIds.add(operator.getHandleId());
                    }
                }
                List<UserEntity> userList = serviceUtil.getUserName(handleIds);
                userId.addAll(userList.stream().map(UserEntity::getId).collect(Collectors.toList()));
            } else {
                userId.add(createUserId);
            }
            for (String id : userId) {
                if (StringUtil.isEmpty(id)) {
                    continue;
                }
                List<UserRelationEntity> userPositionList = serviceUtil.getListByUserIdAll(ImmutableList.of(id)).stream()
                        .filter(t -> PermissionConst.POSITION.equals(t.getObjectType())).collect(Collectors.toList());
                for (UserRelationEntity relation : userPositionList) {
                    UserEntity user = new UserEntity();
                    user.setPositionId(relation.getObjectId());
                    String managerByLevel = getManagerByLevel(user, nodeModel.getManagerLevel());
                    if (StringUtil.isNotEmpty(managerByLevel)) {
                        userIdAll.add(managerByLevel);
                    }
                }
            }
        }
        //【发起本人】
        if (OperatorEnum.INITIATOR_ME.getCode().equals(type)) {
            userIdAll.add(createUserId);
        }
        //【变量】
        if (OperatorEnum.VARIATE.getCode().equals(type)) {
            flowMethod.setIsAssign(false);
            Map<String, Object> dataAll = flowMethod.getSubFormData();
            if (CollUtil.isEmpty(dataAll)) {
                dataAll = Boolean.TRUE.equals(auditFlag) ? flowModel.getFormData() : createOrUpdate(flowMethod);
            }
            Object data = getOneData(dataAll, nodeModel.getFormField());
            List<String> handleIdAll = this.getUserId(data);
            userIdAll.addAll(handleIdAll);
        }
        //【环节】
        if (OperatorEnum.LINK.getCode().equals(type)) {
            List<RecordEntity> list = recordMapper.getList(taskEntity.getId());
            list = list.stream().filter(e -> ObjectUtil.equals(e.getNodeCode(), nodeModel.getApproverNodeId())
                            && (FlowNature.AUDIT_COMPLETION.equals(e.getHandleType()) || FlowNature.REJECT_COMPLETION.equals(e.getHandleType()))
                            && !FlowNature.INVALID.equals(e.getStatus()) && !e.getHandleId().equals(FlowNature.SYSTEM_CODE) && !e.getHandleId().equals(FlowNature.PARENT_ID))
                    .collect(Collectors.toList());
            List<String> handleId = list.stream().map(RecordEntity::getHandleId).collect(Collectors.toList());
            if (CollUtil.isEmpty(handleId) && Boolean.TRUE.equals(autoAudit)) {
                handleId.add(operator.getHandleId());
            }
            userIdAll.addAll(handleId);
        }
        //【服务】
        if (OperatorEnum.SERVE.getCode().equals(type)) {
            flowMethod.setIsAssign(false);
            Map<String, Object> dataAll = Boolean.TRUE.equals(auditFlag) ? flowModel.getFormData() : createOrUpdate(flowMethod);
            InterfaceConfig interfaceConfig = nodeModel.getInterfaceConfig();
            String interfaceId = interfaceConfig.getInterfaceId();
            if (StringUtil.isNotEmpty(interfaceId)) {
                UserInfo userInfo = UserProvider.getUser();
                List<TemplateJsonModel> templateJsonModelList = interfaceConfig.getTemplateJson();
                RecordEntity recordEntity = new RecordEntity();
                recordEntity.setTaskId(taskEntity.getId());
                recordEntity.setNodeCode(nodeEntity.getNodeCode());
                recordEntity.setHandleId(userInfo.getUserId());
                FlowModel parameterModel = new FlowModel();
                parameterModel.setFormData(dataAll);
                parameterModel.setRecordEntity(recordEntity);
                parameterModel.setTaskEntity(taskEntity);
                Map<String, String> parameterMap = parameterMap(parameterModel, templateJsonModelList);
                ActionResult<Object> result = serviceUtil.infoToId(interfaceId, parameterMap);
                if (Objects.equals(200, result.getCode())) {
                    Object data = result.getData();
                    if (data instanceof Map) {
                        JSONObject map = new JSONObject((Map) data);
                        List<String> handleId = StringUtil.isNotEmpty(map.getString("handleId")) ? Arrays.asList(map.getString("handleId").split(",")) : new ArrayList<>();
                        userIdAll.addAll(handleId);
                    }
                }
            }
        }
        //【指定人】
        if (OperatorEnum.NOMINATOR.getCode().equals(type)) {
            List<String> handleIdAll = serviceUtil.getUserListAll(nodeModel.getApprovers());
            userIdAll.addAll(handleIdAll);
        }
        //【逐级】
        if (OperatorEnum.STEP.getCode().equals(type)) {
            LaunchUserEntity launchUser = new LaunchUserEntity();
            launchUser.setId(RandomUtil.uuId());
            launchUser.setNodeCode(nodeModel.getNodeId());
            launchUser.setTaskId(taskEntity.getId());
            launchUser.setType(FlowNature.STEP_INITIATION);
            launchUser.setCreatorTime(new Date());

            this.prevNode(deploymentId, nodeEntity.getNodeCode(), nodeEntityList, prevList);
            if (null != startNode) {
                prevIsStart = prevList.contains(startNode.getNodeCode());
            }
            // 判断 发起人、上一节点
            String userId = createUserId;
            ApproversConfig approversConfig = nodeModel.getApproversConfig();
            Integer start = approversConfig.getStart();
            if (ObjectUtil.equals(start, FlowNature.PREVIOUSLY) && !prevIsStart) {
                List<String> handleIds = this.getHandleIds(taskEntity.getId(), operator, autoAudit);
                userId = !handleIds.isEmpty() ? handleIds.get(0) : "";
            }
            UserEntity info = serviceUtil.getUserInfo(userId);
            String managerByLevel = getManagerByLevel(info, 1);
            //直属主管
            UserEntity managerInfo = serviceUtil.getUserInfo(managerByLevel);
            if (managerInfo != null) {
                userIdAll.add(managerInfo.getId());
                //保存逐级对象
                PositionEntity positionInfo = serviceUtil.getPositionInfo(info.getPositionId());
                if (positionInfo != null) {
                    launchUser.setPositionId(positionInfo.getPositionIdTree());
                    launchUser.setOrganizeId(positionInfo.getOrganizeId());
                    OrganizeEntity organizeInfo = serviceUtil.getOrganizeInfo(positionInfo.getOrganizeId());
                    if (organizeInfo != null) {
                        launchUser.setOrganizeId(organizeInfo.getOrganizeIdTree());
                    }
                    launchUser.setCreatorUserId(userId);
                    flowMethod.setLaunchUser(launchUser);
                }
            }
        }
        return userIdAll;
    }


}

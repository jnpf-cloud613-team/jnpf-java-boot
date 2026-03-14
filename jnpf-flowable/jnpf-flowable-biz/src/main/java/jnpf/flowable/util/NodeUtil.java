package jnpf.flowable.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.ImmutableList;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.*;
import jnpf.flowable.mapper.*;
import jnpf.flowable.model.flowable.FlowAbleUrl;
import jnpf.flowable.model.flowable.FlowableHistoricModel;
import jnpf.flowable.model.task.FlowMethod;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.templatenode.TaskNodeModel;
import jnpf.flowable.model.templatenode.nodejson.AuxiliaryInfo;
import jnpf.flowable.model.templatenode.nodejson.AuxiliaryInfoConfig;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.util.FlowNature;
import jnpf.model.document.FlowFileModel;
import jnpf.permission.entity.UserEntity;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/29 10:29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeUtil {

    private final  FlowAbleUrl flowAbleUrl;

    private final  ServiceUtil serviceUtil;

    private final FlowUtil flowUtil;

    private final  OperatorMapper operatorMapper;

    private final  TaskMapper taskMapper;

    private final  RevokeMapper revokeMapper;

    private final  EventLogMapper eventLogMapper;

    private final TriggerRecordMapper triggerRecordMapper;

    public List<TaskNodeModel> getNodeList(FlowModel flowModel) throws WorkFlowException {
        List<TaskNodeModel> nodeList = new ArrayList<>();
        TaskEntity taskEntity = flowModel.getTaskEntity();
        // 终止后 任务详情的流程图，需全部置灰
        if (ObjectUtil.equals(TaskStatusEnum.CANCEL.getCode(), taskEntity.getStatus())) {
            return nodeList;
        }
        RevokeEntity revokeEntity = revokeMapper.getRevokeTask(taskEntity.getId());
        boolean isRevoke = revokeEntity != null;
        List<TemplateNodeEntity> nodeEntities = flowModel.getNodeEntityList();
        // 当前节点
        String currentNodeCode = StringUtil.isNotEmpty(taskEntity.getCurrentNodeCode()) ? taskEntity.getCurrentNodeCode() : "";
        List<String> currentNodes = new ArrayList<>(Arrays.asList(currentNodeCode.split(",")));
        // 未经过的节点
        List<String> tobePass = flowAbleUrl.getTobePass(taskEntity.getInstanceId());
        if (currentNodeCode.equals(FlowNature.END_CODE)) {
            currentNodes = nodeEntities.stream()
                    .filter(t -> NodeEnum.END.getType().equals(t.getNodeType())).map(TemplateNodeEntity::getNodeCode).collect(Collectors.toList());
        }
        // 经过的节点
        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setTobePass(tobePass);
        flowMethod.setCurrentNodes(currentNodes);
        flowMethod.setTaskId(taskEntity.getId());
        flowMethod.setTaskEntity(taskEntity);
        flowMethod.setNodeEntityList(nodeEntities);
        List<String> passed = this.getPassed(flowMethod);
        TemplateNodeEntity start = nodeEntities.stream().filter(e -> e.getNodeType().equals(NodeEnum.START.getType())).findFirst().orElse(null);
        if (start != null) {
            passed.add(start.getNodeCode());
        }

        List<TriggerRecordEntity> triggerRecordList = triggerRecordMapper.getListByTaskId(taskEntity.getId());

        List<String> typeList = ImmutableList.of(NodeEnum.GLOBAL.getType(), NodeEnum.CONNECT.getType());
        List<String> divideList = Arrays.stream(DivideRuleEnum.values()).map(DivideRuleEnum::getType).collect(Collectors.toList());
        divideList.addAll(typeList);

        flowMethod.setFlowModel(flowModel);
        flowMethod.setErrorRule(false);
        flowMethod.setExtraRule(true);

        // 节点 (-1没有经过,0.经过 1.当前 2.未经过)
        for (TemplateNodeEntity node : nodeEntities) {
            String nodeCode = node.getNodeCode();
            // 跳过全局节点、连接线
            if (divideList.contains(node.getNodeType())) {
                continue;
            }
            TaskNodeModel model = JsonUtil.getJsonToBean(node, TaskNodeModel.class);
            // 节点名称
            NodeModel nodeModel = JsonUtil.getJsonToBean(node.getNodeJson(), NodeModel.class);
            model.setNodeName(nodeModel.getNodeName());
            if (CollUtil.isNotEmpty(currentNodes) && currentNodes.contains(nodeCode)) {
                    model.setType(NodeTypeEnum.CURRENT.getType());
                    if (NodeEnum.END.getType().equals(node.getNodeType())) {
                        model.setType(NodeTypeEnum.PASS.getType());
                    }
                }

            if (CollUtil.isNotEmpty(passed) && passed.contains(nodeCode)) {
                    model.setType(NodeTypeEnum.PASS.getType());
                }

            TriggerRecordEntity triggerRecord = triggerRecordList.stream()
                    .filter(e -> ObjectUtil.equals(e.getNodeCode(), nodeModel.getNodeId())).findFirst().orElse(null);
            if (null != triggerRecord) {
                String type = NodeTypeEnum.CURRENT.getType();
                if (ObjectUtil.equals(triggerRecord.getStatus(), TriggerRecordEnum.PASSED.getCode())) {
                    type = NodeTypeEnum.PASS.getType();
                } else if (ObjectUtil.equals(triggerRecord.getStatus(), TriggerRecordEnum.EXCEPTION.getCode())) {
                    type = NodeTypeEnum.EXCEPTION.getType();
                }
                model.setType(type);
            }

            List<String> userNameList = new ArrayList<>();

            if (StringUtils.equals(node.getNodeType(), NodeEnum.START.getType())) {
                List<String> userIds = new ArrayList<>();
                userIds.add(taskEntity.getCreatorUserId());
                List<UserEntity> users = serviceUtil.getUserName(userIds);
                UserEntity user = users.get(0);
                userNameList.add(user.getRealName() + "/" + user.getAccount());
                if (!TaskStatusEnum.TO_BE_SUBMIT.getCode().equals(taskEntity.getStatus())) {
                    model.setType(NodeTypeEnum.PASS.getType());
                }
            } else if (StringUtils.equals(node.getNodeType(), NodeEnum.SUB_FLOW.getType())) {
                QueryWrapper<TaskEntity> queryWrapper = new QueryWrapper<>();
                queryWrapper.lambda().eq(TaskEntity::getParentId, taskEntity.getId()).eq(TaskEntity::getSubCode, node.getNodeCode());
                List<TaskEntity> list = taskMapper.selectList(queryWrapper);
                List<String> userIds;
                if (CollUtil.isNotEmpty(list)) {
                    userIds = list.stream().map(TaskEntity::getCreatorUserId).collect(Collectors.toList());
                } else {
                    flowMethod.setNodeEntity(node);
                    userIds = flowUtil.userListAll(flowMethod);
                }
                List<UserEntity> users = serviceUtil.getUserName(userIds);
                if (CollUtil.isNotEmpty(users)) {
                    for (UserEntity user : users) {
                        userNameList.add(user.getRealName() + "/" + user.getAccount());
                    }
                }
            } else if (StringUtils.equals(node.getNodeType(), NodeEnum.APPROVER.getType()) || StringUtils.equals(node.getNodeType(), NodeEnum.PROCESSING.getType())) {
                List<OperatorEntity> operatorList = operatorMapper.getByNodeCode(isRevoke ? revokeEntity.getTaskId() : taskEntity.getId(), node.getNodeCode());

                Integer counterSign = nodeModel.getCounterSign();

                // 获取生成经办的人
                List<Integer> statusList = ImmutableList.of(OperatorStateEnum.REVOKE.getCode(), OperatorStateEnum.ADD_SIGN.getCode(), OperatorStateEnum.ASSIST.getCode());
                List<OperatorEntity> list = operatorList.stream().filter(e -> !statusList.contains(e.getStatus()) && ObjectUtil.equals(e.getParentId(), FlowNature.PARENT_ID)).collect(Collectors.toList());
                List<String> userIds = list.stream().map(OperatorEntity::getHandleId).collect(Collectors.toList());
                // 依次审批
                if (counterSign.equals(FlowNature.IMPROPER_APPROVER)) {
                    OperatorEntity last = list.stream().filter(e -> e.getCompletion().equals(FlowNature.NORMAL)).findFirst().orElse(null);
                    if (null != last && StringUtils.isNotBlank(last.getHandleAll())) {
                        String[] split = last.getHandleAll().split(",");
                        userIds = Arrays.stream(split).collect(Collectors.toList());
                    }
                }

                // 经办处理人的id为0，为系统自动通过
                if (userIds.contains(FlowNature.SYSTEM_CODE)) {
                    nodeList.add(model);
                    continue;
                }
                if (CollUtil.isEmpty(userIds)) {
                    flowMethod.setNodeEntity(node);
                    userIds = flowUtil.userListAll(flowMethod);
                }
                if (CollUtil.isEmpty(userIds)) {
                    nodeList.add(model);
                    continue;
                }

                Set<String> userIdList = new LinkedHashSet<>(userIds);
                List<UserEntity> users = serviceUtil.getUserName(userIds);

                if (CollUtil.isNotEmpty(users)) {
                    for (String userId : userIdList) {
                        users.stream().filter(e -> e.getId().equals(userId)).findFirst().ifPresent(user -> userNameList.add(user.getRealName() + "/" + user.getAccount()));
                    }
                }
            }
            model.setUserName(String.join(",", userNameList));
            nodeList.add(model);
        }
        return nodeList;
    }

    /**
     * 经过的节点
     */
    public List<String> getPassed(FlowMethod flowMethod) {
        List<String> tobePass = flowMethod.getTobePass();
        List<String> currentNodes = flowMethod.getCurrentNodes();
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        String taskId = flowMethod.getTaskId();
        List<TemplateNodeEntity> nodeEntityList = flowMethod.getNodeEntityList();
        List<String> resList = new ArrayList<>();

        QueryWrapper<OperatorEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(OperatorEntity::getTaskId, taskId).ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode());
        List<OperatorEntity> operatorList = operatorMapper.selectList(wrapper);
        if (CollUtil.isNotEmpty(operatorList)) {
            List<String> nodeCodes = operatorList.stream().map(OperatorEntity::getNodeCode)
                    .filter(e -> !currentNodes.contains(e)).collect(Collectors.toList());
            resList.addAll(nodeCodes);
        }

        QueryWrapper<EventLogEntity> eventLogWrapper = new QueryWrapper<>();
        eventLogWrapper.lambda().eq(EventLogEntity::getTaskId, taskId);
        eventLogWrapper.lambda().select(EventLogEntity::getId, EventLogEntity::getNodeCode);
        List<EventLogEntity> eventLogList = eventLogMapper.selectList(eventLogWrapper);
        if (CollUtil.isNotEmpty(eventLogList)) {
            List<String> nodeCodes = eventLogList.stream().map(EventLogEntity::getNodeCode)
                    .filter(e -> !currentNodes.contains(e)).collect(Collectors.toList());
            resList.addAll(nodeCodes);
        }


        RevokeEntity revokeEntity = revokeMapper.getRevokeTask(taskEntity.getId());
        if (null != revokeEntity) {
            taskId = revokeEntity.getTaskId();
        }
        if (!ObjectUtil.equals(TaskStatusEnum.BACKED.getCode(), taskEntity.getStatus())) {
            QueryWrapper<TaskEntity> taskWrapper = new QueryWrapper<>();
            taskWrapper.lambda().eq(TaskEntity::getParentId, taskId);
            List<TaskEntity> subFlowList = taskMapper.selectList(taskWrapper);
            List<String> historic = new ArrayList<>();
            try {
                historic = flowAbleUrl.getHistoric(taskEntity.getInstanceId()).stream().map(FlowableHistoricModel::getCode).collect(Collectors.toList());
            } catch (Exception e) {
                log.info(e.getMessage());
            }
            if (CollUtil.isNotEmpty(subFlowList)) {
                List<String> subCodes = subFlowList.stream().map(TaskEntity::getSubCode)
                        .filter(e -> !currentNodes.contains(e) && !tobePass.contains(e))
                        .collect(Collectors.toList());
                if (null != revokeEntity) {
                    subCodes = subCodes.stream().filter(historic::contains).collect(Collectors.toList());
                }
                resList.addAll(subCodes);
            }
            QueryWrapper<EventLogEntity> eventWrapper = new QueryWrapper<>();
            eventWrapper.lambda().eq(EventLogEntity::getTaskId, taskId);
            List<EventLogEntity> eventList = eventLogMapper.selectList(eventWrapper);
            if (CollUtil.isNotEmpty(eventList)) {
                List<String> eventCodes = eventList.stream().map(EventLogEntity::getNodeCode)
                        .filter(e -> !currentNodes.contains(e) && !tobePass.contains(e))
                        .collect(Collectors.toList());
                if (null != revokeEntity) {
                    eventCodes = eventCodes.stream().filter(historic::contains).collect(Collectors.toList());
                }
                resList.addAll(eventCodes);
            }
            // 撤销会跳过办理节点
            if (null != revokeEntity) {
                List<String> finalHistoric = historic;
                List<String> processingList = nodeEntityList.stream()
                        .filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.PROCESSING.getType())
                                && !currentNodes.contains(e.getNodeCode()) && finalHistoric.contains(e.getNodeCode()))
                        .map(TemplateNodeEntity::getNodeCode).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(processingList)) {
                    resList.addAll(processingList);
                }
            }
        }

        return resList;
    }

    /**
     * 辅助信息 添加归档文件信息
     */
    public void setFlowFile(NodeModel currentNode, TaskEntity taskEntity, Map<String, Object> nodeProperties) {
        List<AuxiliaryInfo> auxiliaryList = currentNode.getAuxiliaryInfo();
        if (auxiliaryList.isEmpty()) {
            return;
        }
        long count = auxiliaryList.stream().filter(e -> ObjectUtil.equals(e.getConfig().getOn(), 0)).count();
        if (count == auxiliaryList.size()) {
            nodeProperties.put("auxiliaryInfo", null);
            return;
        }
        AuxiliaryInfo auxiliary = auxiliaryList.stream().filter(e -> ObjectUtil.equals(e.getId(), 3)).findFirst().orElse(null);
        if (null == auxiliary) {
            return;
        }
        AuxiliaryInfoConfig auxiliaryConfig = auxiliary.getConfig();
        if (!ObjectUtil.equals(auxiliaryConfig.getOn(), 1)) {
            return;
        }
        String templateId = taskEntity.getTemplateId();
        FlowFileModel flowFileModel = FlowFileModel.builder().templateId(templateId).userId(UserProvider.getLoginUserId())
                .dataRange(auxiliaryConfig.getDataRange()).build();
        List<Map<String, Object>> flowFile = serviceUtil.getFlowFile(flowFileModel);
        if (CollUtil.isEmpty(flowFile)) {
            return;
        }
        Object auxiliaryInfo = nodeProperties.get("auxiliaryInfo");
        if (auxiliaryInfo instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) auxiliaryInfo;
            for (Map<String, Object> map : list) {
                if (ObjectUtil.equals(map.get("id"), 3)) {
                    Map<String, Object> config = (Map<String, Object>) map.get("config");
                    config.put("fileList", flowFile);
                }
            }
        }
    }
}

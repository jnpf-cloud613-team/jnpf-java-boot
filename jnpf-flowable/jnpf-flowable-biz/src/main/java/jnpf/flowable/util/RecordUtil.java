package jnpf.flowable.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.ImmutableList;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.*;
import jnpf.flowable.mapper.*;
import jnpf.flowable.model.record.ProgressModel;
import jnpf.flowable.model.record.RecordVo;
import jnpf.flowable.model.record.UserItem;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.util.FlowNature;
import jnpf.permission.entity.UserEntity;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UploaderUtil;
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
 * @since 2024/4/26 17:05
 */
@Component
@RequiredArgsConstructor
public class RecordUtil {

    private final ServiceUtil serviceUtil;

    private final RecordMapper recordMapper;

    private final CirculateMapper circulateMapper;

    private final OperatorMapper operatorMapper;

    private final EventLogMapper eventLogMapper;

    private final NodeRecordMapper nodeRecordMapper;

    private final TriggerTaskMapper triggerTaskMapper;

    private final TriggerLaunchflowMapper triggerLaunchflowMapper;


    /**
     * 获取流转记录
     *
     * @param records 流转记录集合
     */
    public List<RecordVo> getRecordList(List<RecordEntity> records) {
        return getRecordList(records, new ArrayList<>());
    }

    /**
     * 获取流转记录
     *
     * @param records 流转记录集合
     */
    public List<RecordVo> getRecordList(List<RecordEntity> records, List<TemplateNodeEntity> nodeEntities) {
        List<RecordVo> vos = new ArrayList<>();
        if (CollUtil.isNotEmpty(records)) {
            List<UserEntity> userList = serviceUtil.getUserName(records.stream().map(RecordEntity::getHandleId).collect(Collectors.toList()));
            for (RecordEntity recordEntity : records) {
                String nodeCode = recordEntity.getNodeCode();
                RecordVo vo = JsonUtil.getJsonToBean(recordEntity, RecordVo.class);
                UserEntity userEntity = userList.stream().filter(t -> t.getId().equals(recordEntity.getHandleId())).findFirst().orElse(null);
                vo.setUserName(userEntity != null ? userEntity.getRealName() + "/" + userEntity.getAccount() : "");
                if (userEntity != null) {
                    vo.setHeadIcon(UploaderUtil.uploaderImg(userEntity.getHeadIcon()));
                }
                // 设置流转操作人
                String handleUserId = recordEntity.getHandleUserId();
                if (StringUtil.isNotEmpty(handleUserId)) {
                    List<String> userIds = Arrays.stream(handleUserId.split(",")).collect(Collectors.toList());
                    List<UserEntity> handleUserList = serviceUtil.getUserName(userIds);
                    List<String> handleUserName = new ArrayList<>();
                    for (String userId : userIds) {
                        UserEntity user = handleUserList.stream().filter(t -> t.getId().equals(userId)).findFirst().orElse(null);
                        if (null != user) {
                            handleUserName.add(user.getRealName() + "/" + user.getAccount());
                        }
                    }
                    if (CollUtil.isNotEmpty(handleUserName)) {
                        vo.setHandleUserName(String.join(",", handleUserName));
                    }
                }
                if (recordEntity.getExpandField() != null) {
                    List<Map<String, Object>> expandField = JsonUtil.getJsonToListMap(recordEntity.getExpandField());
                    vo.setApprovalField(expandField);
                }
                TemplateNodeEntity templateNode = nodeEntities.stream().filter(e -> Objects.equals(nodeCode, e.getNodeCode())).findFirst().orElse(null);
                if (templateNode != null) {
                    vo.setIsOutSideNode(Objects.equals(templateNode.getNodeType(), NodeEnum.OUTSIDE.getType()));
                }
                vos.add(vo);
            }
        }
        return vos;
    }

    /**
     * 进度
     *
     * @param flowModel 参数
     */
    public List<ProgressModel> getProgressList(FlowModel flowModel) {
        List<ProgressModel> progressList = new ArrayList<>();
        String opType = flowModel.getOpType();
        TaskEntity taskEntity = flowModel.getTaskEntity();
        RecordEntity recordEntity = flowModel.getRecordEntity();
        List<TemplateNodeEntity> nodeEntities = flowModel.getNodeEntityList();

        List<NodeRecordEntity> nodeRecordList = nodeRecordMapper.getList(taskEntity.getId());

        List<OperatorEntity> operatorList = operatorMapper.getList(taskEntity.getId());
        List<RecordEntity> recordList = recordMapper.getList(taskEntity.getId());
        List<EventLogEntity> eventLogList = eventLogMapper.getList(taskEntity.getId());
        List<CirculateEntity> circulateList = circulateMapper.getList(taskEntity.getId());

        List<String> userIds = new ArrayList<>();
        List<String> operatorUserIds = operatorList.stream().map(OperatorEntity::getHandleId).collect(Collectors.toList());
        List<String> recordUserIds = recordList.stream().map(RecordEntity::getHandleId).collect(Collectors.toList());
        userIds.addAll(operatorUserIds);
        userIds.addAll(recordUserIds);
        userIds = userIds.stream().distinct().collect(Collectors.toList());

        List<UserEntity> users = serviceUtil.getUserName(userIds, false);

        for (NodeRecordEntity nodeRecord : nodeRecordList) {

            TemplateNodeEntity nodeEntity = nodeEntities.stream().filter(e -> e.getNodeCode().equals(nodeRecord.getNodeCode())).findFirst().orElse(null);

            if (null != nodeEntity) {


                NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
                if (!nodeModel.getType().equals(NodeEnum.SUB_FLOW.getType())) {


                    ProgressModel model = new ProgressModel();
                    model.setId(RandomUtil.uuId());
                    model.setStartTime(nodeRecord.getCreatorTime().getTime());
                    model.setNodeId(nodeRecord.getNodeId());
                    model.setNodeCode(nodeRecord.getNodeCode());
                    model.setNodeName(nodeRecord.getNodeName());
                    model.setNodeType(NodeEnum.APPROVER.getType());
                    model.setNodeStatus(nodeRecord.getNodeStatus());
                    boolean isCirculate = circulateList.stream().anyMatch(e -> e.getNodeId().equals(nodeRecord.getNodeId()));
                    model.setIsCirculate(isCirculate);
                    List<String> typeList = ImmutableList.of(NodeEnum.START.getType(), NodeEnum.OUTSIDE.getType());
                    if (typeList.contains(nodeModel.getType())) {
                        model.setNodeType(nodeModel.getType());
                        progressList.add(model);
                        continue;
                    }

                    model.setCounterSign(nodeModel.getCounterSign());
                    model.setAssigneeType(nodeModel.getAssigneeType());

                    List<RecordEntity> records = recordList.stream()
                            .filter(e -> Objects.equals(e.getNodeCode(), nodeModel.getNodeId()) && Objects.equals(e.getNodeId(), nodeRecord.getNodeId()))
                            .sorted(Comparator.comparing(RecordEntity::getHandleTime).reversed()).collect(Collectors.toList());

                    model.setApproverCount(records.size());

                    List<UserItem> items = new ArrayList<>();
                    for (int i = 0; i < records.size(); i++) {
                        UserItem item = new UserItem();
                        RecordEntity recordEntity1 = records.get(i);
                        UserEntity user = users.stream().filter(e -> ObjectUtil.equals(recordEntity1.getHandleId(), e.getId())).findFirst().orElse(null);
                        if (user != null) {
                            item.setHeadIcon(UploaderUtil.uploaderImg(user.getHeadIcon()));
                            item.setUserId(user.getId());
                            item.setUserName(user.getRealName());
                        } else {
                            item.setHeadIcon(UploaderUtil.uploaderImg(FlowNature.SYSTEM_HEAD_ICON));
                            item.setUserId(FlowNature.SYSTEM_CODE);
                            item.setUserName(FlowNature.SYSTEM_NAME);
                        }
                        item.setHandleType(recordEntity1.getHandleType());
                        items.add(item);
                        if (i == 3) {
                            break;
                        }
                    }
                    model.setApprover(items);
                    // 判断是否存在任务流程
                    model.setShowTaskFlow(triggerTaskMapper.existTriggerTask(taskEntity.getId(), model.getNodeId()));
                    progressList.add(model);
                }
            }
        }

        // 当前节点
        List<Integer> status = ImmutableList.of(TaskStatusEnum.RECALL.getCode(), TaskStatusEnum.BACKED.getCode(), TaskStatusEnum.CANCEL.getCode());
        if (taskEntity.getEndTime() == null && !status.contains(taskEntity.getStatus())) {
            String currentNodeCode = taskEntity.getCurrentNodeCode();
            if (StringUtils.isNotBlank(currentNodeCode)) {
                List<String> currentCodeList = Arrays.stream(currentNodeCode.split(",")).collect(Collectors.toList());

                for (int i = 0; i < currentCodeList.size(); i++) {
                    String nodeCode = currentCodeList.get(i);

                    TemplateNodeEntity nodeEntity = nodeEntities.stream().filter(e -> e.getNodeCode().equals(nodeCode)).findFirst().orElse(null);
                    if (null == nodeEntity) {
                        continue;
                    }
                    NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
                    if (nodeModel.getType().equals(NodeEnum.SUB_FLOW.getType())) {
                        continue;
                    }

                    boolean isProcessing = ObjectUtil.equals(nodeEntity.getNodeType(), NodeEnum.PROCESSING.getType());
                    boolean isOutside = ObjectUtil.equals(nodeEntity.getNodeType(), NodeEnum.OUTSIDE.getType());
                    ProgressModel model = new ProgressModel();
                    model.setId(RandomUtil.uuId());

                    model.setNodeCode(nodeCode);
                    model.setNodeName(nodeModel.getNodeName());
                    String s = isOutside ? NodeEnum.OUTSIDE.getType() : NodeEnum.APPROVER.getType();
                    model.setNodeType(isProcessing ? NodeEnum.PROCESSING.getType() : s);
                    model.setNodeStatus(isProcessing ? NodeStateEnum.TRANSACT.getCode() : NodeStateEnum.APPROVAL.getCode());
                    model.setCounterSign(nodeModel.getCounterSign());
                    model.setAssigneeType(nodeModel.getAssigneeType());

                    List<OperatorEntity> operators = operatorList.stream()
                            .filter(e -> Objects.equals(e.getNodeCode(), nodeModel.getNodeId())
                                    && !(ObjectUtil.equals(e.getCompletion(), FlowNature.ACTION) && null == e.getHandleTime() && !ObjectUtil.equals(e.getParentId(), FlowNature.PARENT_ID))
                                    && !Objects.equals(e.getStatus(), OperatorStateEnum.ASSIST.getCode())
                                    && !Objects.equals(e.getStatus(), OperatorStateEnum.FUTILITY.getCode())
                            ).sorted(Comparator.comparing(OperatorEntity::getCreatorTime, Comparator.reverseOrder())
                                    .thenComparing(OperatorEntity::getCompletion))
                            .collect(Collectors.toList());

                    if (CollUtil.isNotEmpty(operators)) {
                        OperatorEntity operator = operators.get(0);
                        String nodeId = operator.getNodeId();
                        model.setStartTime(operator.getCreatorTime().getTime());
                        operators = operators.stream().filter(e -> ObjectUtil.equals(e.getNodeId(), nodeId)).collect(Collectors.toList());
                        // 触发节点选择同步，变更为等待中状态
                        if (ObjectUtil.equals(operator.getStatus(), OperatorStateEnum.WAITING.getCode())) {
                            model.setNodeStatus(NodeStateEnum.WAIT.getCode());
                        }
                        model.setNodeId(nodeId);
                        boolean isCirculate = circulateList.stream().anyMatch(e -> e.getNodeId().equals(nodeId));
                        model.setIsCirculate(isCirculate);
                    } else {
                        List<EventLogEntity> eventLogs = eventLogList.stream().filter(e -> Objects.equals(e.getNodeCode(), nodeCode)).sorted(Comparator.comparing(EventLogEntity::getCreatorTime, Comparator.reverseOrder())).collect(Collectors.toList());
                        if (CollUtil.isNotEmpty(eventLogs)) {
                            EventLogEntity eventLog = eventLogs.get(0);
                            String nodeId = eventLog.getNodeId();
                            model.setStartTime(eventLog.getCreatorTime().getTime());
                            model.setOutSideStatus(Objects.equals(eventLog.getStatus(), 0));
                            model.setErrorTip(eventLog.getResult());
                            model.setErrorData(eventLog.getData());
                            model.setNodeId(nodeId);
                            if (Objects.equals(OpTypeEnum.LAUNCH_DETAIL.getType(), opType)) {
                                model.setIsRetry(true);
                            }
                            if (Objects.equals(OpTypeEnum.DONE.getType(), opType)
                                    && recordEntity != null
                                    && Objects.equals(recordEntity.getNodeCode(), eventLog.getUpNode())) {
                                model.setIsRetry(true);
                            }

                        }
                    }

                    if (StringUtil.isNotEmpty(model.getNodeId())) {
                        // 判断是否存在任务流程
                        boolean isTrigger = triggerTaskMapper.existTriggerTask(taskEntity.getId(), model.getNodeId());
                        if (isTrigger) {
                            model.setNodeStatus(NodeStateEnum.WAIT.getCode());
                        }
                    }


                    List<RecordEntity> records = recordList.stream()
                            .filter(e -> Objects.equals(e.getNodeCode(), nodeModel.getNodeId()))
                            .sorted(Comparator.comparing(RecordEntity::getHandleTime).reversed()).collect(Collectors.toList());

                    List<UserItem> items = new ArrayList<>();
                    for (OperatorEntity operator : operators) {
                        UserItem item = new UserItem();
                        UserEntity user = users.stream().filter(e -> ObjectUtil.equals(operator.getHandleId(), e.getId())).findFirst().orElse(null);
                        if (user != null) {
                            item.setHeadIcon(UploaderUtil.uploaderImg(user.getHeadIcon()));
                            item.setUserId(user.getId());
                            item.setUserName(user.getRealName());
                        } else {
                            if (ObjectUtil.equals(operator.getHandleId(), FlowNature.SYSTEM_CODE)) {
                                item.setHeadIcon(FlowNature.SYSTEM_HEAD_ICON);
                                item.setUserName(FlowNature.SYSTEM_NAME);
                                item.setUserId(FlowNature.SYSTEM_CODE);
                            }
                        }

                        if (null != operator.getHandleStatus() && null != operator.getHandleTime()) {
                            item.setHandleType(operator.getHandleStatus());
                        } else {
                            RecordEntity recordEntity1 = records.stream().sorted(Comparator.comparing(RecordEntity::getId).reversed())
                                    .filter(e -> ObjectUtil.equals(e.getOperatorId(), operator.getId())
                                            && ObjectUtil.equals(e.getNodeId(), operator.getNodeId())
                                            && !ObjectUtil.equals(e.getStatus(), FlowNature.INVALID)
                                            && !ObjectUtil.equals(e.getHandleType(), RecordEnum.BACK.getCode()))
                                    .findFirst().orElse(null);
                            if (null != recordEntity1) {
                                item.setHandleType(recordEntity1.getHandleType());
                            } else {
                                // -1.待审  -2.未审  -3.待办理  -4.未办理（暂未用到）
                                item.setHandleType(isProcessing ? -3 : -1);
                            }
                        }
                        items.add(item);
                    }
                    model.setApproverCount(items.size());
                    items = items.stream().sorted(Comparator.comparing(UserItem::getHandleType)).collect(Collectors.toList());
                    if (items.size() > 4) {
                        items = items.subList(0, 4);
                    }
                    model.setApprover(items);
                    progressList.add(model);
                }
            }
        }

        // 添加结束节点
        if (taskEntity.getEndTime() != null) {
            ProgressModel model = new ProgressModel();
            model.setId(RandomUtil.uuId());
            model.setStartTime(taskEntity.getEndTime().getTime());
            model.setNodeType(NodeEnum.END.getType());
            model.setNodeName(FlowNature.END_NAME);
            progressList.add(model);
        }
        return progressList;
    }
}

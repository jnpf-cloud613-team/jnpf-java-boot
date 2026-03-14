package jnpf.flowable.util;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jnpf.base.ActionResult;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.EventEnum;
import jnpf.flowable.enums.NodeStateEnum;
import jnpf.flowable.enums.RecordEnum;
import jnpf.flowable.mapper.*;
import jnpf.flowable.model.flowable.CompleteFo;
import jnpf.flowable.model.flowable.FlowAbleUrl;
import jnpf.flowable.model.message.FlowMsgModel;
import jnpf.flowable.model.record.NodeRecordModel;
import jnpf.flowable.model.task.FlowMethod;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.templatenode.nodejson.TemplateJsonModel;
import jnpf.flowable.model.util.FlowContextHolder;
import jnpf.flowable.model.util.FlowNature;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2025/1/9 14:19
 */
@Component
@RequiredArgsConstructor
public class OutsideUtil {


    private final MsgUtil msgUtil;

    private final OperatorUtil operatorUtil;

    private final ServiceUtil serviceUtil;

    private final ConditionUtil conditionUtil;

    private final FlowUtil flowUtil;

    private final FlowAbleUrl flowAbleUrl;

    private final RecordMapper recordMapper;

    private final TaskLineMapper taskLineMapper;

    private final EventLogMapper eventLogMapper;

    private final CandidatesMapper candidatesMapper;

    private final NodeRecordMapper nodeRecordMapper;

    /**
     * 外部节点重试
     */
    public boolean retry(String id) throws WorkFlowException {
        EventLogEntity entity = eventLogMapper.selectById(id);
        if (entity == null) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        String eventId = entity.getId();
        String taskId = entity.getTaskId();
        FlowModel flowModel = new FlowModel();
        flowUtil.setFlowModel(taskId, flowModel);
        String upNodeCode = entity.getUpNode();
        String nodeCode = entity.getNodeCode();
        String interfaceId = entity.getInterfaceId();
        Map<String, NodeModel> nodes = flowModel.getNodes();
        NodeModel nodeModel = nodes.get(nodeCode);
        if (nodeModel == null) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }
        Map<String, Object> formData = StringUtil.isNotEmpty(entity.getData()) ? JsonUtil.stringToMap(entity.getData()) : new HashMap<>();
        flowModel.setFormData(formData);
        flowModel.setNodeCode(entity.getNodeCode());
        Map<String, List<TemplateJsonModel>> outsideOptions = nodeModel.getOutsideOptions();
        List<TemplateJsonModel> templateJsonModelList = outsideOptions.get(upNodeCode) != null ? outsideOptions.get(upNodeCode) : new ArrayList<>();
        Map<String, String> parameterData = operatorUtil.outsideData(flowModel, templateJsonModelList, new HashMap<>(), entity.getUpNode(), eventId);
        ActionResult<Object> result = serviceUtil.infoToId(interfaceId, parameterData);
        boolean retryResult = Objects.equals(200, result.getCode());
        entity.setStatus(retryResult ? FlowNature.SUCCESS : FlowNature.LOSE);
        entity.setResult(JsonUtil.getObjectToString(result));
        eventLogMapper.updateById(entity);
        return retryResult;
    }

    /**
     * 外部节点审批
     */
    @DSTransactional
    public FlowModel outsideAudit(FlowModel flowModel, EventLogEntity eventLog) throws WorkFlowException {
        if (eventLog == null) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }

        String taskId = eventLog.getTaskId();
        //赋值审批对象
        flowUtil.setFlowModel(taskId, flowModel);

        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                .filter(e -> StringUtils.equals(e.getNodeCode(), eventLog.getNodeCode())).findFirst().orElse(null);
        if (nodeEntity == null) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }
        String nodeId = eventLog.getNodeId();
        flowModel.setNodeEntity(nodeEntity);
        TaskEntity taskEntity = flowModel.getTaskEntity();
        flowUtil.isSuspend(taskEntity);
        flowUtil.isCancel(taskEntity);
        String deploymentId = flowModel.getDeploymentId();
        flowModel.setFlowableTaskId(nodeId);
        Map<String, NodeModel> nodes = flowModel.getNodes();
        String nodeCode = nodeEntity.getNodeCode();
        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setType(RecordEnum.AUDIT.getCode());
        flowModel.setEventStatus(EventEnum.APPROVE.getStatus());
        flowMethod.setTaskEntity(taskEntity);
        flowMethod.setNodeEntity(nodeEntity);
        NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
        //接口表单数据
        Map<String, Object> formData = new HashMap<>();
        List<FieLdsModel> formFieldList = nodeModel.getFormFieldList();
        Map<String, Object> dataMap = flowModel.getFormData();
        for (FieLdsModel fieLdsModel : formFieldList) {
            String id = fieLdsModel.getId();
            formData.put(id, dataMap.get(id));
        }
        flowModel.setFormData(formData);
        flowMethod.setNodeModel(nodeModel);
        flowMethod.setFlowableTaskId(nodeId);
        flowMethod.setFormData(formData);
        flowMethod.setSignFor(false);
        flowMethod.setFlowModel(flowModel);

        OperatorEntity operator = JsonUtil.getJsonToBean(eventLog, OperatorEntity.class);
        operator.setHandleId(eventLog.getCreatorUserId());

        Integer handleStatus = FlowNature.AUDIT_COMPLETION;
        flowMethod.setTaskEntity(taskEntity);
        flowMethod.setNodeEntity(nodeEntity);
        flowMethod.setNodeEntityList(nodeEntityList);
        flowMethod.setDeploymentId(deploymentId);
        flowMethod.setNodes(nodes);
        flowMethod.setNodeCode(nodeCode);
        flowMethod.setHandleStatus(handleStatus);
        flowModel.setEventStatus(EventEnum.APPROVE.getStatus());

        Map<String, Boolean> resMap = conditionUtil.handleCondition(flowMethod);
        conditionUtil.checkCondition(resMap, nodes);
        taskLineMapper.create(taskEntity.getId(), resMap);

        // 完成
        CompleteFo fo = new CompleteFo();
        fo.setTaskId(operator.getNodeId());
        fo.setVariables(new HashMap<>(resMap));
        flowAbleUrl.complete(fo);

        // 记录
        FlowMethod method = new FlowMethod();
        method.setFlowModel(flowModel);
        method.setType(RecordEnum.AUDIT.getCode());
        method.setOperatorEntity(operator);
        recordMapper.createRecord(method);

        // 节点记录
        NodeRecordModel nodeRecordModel = new NodeRecordModel();
        nodeRecordModel.setTaskId(operator.getTaskId());
        nodeRecordModel.setNodeId(operator.getNodeId());
        nodeRecordModel.setNodeCode(operator.getNodeCode());
        nodeRecordModel.setNodeName(operator.getNodeName());
        nodeRecordModel.setNodeStatus(ObjectUtil.equals(handleStatus, FlowNature.AUDIT_COMPLETION) ? NodeStateEnum.PASS.getCode() : NodeStateEnum.REJECT.getCode());
        nodeRecordMapper.create(nodeRecordModel);

        // 生成下一节点
        List<OperatorEntity> entityList = operatorUtil.handleOperator(flowModel);
        // 删除选择分支
        candidatesMapper.deleteBranch(operator.getTaskId(), operator.getNodeCode());

        // 判断任务是否结束
        operatorUtil.isFinished(flowModel);

        // 消息
        FlowMsgModel flowMsgModel = new FlowMsgModel();
        flowMsgModel.setNodeList(nodeEntityList);
        flowMsgModel.setCirculateList(new ArrayList<>());
        flowMsgModel.setUserInfo(flowModel.getUserInfo());
        flowMsgModel.setTaskEntity(taskEntity);
        flowMsgModel.setNodeCode(nodeEntity.getNodeCode());
        flowMsgModel.setOperatorList(entityList);
        if (Boolean.TRUE.equals(flowModel.getCopyMsgFlag())) {
            flowMsgModel.setCopy(true);
        }

        flowMsgModel.setApprove(true);

        flowMsgModel.setFormData(FlowContextHolder.getAllData());
        msgUtil.message(flowMsgModel);

        // 系统审批
        operatorUtil.systemAudit();

        FlowContextHolder.deleteFormOperator();
        serviceUtil.handleFormData(taskEntity.getFlowId(), true);

        return flowModel;
    }

}

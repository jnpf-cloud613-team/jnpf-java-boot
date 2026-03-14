package jnpf.flowable.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.google.common.collect.ImmutableList;
import jnpf.base.Pagination;
import jnpf.base.model.flow.FlowFormDataModel;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.*;
import jnpf.flowable.job.FlowJobUtil;
import jnpf.flowable.mapper.*;
import jnpf.flowable.model.candidates.CandidateCheckFo;
import jnpf.flowable.model.candidates.CandidateCheckVo;
import jnpf.flowable.model.candidates.CandidateUserVo;
import jnpf.flowable.model.flowable.*;
import jnpf.flowable.model.message.FlowMsgModel;
import jnpf.flowable.model.operator.AddSignModel;
import jnpf.flowable.model.operator.FlowBatchModel;
import jnpf.flowable.model.operator.OperatorVo;
import jnpf.flowable.model.record.NodeRecordModel;
import jnpf.flowable.model.task.FlowMethod;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.task.TaskPagination;
import jnpf.flowable.model.templatejson.TemplateJsonInfoVO;
import jnpf.flowable.model.templatenode.BackNodeModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.util.FlowContextHolder;
import jnpf.flowable.model.util.FlowNature;
import jnpf.flowable.service.OperatorService;
import jnpf.flowable.util.*;
import jnpf.model.FlowWorkListVO;
import jnpf.model.FlowWorkModel;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.user.WorkHandoverModel;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 15:30
 */
@Service
@RequiredArgsConstructor
public class OperatorServiceImpl extends SuperServiceImpl<OperatorMapper, OperatorEntity> implements OperatorService {

    private final FlowAbleUrl flowAbleUrl;

    private final OperatorUtil operatorUtil;

    private final FlowUtil flowUtil;

    private final ButtonUtil buttonUtil;

    private final RedisUtil redisUtil;

    private final MsgUtil msgUtil;

    private final ServiceUtil serviceUtil;


    private final TaskMapper taskMapper;

    private final TemplateNodeMapper templateNodeMapper;

    private final TemplateJsonMapper templateJsonMapper;

    private final CandidatesMapper candidatesMapper;

    private final RejectDataMapper rejectDataMapper;

    private final RecordMapper recordMapper;

    private final LaunchUserMapper launchUserMapper;

    private final TemplateMapper templateMapper;

    private final NodeRecordMapper nodeRecordMapper;

    private final RevokeMapper revokeMapper;

    private final EventLogMapper eventLogMapper;

    @Override
    public OperatorEntity getInfo(String id) throws WorkFlowException {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public List<OperatorEntity> getList(String taskId) {
        return this.baseMapper.getList(taskId);
    }

    @Override
    public List<OperatorVo> getList(TaskPagination pagination) {
        return flowUtil.getOperatorList(pagination);
    }

    @Override
    public List<OperatorEntity> handleOperator(FlowModel flowModel)  throws WorkFlowException{
        return operatorUtil.handleOperator(flowModel);
    }

    @DSTransactional
    @Override
    public void auditWithCheck(String id, FlowModel flowModel) throws WorkFlowException{
        this.audit(id, flowModel);
    }

    @Override
    public void audit(String id, FlowModel flowModel) throws WorkFlowException {

        OperatorEntity operator = operatorUtil.checkOperator(id);
        flowUtil.setFlowModel(operator.getTaskId(), flowModel);
        TaskEntity taskEntity = flowModel.getTaskEntity();
        String handleOpinion = flowModel.getHandleOpinion();
        String signId = flowModel.getSignId();
        String signImg = flowModel.getSignImg();
        Boolean useSignNext = flowModel.getUseSignNext();
        Map<String, Object> formData = flowModel.getFormData();
        Map<String, NodeModel> nodes = flowModel.getNodes();
        NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
        NodeModel nodeModel = nodes.get(operator.getNodeCode());
        TemplateJsonEntity jsonEntity = flowModel.getJsonEntity();
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                .filter(e -> StringUtils.equals(e.getNodeCode(), operator.getNodeCode())).findFirst().orElse(null);
        String formId = null != nodeEntity ? nodeEntity.getFormId() : "";
        // 获取流程参数
        operatorUtil.getGlobalParam(taskEntity, nodeModel, global, formData);
        flowModel.setTaskEntity(taskEntity);
        RevokeEntity revokeEntity = revokeMapper.getRevokeTask(taskEntity.getId());
        if (CollUtil.isNotEmpty(formData) && null == revokeEntity) {
            FlowFormDataModel model = new FlowFormDataModel();
            model.setFormId(formId);
            model.setId(taskEntity.getId());
            model.setFormOperates(nodeModel.getFormOperates());
            model.setMap(formData);
            model.setFlowId(jsonEntity.getId());
            serviceUtil.saveOrUpdateFormData(model);
        }
        formData = serviceUtil.infoData(formId, taskEntity.getId());
        // 表单
        if (CollUtil.isNotEmpty(formData)) {
            flowModel.setFormData(formData);
            // 保存表单到线程
            FlowContextHolder.addChildData(taskEntity.getId(), formId, formData, nodeModel.getFormOperates(), false);
        }
        this.audit(operator, flowModel);
        if (Boolean.TRUE.equals(useSignNext)) {
            serviceUtil.updateSign(signId, signImg);
        }
        serviceUtil.addCommonWordsNum(handleOpinion);
        // 保存表单数据
        FlowContextHolder.deleteFormOperator();
        serviceUtil.handleFormData(taskEntity.getFlowId(), true);
    }

    @Override
    public void audit(OperatorEntity operator, FlowModel flowModel) throws WorkFlowException {
        operatorUtil.audit(operator, flowModel);
    }

    @Override
    public void sign(FlowModel flowModel) throws WorkFlowException {
        UpdateWrapper<OperatorEntity> updateWrapper = new UpdateWrapper<>();
        if (CollUtil.isNotEmpty(flowModel.getIds())) {
            List<String> ids = new ArrayList<>();
            if (flowModel.getType().equals(0)) {
                operatorUtil.checkBatch(flowModel.getIds(), ids, false);
                List<String> list = new ArrayList<>();
                operatorUtil.checkCancel(ids, list);
                // 签收
                updateWrapper.lambda().in(OperatorEntity::getId, list)
                        .set(OperatorEntity::getSignTime, new Date());
            } else {
                operatorUtil.checkBatch(flowModel.getIds(), ids, true);
                List<String> list = new ArrayList<>();
                operatorUtil.checkCancel(ids, list);
                // 退签
                updateWrapper.lambda().in(OperatorEntity::getId, list)
                        .set(OperatorEntity::getSignTime, null);
            }
            this.update(updateWrapper);
        }
    }

    @Override
    public void startHandle(FlowModel flowModel) throws WorkFlowException {
        List<String> ids = new ArrayList<>();
        operatorUtil.checkBatch(flowModel.getIds(), ids, false);
        List<String> list = new ArrayList<>();
        operatorUtil.checkCancel(ids, list);

        UpdateWrapper<OperatorEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().in(OperatorEntity::getId, list)
                .set(OperatorEntity::getStartHandleTime, new Date());
        this.update(updateWrapper);
    }

    @Override
    public void saveAudit(String id, FlowModel flowModel) throws WorkFlowException {
        OperatorEntity entity = this.getById(id);
        if (null == entity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        TaskEntity taskEntity = taskMapper.getInfo(entity.getTaskId());
        operatorUtil.checkTemplateHide(taskEntity.getTemplateId());
        flowUtil.isSuspend(taskEntity);
        flowUtil.isCancel(taskEntity);
        entity.setDraftData(JsonUtil.getObjectToString(flowModel.getFormData()));
        this.updateById(entity);
    }

    @DSTransactional
    @Override
    public void addSign(String id, FlowModel flowModel) throws WorkFlowException {
        OperatorEntity operator = operatorUtil.checkOperator(id);
        List<OperatorEntity> childList = operatorUtil.getChildList(operator.getId());
        childList = childList.stream().filter(e -> ObjectUtil.equals(e.getCompletion(), FlowNature.NORMAL) && !ObjectUtil.equals(e.getStatus(), OperatorStateEnum.FUTILITY.getCode())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(childList)) {
            throw new WorkFlowException(MsgCode.WF081.get());
        }
        AddSignModel parameter = flowModel.getAddSignParameter() != null ? flowModel.getAddSignParameter() : new AddSignModel();
        List<String> idList = parameter.getAddSignUserIdList();
        buttonUtil.checkAddSign(operator, parameter);
        // 保存选择分支
        List<String> branchList = flowModel.getBranchList();
        if (CollUtil.isNotEmpty(branchList)) {
            candidatesMapper.createBranch(branchList, operator);
        }
        if (idList.isEmpty() || operator.getStatus().equals(OperatorStateEnum.TRANSFER.getCode())
                || operator.getStatus().equals(OperatorStateEnum.ASSIST.getCode())) {
            throw new WorkFlowException(MsgCode.WF081.get());
        }
        String userId = UserProvider.getLoginUserId();
        if (idList.contains(userId)) {
            throw new WorkFlowException(MsgCode.WF106.get());
        }
        if (!ObjectUtil.equals(operator.getHandleId(), userId)) {
            List<DelegateEntity> delegateList = flowUtil.getByToUserId(userId);
            List<String> userIds = delegateList.stream().map(DelegateEntity::getUserId).distinct().collect(Collectors.toList());
            List<String> filterList = userIds.stream().filter(idList::contains).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(filterList)) {
                throw new WorkFlowException(MsgCode.WF116.get());
            }
        }
        // 将之前的加签经办作废
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId()).eq(OperatorEntity::getParentId, id)
                .and(i -> i.eq(OperatorEntity::getStatus, OperatorStateEnum.ADD_SIGN.getCode())
                        .or().eq(OperatorEntity::getStatus, OperatorStateEnum.RECALL.getCode()));
        List<OperatorEntity> list = this.list(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            list.forEach(e -> e.setStatus(OperatorStateEnum.FUTILITY.getCode()));
            super.updateBatchById(list);
        }
        flowUtil.setFlowModel(operator.getTaskId(), flowModel);
        TaskEntity taskEntity = flowModel.getTaskEntity();
        operatorUtil.checkTemplateHide(taskEntity.getTemplateId());
        flowUtil.isSuspend(taskEntity);
        flowUtil.isCancel(taskEntity);
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        Map<String, Object> formData = flowModel.getFormData();
        // 表单
        if (CollUtil.isNotEmpty(formData)) {
            TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                    .filter(e -> StringUtils.equals(e.getNodeCode(), operator.getNodeCode())).findFirst().orElse(null);
            if (null != nodeEntity) {
                NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
                // 保存表单到线程
                FlowContextHolder.addChildData(taskEntity.getId(), nodeEntity.getFormId(), formData, nodeModel.getFormOperates(), true);
            }
        }
        // 保存候选人、异常人
        flowUtil.create(flowModel, operator.getTaskId(), nodeEntityList, operator);
        // 更新经办信息
        operator.setHandleParameter(JsonUtil.getObjectToString(parameter));
        if (parameter.getAddSignType().equals(FlowNature.LATER)) {
            operator.setHandleStatus(FlowNature.AUDIT_COMPLETION);
        }
        operator.setCompletion(FlowNature.ACTION);
        if (this.updateById(operator)) {
            TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                    .filter(e -> e.getNodeCode().equals(operator.getNodeCode())).findFirst().orElse(null);
            if (null == nodeEntity) {
                throw new WorkFlowException(MsgCode.WF076.get());
            }

            NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
            // 全局节点
            TemplateNodeEntity globalEntity = nodeEntityList.stream()
                    .filter(e -> StringUtils.equals(NodeEnum.GLOBAL.getType(), e.getNodeType())).findFirst().orElse(new TemplateNodeEntity());
            NodeModel global = JsonUtil.getJsonToBean(globalEntity.getNodeJson(), NodeModel.class);
            Date date = new Date();
            // 后加签默认同意的记录
            if (parameter.getAddSignType().equals(FlowNature.LATER)) {
                FlowMethod flowMethod = new FlowMethod();
                flowMethod.setType(RecordEnum.AUDIT.getCode());
                flowMethod.setFlowModel(flowModel);
                operator.setHandleTime(date);
                flowMethod.setOperatorEntity(operator);
                recordMapper.createRecord(flowMethod);
            }
            // 生成加签的经办
            List<OperatorEntity> entityList = new ArrayList<>();
            for (String handleId : idList) {
                FlowMethod flowMethod = new FlowMethod();
                flowMethod.setTaskEntity(taskEntity);
                flowMethod.setNodeEntity(nodeEntity);
                flowMethod.setNodeModel(nodeModel);
                flowMethod.setSignFor(global.getHasSignFor());
                flowMethod.setFlowableTaskId(operator.getNodeId());
                OperatorEntity entity = operatorUtil.createOperatorEntity(flowMethod);
                entity.setHandleId(handleId);
                entity.setParentId(operator.getId());
                entity.setStatus(OperatorStateEnum.ADD_SIGN.getCode());
                entityList.add(entity);
                if (ObjectUtil.equals(parameter.getCounterSign(), FlowNature.IMPROPER_APPROVER)) {
                    break;
                }
            }
            // 加签记录
            FlowMethod flowMethod = new FlowMethod();
            flowMethod.setFlowModel(flowModel);
            operator.setHandleTime(date);
            flowMethod.setOperatorEntity(operator);
            Integer type = RecordEnum.ADD_SIGN.getCode();
            flowMethod.setType(type);
            flowMethod.setUserId(String.join(",", idList));
            recordMapper.createRecord(flowMethod);
            if (CollUtil.isNotEmpty(entityList)) {
                super.saveBatch(entityList);
                // 消息
                FlowMsgModel flowMsgModel = new FlowMsgModel();
                flowMsgModel.setOperatorList(entityList);
                flowMsgModel.setNodeList(nodeEntityList);
                flowMsgModel.setUserInfo(flowModel.getUserInfo());
                flowMsgModel.setTaskEntity(taskEntity);
                flowMsgModel.setNodeCode(operator.getNodeCode());
                flowMsgModel.setFormData(FlowContextHolder.getAllData());
                msgUtil.message(flowMsgModel);
                operatorUtil.addOperatorList(entityList, flowModel);
            }
        }
        serviceUtil.handleFormData(taskEntity.getFlowId(), false);
    }

    @Override
    public List<CandidateUserVo> getReduceList(String id, Pagination pagination) throws WorkFlowException {
        RecordEntity recordEntity = recordMapper.getInfo(id);
        if (null == recordEntity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        OperatorEntity operator = this.getById(recordEntity.getOperatorId());
        if (null == operator) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        if (null == operator.getHandleParameter()) {
            throw new WorkFlowException(MsgCode.WF082.get());
        }

        AddSignModel model = JsonUtil.getJsonToBean(operator.getHandleParameter(), AddSignModel.class);

        List<CandidateUserVo> list = new ArrayList<>();

        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId()).eq(OperatorEntity::getParentId, operator.getId())
                .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode())
                .ne(OperatorEntity::getStatus, OperatorStateEnum.ASSIST.getCode());
        List<OperatorEntity> entityList = this.list(queryWrapper);
        if (CollUtil.isEmpty(entityList)) {
            return list;
        }

        // 获取未审批的加签经办
        List<OperatorEntity> todoList = entityList.stream().filter(e -> null == e.getHandleStatus()
                && ObjectUtil.equals(e.getCompletion(), FlowNature.NORMAL)).collect(Collectors.toList());

        // 依次审批，获取未创建经办的人员
        if (ObjectUtil.equals(model.getCounterSign(), FlowNature.IMPROPER_APPROVER) && CollUtil.isNotEmpty(todoList)) {
            List<String> idList = model.getAddSignUserIdList();
            OperatorEntity operatorEntity = todoList.get(0);
            int index = idList.indexOf(operatorEntity.getHandleId());
            if (index < idList.size() - 1) {
                for (int i = index + 1; i < idList.size(); i++) {
                    OperatorEntity op = new OperatorEntity();
                    op.setHandleId(idList.get(i));
                    todoList.add(op);
                }
            }
        }

        return operatorUtil.getReduceUsers(todoList, pagination);
    }

    // 减签
    @Override
    @DSTransactional
    public void reduce(String id, FlowModel flowModel) throws WorkFlowException {
        List<String> ids = flowModel.getIds();
        RecordEntity recordEntity = recordMapper.getInfo(id);
        if (null == recordEntity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        OperatorEntity operator = this.getById(recordEntity.getOperatorId());
        if (null == operator) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }

        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId()).eq(OperatorEntity::getParentId, operator.getId())
                .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode())
                .ne(OperatorEntity::getStatus, OperatorStateEnum.ASSIST.getCode());
        List<OperatorEntity> list = this.list(queryWrapper);
        if (CollUtil.isEmpty(list)) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        if (CollUtil.isNotEmpty(ids)) {
            String userId = ids.get(0);
            OperatorEntity operatorEntity = list.stream()
                    .filter(e -> ObjectUtil.equals(e.getHandleId(), userId) && e.getHandleTime() != null).findFirst().orElse(null);
            if (null != operatorEntity && null != operatorEntity.getHandleStatus()) {
                throw new WorkFlowException(MsgCode.WF128.get());
            }
        }
        AddSignModel model = JsonUtil.getJsonToBean(operator.getHandleParameter(), AddSignModel.class);
        boolean inTurn = ObjectUtil.equals(model.getCounterSign(), FlowNature.IMPROPER_APPROVER);

        List<OperatorEntity> unApproved = list.stream()
                .filter(e -> ObjectUtil.equals(e.getCompletion(), FlowNature.NORMAL) && e.getHandleTime() == null).collect(Collectors.toList());
        if (inTurn) {
            // 依次审批的处理
            List<String> idList = model.getAddSignUserIdList();
            OperatorEntity entity = unApproved.stream().filter(e -> ids.contains(e.getHandleId())).findFirst().orElse(null);
            if (null != entity) {
                flowUtil.setFlowModel(operator.getTaskId(), flowModel);
                Map<String, NodeModel> nodes = flowModel.getNodes();
                NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());

                int index = idList.indexOf(entity.getHandleId());
                if (index == idList.size() - 1) {
                    throw new WorkFlowException(MsgCode.WF109.get());
                }
                String handleId = idList.get(index + 1);
                // 创建下一个经办
                OperatorEntity newOperator = operatorUtil.createOperator(entity, OperatorStateEnum.ADD_SIGN.getCode(), handleId, global);
                this.save(newOperator);
                FlowMethod flowMethod = new FlowMethod();
                flowMethod.setFlowModel(flowModel);
                flowMethod.setAddSignModel(model);
                flowMethod.setOperatorEntity(operator);
                flowMethod.setTaskEntity(flowModel.getTaskEntity());
                List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
                flowMethod.setNodeEntityList(nodeEntityList);
                TemplateNodeEntity nodeEntity = nodeEntityList.stream().filter(e -> ObjectUtil.equals(e.getNodeCode(), operator.getNodeCode()))
                        .findFirst().orElse(null);
                flowMethod.setNodeEntity(nodeEntity);
                operatorUtil.improperApproverMessage(flowMethod, newOperator);
                // 删除需要减签的经办
                this.removeById(entity);
            }
            // 更新handleParameter
            idList.removeAll(ids);
            model.setAddSignUserIdList(idList);
            operator.setHandleParameter(JsonUtil.getObjectToString(model));
            this.updateById(operator);
        } else {
            if (unApproved.size() == 1) {
                throw new WorkFlowException(MsgCode.WF109.get());
            }
        }
        // 记录
        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setFlowModel(flowModel);
        flowMethod.setOperatorEntity(operator);
        Integer type = RecordEnum.SUBTRACT_SIGN.getCode();
        flowMethod.setType(type);
        flowMethod.setUserId(String.join(",", ids));
        recordMapper.createRecord(flowMethod);

        if (inTurn) {
            return;
        }

        List<String> handleIds = list.stream().map(OperatorEntity::getHandleId).collect(Collectors.toList());

        handleIds.removeAll(ids);

        if (handleIds.isEmpty()) {
            // 全部减签
            this.removeByIds(list);
            // 还原经办
            UpdateWrapper<OperatorEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.lambda().eq(OperatorEntity::getId, operator.getId())
                    .set(OperatorEntity::getCompletion, FlowNature.NORMAL)
                    .set(OperatorEntity::getHandleParameter, null);
            this.update(updateWrapper);
        } else {
            List<OperatorEntity> collect = list.stream().filter(t -> ids.contains(t.getHandleId())).collect(Collectors.toList());
            this.removeByIds(collect);
        }
    }

    // 获取可退回的节点
    @Override
    public List<BackNodeModel> getFallbacks(String id) throws WorkFlowException {
        OperatorEntity operator = this.getInfo(id);
        if (null == operator) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        TaskEntity taskEntity = taskMapper.getInfo(operator.getTaskId());
        if (null == taskEntity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        TemplateJsonEntity jsonEntity = templateJsonMapper.getInfo(taskEntity.getFlowId());
        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(taskEntity.getFlowId());
        Map<String, NodeModel> nodes = new HashMap<>();
        for (TemplateNodeEntity nodeEntity : nodeEntityList) {
            nodes.put(nodeEntity.getNodeCode(), JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class));
        }

        NodeModel nodeModel = nodes.get(operator.getNodeCode());
        List<BackNodeModel> list = new ArrayList<>();

        // 获取经办过滤
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId());
        List<OperatorEntity> operatorEntityList = this.list(queryWrapper);
        List<String> nodeList = operatorEntityList.stream().map(OperatorEntity::getNodeCode).distinct().collect(Collectors.toList());

        TemplateNodeEntity start = nodeEntityList.stream().filter(e -> e.getNodeType().equals(NodeEnum.START.getType())).findFirst().orElse(null);
        if (null == start) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }
        NodeModel startModel = JsonUtil.getJsonToBean(start.getNodeJson(), NodeModel.class);
        if (ObjectUtil.equals(nodeModel.getBackNodeCode(), FlowNature.START)) {
            BackNodeModel model = new BackNodeModel();
            model.setNodeName(startModel.getNodeName());
            model.setNodeCode(startModel.getNodeId());
            list.add(model);
        } else if (ObjectUtil.equals(nodeModel.getBackNodeCode(), FlowNature.UP)) {
            List<String> resList = new ArrayList<>();
            List<String> nodeCodeList = new ArrayList<>();
            flowUtil.prevNode(jsonEntity.getFlowableId(), operator.getNodeCode(), nodeEntityList, nodeCodeList);

            List<String> nodeCodes = this.baseMapper.getList(taskEntity.getId()).stream()
                    .filter(e -> !e.getStatus().equals(OperatorStateEnum.FUTILITY.getCode()))
                    .map(OperatorEntity::getNodeCode).distinct().collect(Collectors.toList());
            nodeCodes.add(start.getNodeCode());

            for (String nodeCode : nodeCodeList) {
                NodeModel node = nodes.get(nodeCode);
                // 退回节点包含子流程
                if (null != node) {
                    if (NodeEnum.SUB_FLOW.getType().equals(node.getType())) {
                        throw new WorkFlowException(MsgCode.WF046.get());
                    }
                    if (NodeEnum.OUTSIDE.getType().equals(node.getType())) {
                        throw new WorkFlowException(MsgCode.WF154.get());
                    }
                }

                if (!nodeCodes.contains(nodeCode)) {
                    continue;
                }
                resList.add(nodeCode);
            }

            if (resList.isEmpty()) {
                resList.addAll(nodeCodeList);
            }

            BackNodeModel model = new BackNodeModel();
            model.setNodeName("上级审批节点");
            model.setNodeCode(String.join(",", resList));
            list.add(model);
        } else if (ObjectUtil.equals(nodeModel.getBackNodeCode(), FlowNature.REJECT)) {
            List<String> fallbacks = flowAbleUrl.getFallbacks(operator.getNodeId());
            nodeList.add(start.getNodeCode());
            fallbacks = fallbacks.stream().filter(nodeList::contains).collect(Collectors.toList());
            // 获取经办进行过滤
            List<String> nodeCodeList = this.baseMapper.getList(taskEntity.getId()).stream()
                    .filter(e -> !e.getStatus().equals(OperatorStateEnum.FUTILITY.getCode()))
                    .map(OperatorEntity::getNodeCode).distinct().collect(Collectors.toList());
            nodeCodeList.add(start.getNodeCode());

            for (String nodeCode : fallbacks) {
                // 过滤没有走过的点
                if (!nodeCodeList.contains(nodeCode) || ObjectUtil.equals(nodeCode, operator.getNodeCode())) {
                    continue;
                }
                NodeModel node = nodes.get(nodeCode);
                BackNodeModel model = new BackNodeModel();
                model.setNodeName(node.getNodeName());
                model.setNodeCode(node.getNodeId());
                list.add(model);
            }
        } else {
            NodeModel optional = nodes.get(nodeModel.getBackNodeCode());
            if (null != optional) {
                BackNodeModel model = new BackNodeModel();
                model.setNodeName(optional.getNodeName());
                model.setNodeCode(nodeModel.getBackNodeCode());
                list.add(model);
            }
        }
        return list;
    }

    // 退回
    @DSTransactional
    @Override
    public void back(String id, FlowModel flowModel) throws WorkFlowException {
        operatorUtil.back(id, flowModel);
    }

    // 撤回
    @DSTransactional
    @Override
    public void recall(String id, FlowModel flowModel) throws WorkFlowException {
        RecordEntity recordEntity = flowModel.getRecordEntity();
        OperatorEntity operator = flowModel.getOperatorEntity();

        flowUtil.setFlowModel(operator.getTaskId(), flowModel);
        TaskEntity taskEntity = flowModel.getTaskEntity();
        operatorUtil.checkTemplateHide(taskEntity.getTemplateId());
        flowUtil.isSuspend(taskEntity);
        flowUtil.isCancel(taskEntity);
        flowUtil.isTrigger(taskEntity);
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        if (CollUtil.isEmpty(nodeEntityList)) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }
        TemplateNodeEntity nodeEntity = nodeEntityList.stream().filter(e -> e.getNodeCode().equals(operator.getNodeCode())).findFirst().orElse(null);
        if (null == nodeEntity) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }

        flowModel.setNodeEntity(nodeEntity);
        flowModel.setRecordEntity(recordEntity);
        flowModel.setFlag(FlowNature.APPROVAL_FLAG);
        flowModel.setIsException(true);
        if (!buttonUtil.checkRecall(flowModel)) {
            throw new WorkFlowException(MsgCode.WF077.get());
        }
        operatorUtil.addTask(ImmutableList.of(taskEntity.getId()));
        List<String> nextCodes = flowModel.getNextCodes();
        if (CollUtil.isNotEmpty(nextCodes)) {
            // 删除外部节点
            eventLogMapper.delete(taskEntity.getId(), nextCodes);
            // 删除子流程
            operatorUtil.deleteSubflow(taskEntity.getId(), nextCodes);
        }

        // 更新记录状态
        recordEntity.setStatus(FlowNature.INVALID);
        recordMapper.updateById(recordEntity);

        // 记录
        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setType(RecordEnum.RECALL.getCode());
        flowMethod.setFlowModel(flowModel);
        operator.setHandleTime(null);
        flowMethod.setOperatorEntity(operator);
        recordMapper.createRecord(flowMethod);

        List<String> currentNodes = new ArrayList<>();
        String currentNodeCode = taskEntity.getCurrentNodeCode();
        if (StringUtils.isNotBlank(currentNodeCode)) {
            currentNodes = Arrays.stream(currentNodeCode.split(",")).collect(Collectors.toList());
        }
        if (currentNodes.contains(operator.getNodeCode())) {
            // 加签 撤回的处理
            if (!ObjectUtil.equals(operator.getParentId(), FlowNature.PARENT_ID)) {
                OperatorEntity parentEntity = this.getById(operator.getParentId());
                if (null != parentEntity && ObjectUtil.equals(parentEntity.getCompletion(), FlowNature.NORMAL)) {
                    parentEntity.setCompletion(FlowNature.ACTION);
                    this.updateById(parentEntity);
                }

            }
            // 当前节点已审批的人 撤回无需跳转
            UpdateWrapper<OperatorEntity> wrapper = new UpdateWrapper<>();
            wrapper.lambda().eq(OperatorEntity::getId, operator.getId())
                    .set(OperatorEntity::getCompletion, FlowNature.NORMAL)
                    .set(OperatorEntity::getStatus, OperatorStateEnum.RECALL.getCode())
                    .set(OperatorEntity::getHandleStatus, null)
                    .set(OperatorEntity::getCreatorTime, new Date())
                    .set(OperatorEntity::getHandleTime, null);
            this.update(wrapper);

            //删除其他经办
            if (ObjectUtil.equals(operator.getParentId(), FlowNature.PARENT_ID)) {
                // 依次审批
                if (StringUtils.isNotBlank(operator.getHandleAll())) {
                    operatorUtil.deleteInTurnOperator(operator, operator.getHandleId());
                }
                //逐级审批
                operatorUtil.deleteStepOperator(operator);
            }
            return;
        }
        // 节点记录
        NodeRecordModel nodeRecordModel = new NodeRecordModel();
        nodeRecordModel.setTaskId(operator.getTaskId());
        nodeRecordModel.setNodeId(operator.getNodeId());
        nodeRecordModel.setNodeCode(operator.getNodeCode());
        nodeRecordModel.setNodeName(operator.getNodeName());
        nodeRecordModel.setNodeStatus(NodeStateEnum.RECALL.getCode());
        nodeRecordMapper.update(nodeRecordModel);

        // 删除该节点下一级的经办
        if (CollUtil.isNotEmpty(nextCodes)) {
            QueryWrapper<OperatorEntity> wrapper = new QueryWrapper<>();
            wrapper.lambda().eq(OperatorEntity::getTaskId, taskEntity.getId())
                    .in(OperatorEntity::getNodeCode, nextCodes);
            List<OperatorEntity> list = this.list(wrapper);
            if (CollUtil.isNotEmpty(list)) {
                this.remove(wrapper);
                // 删除超时相关
                FlowJobUtil.deleteByOperatorId(operator.getId(), redisUtil);
                for (OperatorEntity operatorEntity : list) {
                    FlowJobUtil.deleteByOperatorId(operatorEntity.getId(), redisUtil);
                }
            }

            //逐级信息删除
            QueryWrapper<LaunchUserEntity> launchWrapper = new QueryWrapper<>();
            launchWrapper.lambda().eq(LaunchUserEntity::getType, FlowNature.STEP_INITIATION)
                    .eq(LaunchUserEntity::getTaskId, taskEntity.getId())
                    .in(LaunchUserEntity::getNodeCode, nextCodes);
            launchUserMapper.delete(launchWrapper);
        }

        // 撤回跳转
        JumpFo fo = new JumpFo();
        fo.setInstanceId(taskEntity.getInstanceId());
        List<String> executeNodes = this.getNodeCodeByGroupId(nextCodes, flowModel.getNodes());
        if (CollUtil.isNotEmpty(executeNodes)) {
            nextCodes.addAll(executeNodes);
        }
        fo.setSource(nextCodes);
        List<String> targetList = new ArrayList<>();
        targetList.add(nodeEntity.getNodeCode());
        fo.setTarget(targetList);
        flowAbleUrl.jump(fo);

        List<FlowableTaskModel> taskModelList = flowAbleUrl.getCurrentTask(taskEntity.getInstanceId());
        FlowableTaskModel model = taskModelList.stream().filter(e -> e.getTaskKey().equals(operator.getNodeCode())).findFirst().orElse(null);
        String nodeId = null;
        if (null != model) {
            nodeId = model.getTaskId();
        }

        // 还原经办（其它已审批、作废的经办无需还原）
        UpdateWrapper<OperatorEntity> wrapper = new UpdateWrapper<>();
        wrapper.lambda().eq(OperatorEntity::getId, operator.getId())
                .set(OperatorEntity::getCompletion, FlowNature.NORMAL)
                .set(OperatorEntity::getNodeId, nodeId)
                .set(OperatorEntity::getStatus, OperatorStateEnum.RECALL.getCode())
                .set(OperatorEntity::getHandleStatus, null)
                .set(OperatorEntity::getCreatorTime, new Date())
                .set(OperatorEntity::getHandleTime, null);
        this.update(wrapper);
        // 更新节点经办的nodeId
        wrapper.clear();
        wrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId())
                .eq(OperatorEntity::getNodeCode, operator.getNodeCode()).eq(OperatorEntity::getParentId, operator.getParentId())
                .set(OperatorEntity::getNodeId, nodeId);
        this.update(wrapper);

        List<String> userIds = new ArrayList<>();
        userIds.add(operator.getHandleId());
        // 获取未审批过的复原
        operatorUtil.recallRestore(operator, nodeId, userIds);

        // 删除该节点下一级的候选人，有修改操作的
        candidatesMapper.delete(taskEntity.getId(), nextCodes, userIds);

        // 超时
        List<OperatorEntity> entityList = new ArrayList<>();
        OperatorEntity entity = this.getById(operator.getId());
        entityList.add(entity);
        operatorUtil.addOperatorList(entityList, flowModel);

        // 获取当前节点信息
        operatorUtil.updateCurrentNode(taskModelList, flowModel.getNodes(), taskEntity);
    }

    // 根据组ID获取同组的执行节点
    public List<String> getNodeCodeByGroupId(List<String> nextCodes, Map<String, NodeModel> nodes) {
        List<String> list = new ArrayList<>();
        if (CollUtil.isEmpty(nextCodes) || CollUtil.isEmpty(nodes)) {
            return list;
        }
        for (String nextCode : nextCodes) {
            NodeModel nodeModel = nodes.get(nextCode);
            if (null != nodeModel && ObjectUtil.equals(nodeModel.getType(), NodeEnum.TRIGGER.getType())) {
                String groupId = nodeModel.getGroupId();
                for (Map.Entry<String, NodeModel> stringNodeModelEntry : nodes.entrySet()) {
                    String key = stringNodeModelEntry.getKey();
                    NodeModel model = nodes.get(key);
                    if (null != model && ObjectUtil.equals(model.getGroupId(), groupId)) {
                        list.add(key);
                    }
                }

            }
        }
        return list;
    }

    // 转审
    @Override
    public void transfer(String id, FlowModel flowModel) throws WorkFlowException {
        String transferUserId = flowModel.getHandleIds();
        OperatorEntity operator = operatorUtil.checkOperator(id);
        boolean isProcessing = ObjectUtil.equals(operator.getIsProcessing(), FlowNature.PROCESSING);
        if (StringUtils.isBlank(transferUserId) || operator.getStatus().equals(OperatorStateEnum.TRANSFER.getCode())
                || operator.getStatus().equals(OperatorStateEnum.ADD_SIGN.getCode())) {
            throw new WorkFlowException(isProcessing ? MsgCode.WF151.get() : MsgCode.WF084.get());
        }
        String userId = flowModel.getUserInfo() == null ? UserProvider.getLoginUserId() : flowModel.getUserInfo().getUserId();
        if (StringUtils.equals(transferUserId, userId)) {
            throw new WorkFlowException(isProcessing ? MsgCode.WF149.get() : MsgCode.WF107.get());
        }
        if (!ObjectUtil.equals(operator.getHandleId(), userId)) {
            List<DelegateEntity> delegateList = flowUtil.getByToUserId(userId);
            List<String> userIds = delegateList.stream().map(DelegateEntity::getUserId).distinct().collect(Collectors.toList());
            if (userIds.contains(transferUserId)) {
                throw new WorkFlowException(isProcessing ? MsgCode.WF150.get() : MsgCode.WF117.get());
            }
        }

        flowUtil.setFlowModel(operator.getTaskId(), flowModel);
        TaskEntity taskEntity = flowModel.getTaskEntity();
        operatorUtil.checkTemplateHide(taskEntity.getTemplateId());
        flowUtil.isSuspend(taskEntity);
        flowUtil.isCancel(taskEntity);

        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();

        TemplateNodeEntity globalEntity = nodeEntityList.stream()
                .filter(e -> StringUtils.equals(NodeEnum.GLOBAL.getType(), e.getNodeType())).findFirst().orElse(new TemplateNodeEntity());
        NodeModel global = JsonUtil.getJsonToBean(globalEntity.getNodeJson(), NodeModel.class);

        // 记录
        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setType(ObjectUtil.equals(operator.getIsProcessing(), FlowNature.NOT_PROCESSING) ? RecordEnum.TRANSFER.getCode() : RecordEnum.TRANSFER_PROCESSING.getCode());
        flowMethod.setFlowModel(flowModel);
        flowMethod.setOperatorEntity(operator);
        List<String> userIds = new ArrayList<>();
        userIds.add(transferUserId);
        flowMethod.setUserId(String.join(",", userIds));
        recordMapper.createRecord(flowMethod);

        // 更新经办
        UpdateWrapper<OperatorEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().eq(OperatorEntity::getId, operator.getId())
                .set(OperatorEntity::getHandleId, transferUserId)
                .set(OperatorEntity::getCreatorTime, new Date())
                .set(OperatorEntity::getDraftData, null)
                .set(OperatorEntity::getStatus, OperatorStateEnum.TRANSFER.getCode());
        Boolean flowTodo = serviceUtil.getFlowTodo();
        if (Boolean.FALSE.equals(flowTodo)) {
            updateWrapper.lambda().set(OperatorEntity::getStartHandleTime, null);
            Boolean flowSign = serviceUtil.getFlowSign();
            if (Boolean.TRUE.equals(!flowSign && global != null) && Boolean.TRUE.equals(global.getHasSignFor())) {
                updateWrapper.lambda().set(OperatorEntity::getSignTime, null);
            }

        }
        this.update(updateWrapper);

        // 消息
        FlowMsgModel flowMsgModel = new FlowMsgModel();
        List<OperatorEntity> list = new ArrayList<>();
        operator = this.getById(operator.getId());
        list.add(operator);
        flowMsgModel.setOperatorList(list);
        flowMsgModel.setNodeList(nodeEntityList);
        flowMsgModel.setUserInfo(flowModel.getUserInfo());
        flowMsgModel.setTaskEntity(taskEntity);
        flowMsgModel.setNodeCode(operator.getNodeCode());
        flowMsgModel.setFormData(FlowContextHolder.getAllData());
        flowMsgModel.setTransfer(true);
        msgUtil.message(flowMsgModel);

        // 删除超时相关
        FlowJobUtil.deleteByOperatorId(operator.getId(), redisUtil);
        operatorUtil.addOperatorList(list, flowModel);
    }

    // 协办， 新增经办，节点通过后，删除协办
    @Override
    public void assist(String id, FlowModel flowModel) throws WorkFlowException {
        String handleIds = flowModel.getHandleIds();
        List<String> ids = Arrays.stream(handleIds.split(",")).collect(Collectors.toList());
        String userId = UserProvider.getLoginUserId();
        if (ids.contains(userId)) {
            throw new WorkFlowException(MsgCode.WF108.get());
        }

        OperatorEntity operator = operatorUtil.checkOperator(id);

        if (!ObjectUtil.equals(operator.getHandleId(), userId)) {
            List<DelegateEntity> delegateList = flowUtil.getByToUserId(userId);
            List<String> userIds = delegateList.stream().map(DelegateEntity::getUserId).distinct().collect(Collectors.toList());
            List<String> filterList = userIds.stream().filter(ids::contains).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(filterList)) {
                throw new WorkFlowException(MsgCode.WF118.get());
            }
        }


        flowUtil.setFlowModel(operator.getTaskId(), flowModel);
        TaskEntity taskEntity = flowModel.getTaskEntity();
        operatorUtil.checkTemplateHide(taskEntity.getTemplateId());
        flowUtil.isSuspend(taskEntity);
        flowUtil.isCancel(taskEntity);

        Map<String, NodeModel> nodes = flowModel.getNodes();
        NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
        boolean signFor = false;
        if (null != global) {
            signFor = global.getHasSignFor();
        }
        List<UserEntity> userList = serviceUtil.getUserName(ids);
        if (CollUtil.isNotEmpty(userList)) {
            Boolean flowTodo = serviceUtil.getFlowTodo();
            Boolean flowSign = serviceUtil.getFlowSign();
            List<OperatorEntity> list = new ArrayList<>();
            NodeModel nodeModel = nodes.get(operator.getNodeCode());
            int processing = ObjectUtil.isNotEmpty(nodeModel) && ObjectUtil.equals(nodeModel.getType(), NodeEnum.PROCESSING.getType()) ? FlowNature.PROCESSING : FlowNature.NOT_PROCESSING;
            for (UserEntity user : userList) {
                OperatorEntity operatorEntity = new OperatorEntity();
                operatorEntity.setParentId(operator.getId());
                operatorEntity.setNodeName(operator.getNodeName());
                operatorEntity.setNodeCode(operator.getNodeCode());
                operatorEntity.setTaskId(operator.getTaskId());
                operatorEntity.setNodeId(operator.getNodeId());
                operatorEntity.setEngineType(operator.getEngineType());
                operatorEntity.setHandleId(user.getId());
                operatorEntity.setStatus(OperatorStateEnum.ASSIST.getCode());
                operatorEntity.setIsProcessing(processing);
                operatorEntity.setCompletion(FlowNature.NORMAL);
                if (Boolean.TRUE.equals(flowTodo)) {
                    operatorEntity.setSignTime(new Date());
                    operatorEntity.setStartHandleTime(new Date());
                } else {
                    if (Boolean.TRUE.equals(flowSign)) {
                        operatorEntity.setSignTime(new Date());
                    } else {
                        if (!signFor) {
                            operatorEntity.setSignTime(new Date());
                        }
                    }
                }
                list.add(operatorEntity);
            }
            super.saveBatch(list);

            // 记录
            FlowMethod flowMethod = new FlowMethod();
            flowMethod.setType(RecordEnum.ASSIST.getCode());
            flowMethod.setFlowModel(flowModel);
            flowMethod.setOperatorEntity(operator);
            List<String> userIds = userList.stream().map(UserEntity::getId).collect(Collectors.toList());
            flowMethod.setUserId(String.join(",", userIds));
            recordMapper.createRecord(flowMethod);

            // 消息
            FlowMsgModel flowMsgModel = new FlowMsgModel();
            flowMsgModel.setOperatorList(list);
            flowMsgModel.setNodeList(flowModel.getNodeEntityList());
            flowMsgModel.setUserInfo(flowModel.getUserInfo());
            flowMsgModel.setTaskEntity(taskEntity);
            flowMsgModel.setNodeCode(operator.getNodeCode());
            msgUtil.message(flowMsgModel);
        }
    }

    // 协办保存，仅保存表单数据
    @DSTransactional
    @Override
    public void assistSave(String id, FlowModel flowModel) throws WorkFlowException {
        OperatorEntity operator = operatorUtil.checkOperator(id);
        flowUtil.setFlowModel(operator.getTaskId(), flowModel);
        TaskEntity taskEntity = flowModel.getTaskEntity();
        operatorUtil.checkTemplateHide(taskEntity.getTemplateId());
        flowUtil.isSuspend(taskEntity);
        flowUtil.isCancel(taskEntity);
        Map<String, Object> formData = flowModel.getFormData();
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        TemplateNodeEntity nodeEntity = nodeEntityList.stream().filter(e -> e.getNodeCode().equals(operator.getNodeCode())).findFirst().orElse(null);
        if (null == nodeEntity) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }
        NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);

        FlowContextHolder.addChildData(operator.getTaskId(), nodeEntity.getFormId(), formData, nodeModel.getFormOperates(), true);
        serviceUtil.handleFormData(taskEntity.getFlowId(), false);
    }

    @Override
    public List<FlowBatchModel> batchFlowSelector() {
        List<OperatorEntity> batchList = this.getBatchList();
        List<String> taskIdList = batchList.stream().map(OperatorEntity::getTaskId).collect(Collectors.toList());

        List<TaskEntity> taskList = taskMapper.getOrderStaList(taskIdList);

        Map<String, List<TaskEntity>> flowList = taskList.stream()
                .collect(Collectors.groupingBy(TaskEntity::getTemplateId));

        List<FlowBatchModel> batchFlowList = new ArrayList<>();
        for (Map.Entry<String, List<TaskEntity>> entry : flowList.entrySet()) {
            String key = entry.getKey();
            List<TaskEntity> flowTaskList = flowList.get(key);
            if (CollUtil.isNotEmpty(flowTaskList)) {
                List<String> taskIds = flowTaskList.stream().map(TaskEntity::getId).collect(Collectors.toList());
                List<String> flowIds = flowTaskList.stream().map(TaskEntity::getFlowId).collect(Collectors.toList());
                List<Integer> flowType = flowTaskList.stream().map(TaskEntity::getFlowType).collect(Collectors.toList());
                if (flowType.contains(FlowNature.FREE)) {
                    continue;
                }
                List<TemplateJsonEntity> jsonList = templateJsonMapper.selectByIds(flowIds);

                String fullName = flowTaskList.stream().map(TaskEntity::getFlowName).distinct().collect(Collectors.joining(","));
                String flowId = jsonList.stream().map(TemplateJsonEntity::getTemplateId).distinct().collect(Collectors.joining(","));
                long count = batchList.stream().filter(e -> taskIds.contains(e.getTaskId())).count();

                FlowBatchModel model = new FlowBatchModel();
                model.setNum(count);
                model.setId(flowId);
                model.setFullName(fullName + "(" + count + ")");
                batchFlowList.add(model);
            }
        }
        batchFlowList = batchFlowList.stream().sorted(Comparator.comparing(FlowBatchModel::getNum).reversed()).collect(Collectors.toList());
        return batchFlowList;
    }

    public List<OperatorEntity> getBatchList() {
        String userId = UserProvider.getLoginUserId();
        List<String> userList = flowUtil.getByToUserId(userId).stream()
                .map(DelegateEntity::getCreatorUserId).collect(Collectors.toList());
        userList.add(userId);
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(OperatorEntity::getHandleId, userList)
                .eq(OperatorEntity::getCompletion, FlowNature.NORMAL)
                .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode())
                .ne(OperatorEntity::getStatus, OperatorStateEnum.ASSIST.getCode())
                .isNotNull(OperatorEntity::getSignTime).isNotNull(OperatorEntity::getStartHandleTime)
                .isNull(OperatorEntity::getHandleStatus)
                .select(OperatorEntity::getTaskId);
        return this.list(queryWrapper);
    }

    @Override
    public List<FlowBatchModel> batchVersionSelector(String templateId) {
        List<String> taskIdList = this.getBatchList().stream().map(OperatorEntity::getTaskId).collect(Collectors.toList());
        List<TaskEntity> taskList = taskMapper.getOrderStaList(taskIdList);
        List<String> flowIdList = taskList.stream().filter(e -> e.getTemplateId().equals(templateId))
                .map(TaskEntity::getFlowId).collect(Collectors.toList());
        List<FlowBatchModel> batchFlowList = new ArrayList<>();
        if (CollUtil.isEmpty(flowIdList)) {
            return batchFlowList;
        }

        List<TemplateJsonEntity> jsonList = templateJsonMapper.selectByIds(flowIdList);
        List<TemplateEntity> templateList = templateMapper.selectByIds(jsonList.stream().map(TemplateJsonEntity::getTemplateId).distinct().collect(Collectors.toList()));

        for (TemplateJsonEntity jsonEntity : jsonList) {
            FlowBatchModel model = JsonUtil.getJsonToBean(jsonEntity, FlowBatchModel.class);
            TemplateEntity template = templateList.stream().filter(e -> e.getId().equals(jsonEntity.getTemplateId())).findFirst().orElse(null);
            if (null != template) {
                model.setFullName(template.getFullName() + "(v" + jsonEntity.getVersion() + ")");
                batchFlowList.add(model);
            }
        }
        return batchFlowList;
    }

    @Override
    public List<FlowBatchModel> batchNodeSelector(String flowId) {
        List<TemplateNodeEntity> nodeList = templateNodeMapper.getList(flowId);
        List<FlowBatchModel> list = new ArrayList<>();
        for (TemplateNodeEntity nodeEntity : nodeList) {
            if (NodeEnum.APPROVER.getType().equals(nodeEntity.getNodeType())) {
                NodeModel nodeModel = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
                FlowBatchModel model = new FlowBatchModel();
                model.setId(nodeModel.getNodeId());
                model.setFullName(nodeModel.getNodeName());
                list.add(model);
            }
        }
        return list;
    }

    @Override
    public Map<String, Object> batchNode(FlowModel flowModel) throws WorkFlowException {
        String flowId = flowModel.getFlowId();
        String nodeCode = flowModel.getNodeCode();

        TemplateJsonInfoVO jsonInfoVO = flowUtil.getInfoVo(flowId);
        Map<String, Map<String, Object>> flowNodes = jsonInfoVO.getFlowNodes();

        return flowNodes.get(nodeCode);
    }

    // 批量获取候选人，不允许条件 1.线上存在条件  2.下一级节点存在候选人（20240711 第二项去除）
    @Override
    public CandidateCheckVo batchCandidates(String flowId, String operatorId, Integer batchType) throws WorkFlowException {
        TemplateJsonEntity jsonEntity = templateJsonMapper.getInfo(flowId);
        OperatorEntity operator = this.getById(operatorId);
        if (null == operator) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }

        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(flowId);
        Map<String, NodeModel> nodes = new HashMap<>();
        for (TemplateNodeEntity nodeEntity : nodeEntityList) {
            nodes.put(nodeEntity.getNodeCode(), JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class));
        }

        NodeModel nodeModel = nodes.get(operator.getNodeCode());
        if (flowUtil.checkBranch(nodeModel)) {
            throw new WorkFlowException("下一节点为选择分支无法批量审批");
        }

        NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
        if (ObjectUtil.equals(batchType, 1) && Boolean.TRUE.equals(!global.getHasContinueAfterReject())) {
            return new CandidateCheckVo();
        }
        // 获取节点的出线
        OutgoingFlowsFo flowsFo = new OutgoingFlowsFo();
        flowsFo.setDeploymentId(jsonEntity.getFlowableId());
        flowsFo.setTaskKey(operator.getNodeCode());
        List<String> outgoingFlows = flowAbleUrl.getOutgoingFlows(flowsFo);
        // 线上存在条件
        for (String outgoingFlow : outgoingFlows) {
            NodeModel flowNode = nodes.get(outgoingFlow);
            if (flowNode != null && CollUtil.isNotEmpty(flowNode.getConditions())) {
                throw new WorkFlowException(MsgCode.WF119.get());
            }
        }

        CandidateCheckFo fo = new CandidateCheckFo();
        fo.setFlowId(flowId);
        fo.setHandleStatus(ObjectUtil.equals(batchType, 1) ? FlowNature.REJECT_COMPLETION : FlowNature.AUDIT_COMPLETION);
        return operatorUtil.checkCandidates(operatorId, fo);
    }

    @DSTransactional
    @Override
    public void batch(FlowModel flowModel) throws WorkFlowException {
        Integer batchType = flowModel.getBatchType();
        List<String> ids = new ArrayList<>();
        List<String> idList = new ArrayList<>();
        operatorUtil.checkBatch(flowModel.getIds(), idList, false);
        operatorUtil.checkBatchRevoke(idList, ids, batchType);
        int count = 0;
        List<TaskEntity> taskList = new ArrayList<>();
        for (String id : ids) {
            operatorUtil.checkOperatorPermission(id);
            OperatorEntity operator = operatorUtil.checkOperator(id);
            TaskEntity taskEntity = taskMapper.getInfo(operator.getTaskId());
            taskList.add(taskEntity);
            FlowModel model = JsonUtil.getJsonToBean(flowModel, FlowModel.class);
            model.setId(id);
            switch (batchType) {
                case 0:
                case 1:
                    model.setHandleStatus(Objects.equals(1, batchType) ? FlowNature.REJECT_COMPLETION : FlowNature.AUDIT_COMPLETION);
                    try {
                        this.audit(id, model);
                    } catch (Exception e) {
                        if (e instanceof WorkFlowException) {
                            // 当选择多条数据且下个审批节点出现异常时，跳过异常节点能通过的即审批通过，异常的不处理
                            WorkFlowException workFlowException = (WorkFlowException) e;
                            if (ObjectUtil.equals(workFlowException.getCode(), 200)) {
                                count++;
                                break;
                            }
                        }
                        operatorUtil.compensate(taskEntity);
                        throw e;
                    }
                    break;
                case 2:
                    this.transfer(id, model);
                    break;
                case 3:
                    try {
                        this.back(id, model);
                        break;
                    } catch (Exception e) {
                        operatorUtil.compensate(taskEntity);
                        throw e;
                    }
                default:
                    break;
            }
        }
        flowModel.setTaskList(taskList);
        // 当选择的数据都是异常时，提示：“下一节点审批异常，无法批量审批”
        if (ids.size() == count) {
            throw new WorkFlowException(MsgCode.WF120.get());
        }
    }

    @Override
    public FlowWorkListVO flowWork(String fromId) {
        FlowWorkListVO vo = new FlowWorkListVO();
        List<FlowWorkModel> waitList = new ArrayList<>();
        // 经办
        QueryWrapper<OperatorEntity> operatorWrapper = new QueryWrapper<>();
        operatorWrapper.lambda().eq(OperatorEntity::getHandleId, fromId).eq(OperatorEntity::getCompletion, FlowNature.NORMAL)
                .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode());
        List<OperatorEntity> operatorList = this.list(operatorWrapper);

        if (CollUtil.isNotEmpty(operatorList)) {
            List<String> taskIds = operatorList.stream().map(OperatorEntity::getTaskId).collect(Collectors.toList());
            List<TaskEntity> taskList = taskMapper.getOrderStaList(taskIds);

            List<TemplateEntity> templateList = templateMapper.getListOfHidden(taskList.stream().map(TaskEntity::getTemplateId).collect(Collectors.toList()));

            for (OperatorEntity operator : operatorList) {
                FlowWorkModel workModel = JsonUtil.getJsonToBean(operator, FlowWorkModel.class);
                TaskEntity taskEntity = taskList.stream().filter(e -> e.getId().equals(operator.getTaskId())).findFirst().orElse(null);
                if (null != taskEntity) {
                    workModel.setFullName(taskEntity.getFullName());
                    TemplateEntity template = templateList.stream()
                            .filter(e -> e.getId().equals(taskEntity.getTemplateId())).findFirst().orElse(null);
                    if (null != template) {
                        workModel.setIcon(template.getIcon());
                        waitList.add(workModel);
                    }
                }
            }
        }
        vo.setFlowTask(waitList);

        // 流程
        List<FlowWorkModel> flowList = new ArrayList<>();
        List<TemplateNodeEntity> nodeList = templateNodeMapper.getListLikeUserId(fromId);
        if (CollUtil.isNotEmpty(nodeList)) {
            List<String> flowIds = nodeList.stream().map(TemplateNodeEntity::getFlowId).collect(Collectors.toList());
            List<TemplateJsonEntity> versionList = templateJsonMapper.selectByIds(flowIds);
            List<TemplateEntity> templateList = flowUtil.getListByFlowIds(flowIds);
            for (TemplateJsonEntity jsonEntity : versionList) {
                if (!ObjectUtil.equals(jsonEntity.getState(), TemplateJsonStatueEnum.START.getCode())) {
                    continue;
                }
                FlowWorkModel workModel = JsonUtil.getJsonToBean(jsonEntity, FlowWorkModel.class);
                for (TemplateNodeEntity nodeEntity : nodeList) {
                    if (nodeEntity.getNodeJson().contains(fromId)) {
                        TemplateEntity template = templateList.stream()
                                .filter(e -> e.getId().equals(jsonEntity.getTemplateId())).findFirst().orElse(null);
                        if (null != template) {
                            workModel.setIcon(template.getIcon());
                            workModel.setFullName(template.getFullName() + "(V" + jsonEntity.getVersion() + ")");
                            flowList.add(workModel);
                            break;
                        }
                    }
                }
            }
        }
        vo.setFlow(flowList);
        return vo;
    }

    @Override
    public boolean flowWork(WorkHandoverModel workHandoverModel) {
        String fromId = workHandoverModel.getFromId();
        String toId = workHandoverModel.getHandoverUser();
        List<String> waitList = workHandoverModel.getFlowTaskList();
        if (!waitList.isEmpty()) {
            // 更新经办
            QueryWrapper<OperatorEntity> operator = new QueryWrapper<>();
            operator.lambda().in(OperatorEntity::getId, waitList)
                    .eq(OperatorEntity::getHandleId, fromId);
            List<OperatorEntity> operatorList = this.list(operator);
            if (CollUtil.isNotEmpty(operatorList)) {
                for (OperatorEntity entity : operatorList) {
                    entity.setHandleId(toId);
                    String handleAll = entity.getHandleAll();
                    if (StringUtils.isNotBlank(handleAll)) {
                        String str = handleAll.replaceAll(fromId, toId);
                        entity.setHandleAll(str);
                    }
                }
                super.updateBatchById(operatorList);
            }
            // 更新候选人
            UpdateWrapper<CandidatesEntity> candidate = new UpdateWrapper<>();
            candidate.lambda().in(CandidatesEntity::getOperatorId, waitList)
                    .eq(CandidatesEntity::getHandleId, fromId)
                    .set(CandidatesEntity::getHandleId, toId);
            candidatesMapper.update(candidate);
        }
        // 流程
        List<String> flowList = workHandoverModel.getFlowList();
        if (!flowList.isEmpty()) {
            UserEntity toUser = serviceUtil.getUserInfo(toId);
            String toUserName = toUser != null ? toUser.getRealName() + "/" + toUser.getAccount() : "";
            UserEntity fromUser = serviceUtil.getUserInfo(fromId);
            String fromUserName = fromUser != null ? fromUser.getRealName() + "/" + fromUser.getAccount() : "";

            List<TemplateJsonEntity> versionList = templateJsonMapper.selectByIds(flowList);
            if (CollUtil.isNotEmpty(versionList)) {
                List<TemplateNodeEntity> updateList = new ArrayList<>();
                for (TemplateJsonEntity jsonEntity : versionList) {
                    List<TemplateNodeEntity> nodeList = templateNodeMapper.getList(jsonEntity.getId());
                    for (TemplateNodeEntity nodeEntity : nodeList) {
                        if (nodeEntity.getNodeJson().contains(fromId)) {
                            String str = nodeEntity.getNodeJson().replaceAll(fromId, toId).replaceAll(fromUserName, toUserName);
                            nodeEntity.setNodeJson(str);
                            updateList.add(nodeEntity);
                        }
                    }
                }
                if (CollUtil.isNotEmpty(updateList)) {
                    templateNodeMapper.updateById(updateList);
                }
            }
        }
        return true;
    }

}

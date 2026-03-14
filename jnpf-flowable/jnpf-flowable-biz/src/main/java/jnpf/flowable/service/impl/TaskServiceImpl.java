package jnpf.flowable.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jnpf.base.UserInfo;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.entity.SystemEntity;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.module.ModuleModel;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.*;
import jnpf.flowable.mapper.*;
import jnpf.flowable.model.candidates.*;
import jnpf.flowable.model.flowable.FlowAbleUrl;
import jnpf.flowable.model.message.FlowMsgModel;
import jnpf.flowable.model.monitor.MonitorVo;
import jnpf.flowable.model.operator.OperatorVo;
import jnpf.flowable.model.record.NodeRecordModel;
import jnpf.flowable.model.task.*;
import jnpf.flowable.model.template.BeforeInfoVo;
import jnpf.flowable.model.templatejson.TemplateJsonInfoVO;
import jnpf.flowable.model.templatenode.ButtonModel;
import jnpf.flowable.model.templatenode.TaskNodeModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.util.FlowNature;
import jnpf.flowable.service.RejectDataService;
import jnpf.flowable.service.TaskService;
import jnpf.flowable.util.*;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.util.*;
import jnpf.util.context.RequestContext;
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
 * @since 2024/4/17 15:09
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends SuperServiceImpl<TaskMapper, TaskEntity> implements TaskService {


    private final FlowAbleUrl flowAbleUrl;

    private final ServiceUtil serviceUtil;

    private final OperatorUtil operatorUtil;

    private final NodeUtil nodeUtil;

    private final MsgUtil msgUtil;

    private final ButtonUtil buttonUtil;

    private final RecordUtil recordUtil;

    private final FlowUtil flowUtil;

    private final TaskUtil taskUtil;


    private final TemplateMapper templateMapper;

    private final TemplateJsonMapper templateJsonMapper;

    private final TemplateNodeMapper templateNodeMapper;

    private final LaunchUserMapper launchUserMapper;

    private final CandidatesMapper candidatesMapper;

    private final OperatorMapper operatorMapper;

    private final RecordMapper recordMapper;

    private final CirculateMapper circulateMapper;

    private final RejectDataService rejectDataService;

    private final NodeRecordMapper nodeRecordMapper;

    private final TaskLineMapper taskLineMapper;

    private final RevokeMapper revokeMapper;

    private final EventLogMapper eventLogMapper;

    private final TemplateUseNumMapper templateUseNumMapper;

    private final TriggerLaunchflowMapper triggerLaunchflowMapper;

    @Override
    public TaskEntity getInfoSubmit(String id, SFunction<TaskEntity, ?>... columns) {
        return this.baseMapper.getInfoSubmit(id, columns);
    }

    @Override
    public List<TaskEntity> getInfosSubmit(String[] ids, SFunction<TaskEntity, ?>... columns) {
        return this.baseMapper.getInfosSubmit(ids, columns);
    }

    @Override
    public List<TaskVo> getList(TaskPagination pagination) {
        String userId = UserProvider.getUser().getUserId();
        List<Integer> templateStatusList = ImmutableList.of(TemplateStatueEnum.UP.getCode(), TemplateStatueEnum.DOWN_CONTINUE.getCode());
        List<Integer> taskStatusList = ImmutableList.of(TaskStatusEnum.TO_BE_SUBMIT.getCode(),
                TaskStatusEnum.RUNNING.getCode(), TaskStatusEnum.PAUSED.getCode(),
                TaskStatusEnum.BACKED.getCode(), TaskStatusEnum.RECALL.getCode());
        MPJLambdaWrapper<TaskEntity> queryWrapper = JoinWrappers.lambda(TaskEntity.class)
                .select(TaskEntity::getId, TaskEntity::getFullName, TaskEntity::getStartTime,
                        TaskEntity::getCurrentNodeName, TaskEntity::getFlowName, TaskEntity::getFlowType,
                        TaskEntity::getFlowVersion, TaskEntity::getParentId, TaskEntity::getStatus,
                        TaskEntity::getFlowCategory, TaskEntity::getFlowCode, TaskEntity::getFlowId,
                        TaskEntity::getInstanceId, TaskEntity::getTemplateId
                )
                .selectAs(TaskEntity::getDelegateUserId, TaskVo::getDelegateUser)
                .selectAs(TaskEntity::getUrgent, TaskVo::getFlowUrgent)
                .selectAs(TemplateEntity::getSystemId, TaskVo::getSystemName)
                .selectAs(TaskEntity::getCreatorUserId, TaskVo::getCreatorUser)
                .leftJoin(TemplateEntity.class, TemplateEntity::getId, TaskEntity::getTemplateId)
                .and(e -> e.in(TemplateEntity::getStatus, templateStatusList)
                        .or(t -> t.eq(TemplateEntity::getStatus, TemplateStatueEnum.DOWN_HIDDEN.getCode()).notIn(TaskEntity::getStatus, taskStatusList))
                )
                .ne(TaskEntity::getStatus, TaskStatusEnum.WAITING.getCode());

        List<DelegateEntity> delegateList = flowUtil.getByToUserId(userId, 0);
        queryWrapper.and(t -> {
            t.eq(TaskEntity::getCreatorUserId, userId);
            for (DelegateEntity delegate : delegateList) {
                if (StringUtil.isNotEmpty(delegate.getFlowId())) {
                    String[] flowIds = delegate.getFlowId().split(",");
                    t.or(tw -> tw.in(TaskEntity::getTemplateId, flowIds)
                            .eq(TaskEntity::getCreatorUserId, delegate.getUserId()).eq(TaskEntity::getDelegateUserId, userId)
                            .between(TaskEntity::getStartTime, delegate.getStartTime(), delegate.getEndTime())
                    );
                } else {
                    t.or().eq(TaskEntity::getCreatorUserId, delegate.getUserId()).eq(TaskEntity::getDelegateUserId, userId)
                            .between(TaskEntity::getStartTime, delegate.getStartTime(), delegate.getEndTime());
                }
            }
        });

        //关键字（流程名称、流程编码）
        String keyWord = pagination.getKeyword();
        if (ObjectUtil.isNotEmpty(keyWord)) {
            queryWrapper.and(t -> t.like(TaskEntity::getEnCode, keyWord).or().like(TaskEntity::getFullName, keyWord));
        }
        //日期范围（近7天、近1月、近3月、自定义）
        if (ObjectUtil.isNotEmpty(pagination.getStartTime()) && ObjectUtil.isNotEmpty(pagination.getEndTime())) {
            queryWrapper.between(TaskEntity::getStartTime, new Date(pagination.getStartTime()), new Date(pagination.getEndTime()));
        }
        //所属流程
        String templateId = pagination.getTemplateId();
        if (ObjectUtil.isNotEmpty(templateId)) {
            queryWrapper.eq(TaskEntity::getTemplateId, templateId);
        }
        //流程状态
        Integer status = pagination.getStatus();
        if (ObjectUtil.isNotEmpty(status)) {
            if (status.equals(0)) {// 待提交
                List<Integer> list = ImmutableList.of(TaskStatusEnum.TO_BE_SUBMIT.getCode(), TaskStatusEnum.BACKED.getCode()
                        , TaskStatusEnum.RECALL.getCode());
                queryWrapper.in(TaskEntity::getStatus, list);
            } else if (status.equals(1)) { // 进行中
                List<Integer> list = ImmutableList.of(TaskStatusEnum.RUNNING.getCode(), TaskStatusEnum.PAUSED.getCode());
                queryWrapper.in(TaskEntity::getStatus, list);
            } else { // 已完成
                List<Integer> list = ImmutableList.of(TaskStatusEnum.PASSED.getCode(), TaskStatusEnum.REJECTED.getCode(),
                        TaskStatusEnum.CANCEL.getCode(), TaskStatusEnum.REVOKED.getCode());
                queryWrapper.in(TaskEntity::getStatus, list);
            }
        }
        //紧急程度
        Integer flowUrgent = pagination.getFlowUrgent();
        if (ObjectUtil.isNotEmpty(flowUrgent)) {
            queryWrapper.eq(TaskEntity::getUrgent, flowUrgent);
        }
        //所属分类
        String flowCategory = pagination.getFlowCategory();
        if (ObjectUtil.isNotEmpty(flowCategory)) {
            queryWrapper.eq(TaskEntity::getFlowCategory, flowCategory);
        }
        //应用主键
        String systemId = pagination.getSystemId();
        if (ObjectUtil.isNotEmpty(systemId)) {
            queryWrapper.eq(TemplateEntity::getSystemId, systemId);
        }
        //排序
        queryWrapper.orderByAsc(TaskEntity::getStatus).orderByDesc(TaskEntity::getStartTime);
        Page<TaskVo> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<TaskVo> taskEntityPage = this.selectJoinListPage(page, TaskVo.class, queryWrapper);

        return pagination.setData(taskEntityPage.getRecords(), page.getTotal());
    }

    @Override
    public List<MonitorVo> getMonitorList(TaskPagination pagination) {
        return this.baseMapper.getMonitorList(pagination);
    }

    @Override
    public TaskEntity getInfo(String id) throws WorkFlowException {
        return this.baseMapper.getInfo(id);
    }

    // 发起、审批详情
    @Override
    public BeforeInfoVo getInfo(String id, FlowModel fo) throws WorkFlowException {
        BeforeInfoVo vo = new BeforeInfoVo();
        TemplateJsonInfoVO jsonInfoVO = null;
        TemplateEntity template = null;

        if (!StringUtils.equals(FlowNature.PARENT_ID, id)) {
            TaskEntity taskEntity = this.getById(id);
            if (null != taskEntity) {
                jsonInfoVO = flowUtil.getInfoVo(taskEntity.getFlowId());
                template = templateMapper.getInfo(taskEntity.getTemplateId());
            }
            vo.setLineKeyList(taskLineMapper.getLineKeyList(id));
        }
        if (null == jsonInfoVO) {
            template = null == template ? templateMapper.getInfo(fo.getFlowId()) : template;
            jsonInfoVO = flowUtil.getInfoVo(template.getFlowId());
        }
        fo.setTemplateEntity(template);
        vo.setFlowInfo(jsonInfoVO);
        fo.setDeploymentId(jsonInfoVO.getFlowableId());

        // 节点
        List<TemplateNodeEntity> nodeEntities = templateNodeMapper.getList(jsonInfoVO.getFlowId());

        TemplateNodeEntity nodeEntity = null;
        OperatorEntity operatorEntity = new OperatorEntity();
        RecordEntity recordEntity = new RecordEntity();
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> draft = new HashMap<>();
        String operatorId = ObjectUtil.isNotEmpty(fo.getOperatorId()) ? fo.getOperatorId() : "";
        OpTypeEnum type = OpTypeEnum.getType(fo.getOpType());
        switch (type) {
            case LAUNCH_CREATE:
            case SIGN:// 待签
            case TODO:// 待办
            case DOING:// 在办
                OperatorEntity operator = operatorMapper.getInfo(operatorId);
                if (null != operator) {
                    nodeEntity = nodeEntities.stream()
                            .filter(e -> StringUtils.equals(operator.getNodeCode(), e.getNodeCode())).findFirst().orElse(null);
                    operatorEntity = operator;
                }
                break;
            case DONE:// 已办
                RecordEntity recordMapperInfo = recordMapper.getInfo(operatorId);
                if (null != recordMapperInfo) {
                    nodeEntity = nodeEntities.stream()
                            .filter(e -> StringUtils.equals(recordMapperInfo.getNodeCode(), e.getNodeCode()) && !Objects.equals(NodeEnum.OUTSIDE.getType(), e.getNodeType())).findFirst().orElse(null);
                    if (StringUtils.isNotBlank(recordMapperInfo.getOperatorId())) {
                        OperatorEntity op = operatorMapper.selectById(recordMapperInfo.getOperatorId());
                        if (null != op) {
                            operatorEntity = op;
                        }
                    }
                    recordEntity = recordMapperInfo;
                }
                break;
            case CIRCULATE:// 抄送
                CirculateEntity circulateEntity = circulateMapper.selectById(operatorId);
                if (null != circulateEntity) {
                    nodeEntity = nodeEntities.stream()
                            .filter(e -> StringUtils.equals(circulateEntity.getNodeCode(), e.getNodeCode())).findFirst().orElse(null);
                    circulateEntity.setCirculateRead(1);
                    circulateMapper.updateById(circulateEntity);
                    if (StringUtils.isNotBlank(circulateEntity.getOperatorId())) {
                        OperatorEntity ope = operatorMapper.selectById(circulateEntity.getOperatorId());
                        if (null != ope) {
                            operatorEntity = ope;
                        }
                    }
                }
                break;
            default:
                break;
        }
        //发起流程添加使用次数
        if (Objects.equals(fo.getIsFlow(), 1)) {
            templateUseNumMapper.insertOrUpdateUseNum(fo.getFlowId());
        }

        if (null == nodeEntity) {
            // 默认获取开始节点
            nodeEntity = nodeEntities.stream()
                    .filter(e -> StringUtils.equals(NodeEnum.START.getType(), e.getNodeType())).findFirst().orElse(null);
        }
        if (null == nodeEntity) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }
        if (OpTypeEnum.DOING.equals(type) && null != operatorEntity.getDraftData()) {
            Map<String, Object> draftData = JsonUtil.stringToMap(operatorEntity.getDraftData());
            draftData.forEach((k, v) -> {
                if (ObjectUtil.isNotEmpty(v) && !Objects.equals(TableFeildsEnum.VERSION.getField(), k)) {
                    map.put(k, v);
                }
            });
            draft.putAll(draftData);
        }

        String formId = nodeEntity.getFormId();
        NodeModel currentNode = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);
        vo.setFormOperates(currentNode.getFormOperates());
        // 表单数据
        if (!StringUtils.equals(FlowNature.PARENT_ID, id)) {
            Map<String, Object> formData = serviceUtil.infoData(formId, id);
            vo.setFormData(formData);
            fo.setFormData(formData);
            formData.putAll(map);
            draft.putAll(formData);

            String flowTaskID = Objects.nonNull(formData.get(FlowFormConstant.FLOWTASKID)) ? formData.get(FlowFormConstant.FLOWTASKID).toString() : "";
            id = StringUtils.isNotBlank(flowTaskID) ? flowTaskID : id;
            TaskEntity taskEntity = this.getById(id);
            if (null != taskEntity) {
                TaskVo taskVo = JsonUtil.getJsonToBean(taskEntity, TaskVo.class);
                taskVo.setFlowUrgent(taskEntity.getUrgent());
                UserEntity user = serviceUtil.getUserInfo(taskEntity.getCreatorUserId());
                taskVo.setCreatorUser(user.getRealName());
                taskVo.setHeadIcon(UploaderUtil.uploaderImg(user.getHeadIcon()));
                String flowCategory = taskEntity.getFlowCategory();
                // 分类名称
                List<String> categoryIds = ImmutableList.of(flowCategory);
                List<DictionaryDataEntity> dictionName = serviceUtil.getDictionName(categoryIds);
                taskVo.setFlowCategory(dictionName.stream().map(DictionaryDataEntity::getFullName).collect(Collectors.joining(",")));

                vo.setTaskInfo(taskVo);
                fo.setTaskEntity(taskEntity);

                // 流转记录
                List<RecordEntity> records = recordMapper.getList(taskEntity.getId());
                vo.setRecordList(recordUtil.getRecordList(records, nodeEntities));

                FlowModel flowModel = new FlowModel();
                flowModel.setFormData(formData);
                flowModel.setDeploymentId(fo.getDeploymentId());
                flowModel.setTaskEntity(taskEntity);
                flowModel.setNodeEntityList(nodeEntities);
                flowModel.setNodeEntity(nodeEntity);
                flowModel.setOpType(fo.getOpType());
                flowModel.setRecordEntity(recordEntity);
                // 节点
                List<TaskNodeModel> nodeList = nodeUtil.getNodeList(flowModel);
                vo.setNodeList(nodeList);
                fo.setNodeList(nodeList);

                vo.setFlowInfo(jsonInfoVO);
                Map<String, Object> nodeProperties = jsonInfoVO.getFlowNodes().get(nodeEntity.getNodeCode());
                nodeUtil.setFlowFile(currentNode, taskEntity, nodeProperties);
                vo.setNodeProperties(nodeProperties);

                vo.setProgressList(!Objects.equals(type, OpTypeEnum.MONITOR) ? recordUtil.getProgressList(flowModel) : new ArrayList<>());
            }
        }
        // 判断按钮
        fo.setOperatorEntity(operatorEntity);
        fo.setNodeEntity(nodeEntity);
        fo.setRecordEntity(recordEntity);
        fo.setNodeEntityList(nodeEntities);
        fo.setFlowId(jsonInfoVO.getFlowId());
        ButtonModel model = buttonUtil.handleButton(fo);
        vo.setBtnInfo(model);
        RevokeEntity revokeEntity = revokeMapper.getRevokeTask(id);
        if (null != revokeEntity) {
            Map<String, Object> revokeMap = JsonUtil.stringToMap(revokeEntity.getFormData());
            vo.setFormData(revokeMap);
            VisualdevEntity formInfo = new VisualdevEntity();
            formInfo.setEnCode(FlowNature.REVOKE_FORM_CODE);
            formInfo.setType(2);
            vo.setFormInfo(formInfo);
            TaskVo taskInfo = vo.getTaskInfo();
            if (null != taskInfo) {
                taskInfo.setIsRevokeTask(true);
            }
        } else {
            vo.setFormData(draft);
            // 获取表单
            if (null != formId) {
                VisualdevEntity formInfo = serviceUtil.getFormInfo(formId);
                vo.setFormInfo(formInfo);
            }
        }
        return vo;
    }

    @Override
    public CandidateCheckVo checkCandidates(String id, CandidateCheckFo fo) throws WorkFlowException {
        return operatorUtil.checkCandidates(id, fo);
    }

    @Override
    public List<CandidateUserVo> getCandidateUser(String id, CandidateCheckFo fo) throws WorkFlowException {
        TemplateJsonEntity jsonEntity = templateJsonMapper.selectById(fo.getFlowId());
        if (null == jsonEntity) {
            TemplateEntity template = templateMapper.getInfo(fo.getFlowId());
            jsonEntity = templateJsonMapper.getInfo(template.getFlowId());
        }
        List<TemplateNodeEntity> nodeEntities = templateNodeMapper.getList(jsonEntity.getId());

        Map<String, NodeModel> nodes = new HashMap<>();
        for (TemplateNodeEntity nodeEntity : nodeEntities) {
            nodes.put(nodeEntity.getNodeCode(), JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class));
        }
        NodeModel nodeModel = nodes.get(fo.getNodeCode());
        if (null == nodeModel) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }

        // 获取候选人
        CandidateCheckVo candidateCheckVo = checkCandidates(id, fo);
        List<CandidateListModel> list = candidateCheckVo.getList();
        Map<String, List<CandidateListModel>> nodeMap = list.stream().collect(Collectors.groupingBy(CandidateListModel::getNodeCode));
        List<CandidateListModel> candidateList = nodeMap.get(fo.getNodeCode()) != null ? nodeMap.get(fo.getNodeCode()) : new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        for (CandidateListModel model : candidateList) {
            userIds.addAll(model.getSelectIdList());
        }

        // 候选人范围过滤
        UserInfo userInfo = UserProvider.getUser();
        UserEntity userEntity = serviceUtil.getUserInfo(userInfo.getUserId());
        OperatorEntity operator = operatorMapper.selectById(id);
        if (null != operator) {
            userEntity = serviceUtil.getUserInfo(operator.getHandleId());
        } else {
            if (StringUtils.isNotBlank(fo.getDelegateUser())) {
                userEntity = serviceUtil.getUserInfo(fo.getDelegateUser());
            }
        }
        LaunchUserEntity flowUser = new LaunchUserEntity();
        flowUtil.launchUser(flowUser, userEntity);
        flowUtil.rule(userIds, flowUser, nodeModel.getExtraRule());

        return operatorUtil.getUserModel(userIds, fo);
    }

    @DSTransactional
    @Override
    public void batchSaveOrSubmit(FlowModel flowModel) throws WorkFlowException {
        operatorUtil.batchSaveOrSubmit(flowModel);
    }

    // 提交或暂存
    @Override
    public void saveOrSubmit(FlowModel flowModel) throws WorkFlowException {
        operatorUtil.saveOrSubmit(flowModel);
    }

    // 发起撤回
    @DSTransactional
    @Override
    public void recall(String id, FlowModel flowModel) throws WorkFlowException {
        flowUtil.setFlowModel(id, flowModel);
        TaskEntity taskEntity = flowModel.getTaskEntity();
        operatorUtil.checkTemplateHide(taskEntity.getTemplateId());
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();
        if (CollUtil.isEmpty(nodeEntityList)) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }
        operatorUtil.addTask(ImmutableList.of(taskEntity.getId()));
        // 开始节点
        TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                .filter(e -> StringUtils.equals(NodeEnum.START.getType(), e.getNodeType())).findFirst().orElse(null);
        if (null == nodeEntity) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }
        NodeModel start = JsonUtil.getJsonToBean(nodeEntity.getNodeJson(), NodeModel.class);

        flowModel.setNodeEntity(nodeEntity);
        flowModel.setIsException(true);
        if (!buttonUtil.checkRecall(flowModel)) {
            throw new WorkFlowException(MsgCode.WF077.get());
        }
        // 删除子流程(判断方法中，存在异步 或 同步子流程已提交 则不允许撤回)
        operatorUtil.deleteSubflow(taskEntity.getId(), null);
        // 删除外部节点
        eventLogMapper.delete(taskEntity.getId(), null);
        // 记录
        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setType(RecordEnum.RECALL.getCode());
        flowMethod.setFlowModel(flowModel);
        OperatorEntity operatorEntity = new OperatorEntity();
        operatorEntity.setNodeCode(nodeEntity.getNodeCode());
        operatorEntity.setNodeName(start.getNodeName());
        operatorEntity.setTaskId(taskEntity.getId());
        operatorEntity.setHandleId(taskEntity.getCreatorUserId());
        operatorEntity.setHandleTime(new Date());
        flowMethod.setOperatorEntity(operatorEntity);
        recordMapper.createRecord(flowMethod);

        // 节点记录
        NodeRecordModel nodeRecordModel = new NodeRecordModel();
        nodeRecordModel.setTaskId(taskEntity.getId());
        nodeRecordModel.setNodeCode(nodeEntity.getNodeCode());
        nodeRecordModel.setNodeStatus(NodeStateEnum.RECALL.getCode());
        nodeRecordMapper.update(nodeRecordModel);

        // 删除经办
        QueryWrapper<OperatorEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(OperatorEntity::getTaskId, taskEntity.getId());
        operatorMapper.delete(wrapper);

        // 删除候选人
        candidatesMapper.deleteByCodes(taskEntity.getId(), null);

        // 删除发起人
        launchUserMapper.delete(taskEntity.getId());

        //删除同步流程
        List<TriggerLaunchflowEntity> triggerLaunchList = operatorUtil.getLaunchFlowList(taskEntity.getId());
        triggerLaunchflowMapper.deleteByIds(triggerLaunchList);

        // 删除引擎实例
        flowAbleUrl.deleteInstance(taskEntity.getInstanceId(), "retract");

        // 变更任务状态
        UpdateWrapper<TaskEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().eq(TaskEntity::getId, taskEntity.getId())
                .set(TaskEntity::getInstanceId, null)
                .set(TaskEntity::getRejectDataId, null)
                .set(TaskEntity::getCurrentNodeName, FlowNature.START_NAME)
                .set(TaskEntity::getCurrentNodeCode, FlowNature.START_CODE)
                .set(TaskEntity::getStatus, TaskStatusEnum.RECALL.getCode());
        this.update(updateWrapper);
        if (taskEntity.getRejectDataId() != null) {
            rejectDataService.removeById(taskEntity.getRejectDataId());
        }
    }

    // 催办
    @Override
    public boolean press(String id) throws WorkFlowException {
        FlowModel flowModel = new FlowModel();
        flowUtil.setFlowModel(id, flowModel);
        TaskEntity taskEntity = flowModel.getTaskEntity();
        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();

        // 消息
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, id).eq(OperatorEntity::getCompletion, FlowNature.NORMAL)
                .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode())
                .isNull(OperatorEntity::getHandleStatus).isNotNull(OperatorEntity::getDuedate);
        List<OperatorEntity> operatorList = operatorMapper.selectList(queryWrapper);
        if (operatorList.isEmpty()) {
            return false;
        }
        FlowMsgModel flowMsgModel = new FlowMsgModel();
        flowMsgModel.setNodeList(nodeEntityList);
        flowMsgModel.setUserInfo(flowModel.getUserInfo());
        flowMsgModel.setTaskEntity(taskEntity);
        flowMsgModel.setOperatorList(operatorList);
        flowMsgModel.setPress(true);
        msgUtil.message(flowMsgModel);
        return true;
    }

    @DSTransactional
    @Override
    public void revoke(String id, FlowModel flowModel) throws WorkFlowException {
        flowUtil.setFlowModel(id, flowModel);
        TaskEntity taskEntity = flowModel.getTaskEntity();
        if (!TaskStatusEnum.PASSED.getCode().equals(taskEntity.getStatus())) {
            throw new WorkFlowException(MsgCode.WF078.get());
        }
        // 处理撤销表单的数据
        operatorUtil.submitOfRevoke(flowModel);
    }

    @DSTransactional
    @Override
    public List<TaskEntity> delete(String id) throws WorkFlowException {
        List<TaskEntity> taskList = taskUtil.delete(ImmutableList.of(id), false);
        operatorUtil.deleteFormData(taskList);
        return taskList;
    }

    @DSTransactional
    @Override
    public void deleteBatch(List<String> ids) throws WorkFlowException {
        List<TaskEntity> taskList = taskUtil.delete(ids, true);
        operatorUtil.deleteFormData(taskList);

        List<String> instanceIds = taskList.stream()
                .filter(t -> TaskStatusEnum.RUNNING.getCode().equals(t.getStatus()) || TaskStatusEnum.CANCEL.getCode().equals(t.getStatus()))
                .map(TaskEntity::getInstanceId).distinct().collect(Collectors.toList());
        if (CollUtil.isNotEmpty(instanceIds)) {
            for (String instanceId : instanceIds) {
                flowAbleUrl.deleteInstance(instanceId, "monitor");
            }
        }
    }

    @Override
    public void deleTaskAll(List<String> idList, List<String> idAll) {
        this.baseMapper.deleTaskAll(idList, idAll);
    }

    // 终止与挂起的区别，就是能否在列表中查询到，撤销中不允许终止等操作
    @DSTransactional
    @Override
    public void cancel(String id, FlowModel flowModel, boolean isCancel) throws WorkFlowException {
        operatorUtil.cancel(id, flowModel, isCancel);
    }

    @Override
    public boolean checkAsync(String id) {
        return this.baseMapper.checkAsync(id);
    }

    // isSuspend: true 挂起、false 恢复
    @DSTransactional
    @Override
    public void pause(String id, FlowModel flowModel, Boolean isSuspend) throws WorkFlowException {
        List<String> idList = new ArrayList<>();
        idList.add(id);

        boolean pause = false;

        // 暂停的选项，0.全部  1：仅主流程，同时暂停同步子流程
        if (Boolean.TRUE.equals(isSuspend)) {
            pause = flowModel.getPause() == 1;
        } else {
            TaskEntity taskEntity = this.getInfo(id);
            if (ObjectUtil.equals(taskEntity.getIsRestore(), FlowNature.NOT_RESTORE)) {
                throw new WorkFlowException("无法恢复");
            }
            List<TaskEntity> childList = this.baseMapper.getChildList(id, TaskEntity::getId, TaskEntity::getIsRestore);
            // 存在 0（能恢复的），就只恢复 1（不能恢复的，即同步的）
            pause = childList.stream().anyMatch(e -> ObjectUtil.equals(e.getIsRestore(), FlowNature.RESTORE));
        }
        String userId = UserProvider.getLoginUserId();

        this.baseMapper.getChildList(id, pause, idList);

        List<TaskEntity> orderStaList = this.baseMapper.getOrderStaList(idList);

        this.pause(orderStaList, isSuspend, id);

        for (TaskEntity entity : orderStaList) {
            // 记录
            FlowMethod flowMethod = new FlowMethod();
            flowMethod.setFlowModel(flowModel);
            flowMethod.setType(Boolean.TRUE.equals(isSuspend) ? RecordEnum.PAUSE.getCode() : RecordEnum.REBOOT.getCode());
            OperatorEntity operatorEntity = new OperatorEntity();
            operatorEntity.setNodeCode(StringUtils.isNotBlank(entity.getCurrentNodeCode()) ? entity.getCurrentNodeCode() : FlowNature.START_CODE);
            operatorEntity.setNodeName(StringUtils.isNotBlank(entity.getCurrentNodeName()) ? entity.getCurrentNodeName() : FlowNature.START_NAME);
            operatorEntity.setTaskId(entity.getId());
            operatorEntity.setHandleId(userId);
            flowMethod.setOperatorEntity(operatorEntity);
            recordMapper.createRecord(flowMethod);
            operatorUtil.addTask(ImmutableList.of(entity.getId()));
        }

    }

    public List<OperatorEntity> pause(List<TaskEntity> orderStaList, boolean isSuspend, String id) {
        List<OperatorEntity> operatorList = new ArrayList<>();
        for (TaskEntity taskEntity : orderStaList) {
            if (isSuspend) {
                // 暂停
                if (!ObjectUtil.equals(taskEntity.getStatus(), TaskStatusEnum.PAUSED.getCode())) {
                    taskEntity.setHisStatus(taskEntity.getStatus());
                    taskEntity.setStatus(TaskStatusEnum.PAUSED.getCode());
                }
                taskEntity.setIsRestore(ObjectUtil.equals(taskEntity.getId(), id) ? FlowNature.RESTORE : FlowNature.NOT_RESTORE);
                this.updateById(taskEntity);
            } else {
                // 恢复
                taskEntity.setStatus(taskEntity.getHisStatus());
                taskEntity.setHisStatus(TaskStatusEnum.PAUSED.getCode());
                taskEntity.setIsRestore(FlowNature.RESTORE);
                this.updateById(taskEntity);
            }
            // 过滤作废的经办
            List<OperatorEntity> list = operatorMapper.getList(taskEntity.getId()).stream()
                    .filter(e -> !e.getStatus().equals(OperatorStateEnum.FUTILITY.getCode())).collect(Collectors.toList());
            operatorList.addAll(list);
        }
        return operatorList;
    }

    @DSTransactional
    @Override
    public void assign(String id, FlowModel flowModel) throws WorkFlowException {
        flowUtil.setFlowModel(id, flowModel);
        TaskEntity entity = flowModel.getTaskEntity();
        flowUtil.isSuspend(entity);
        flowUtil.isCancel(entity);

        String handleId = flowModel.getUserInfo() == null ? UserProvider.getLoginUserId() : flowModel.getUserInfo().getUserId();

        List<TemplateNodeEntity> nodeEntityList = flowModel.getNodeEntityList();

        // 作废原先的经办、生成指派的经办
        OperatorEntity operator = operatorUtil.handleAssign(flowModel);
        List<OperatorEntity> list = new ArrayList<>();
        list.add(operator);

        String nodeCode = flowModel.getNodeCode();
        NodeModel nodeModel = flowModel.getNodes().get(nodeCode);
        Boolean autoTransfer = flowModel.getAutoTransferFlag();
        boolean isProcessing = ObjectUtil.equals(nodeModel.getType(), NodeEnum.PROCESSING.getType());
        if (Boolean.TRUE.equals(autoTransfer)) {
            flowModel.setHandleOpinion(isProcessing ? "系统转办" : "系统转审");
        }

        //指派逐级审批失效
        if (StringUtil.isNotEmpty(nodeCode)) {
            launchUserMapper.delete(operator.getTaskId(), ImmutableList.of(nodeCode));
        }

        // 记录
        FlowMethod flowMethod = new FlowMethod();
        flowMethod.setFlowModel(flowModel);
        Integer transferType = isProcessing ? RecordEnum.TRANSFER_PROCESSING.getCode() : RecordEnum.TRANSFER.getCode();
        flowMethod.setType(Boolean.TRUE.equals(autoTransfer) ? transferType : RecordEnum.ASSIGN.getCode());
        OperatorEntity operatorEntity = new OperatorEntity();
        operatorEntity.setId(operator.getId());
        operatorEntity.setNodeCode(nodeCode);
        operatorEntity.setNodeId(operator.getNodeId());
        operatorEntity.setNodeName(nodeModel.getNodeName());
        operatorEntity.setTaskId(entity.getId());
        operatorEntity.setHandleId(Boolean.TRUE.equals(autoTransfer) ? FlowNature.SYSTEM_CODE : handleId);
        flowMethod.setOperatorEntity(operatorEntity);
        List<String> userIds = ImmutableList.of(flowModel.getHandleIds());
        flowMethod.setUserId(String.join(",", userIds));
        recordMapper.createRecord(flowMethod);

        // 消息
        FlowMsgModel flowMsgModel = new FlowMsgModel();
        flowMsgModel.setNodeList(nodeEntityList);
        flowMsgModel.setUserInfo(flowModel.getUserInfo());
        flowMsgModel.setTaskEntity(entity);
        flowMsgModel.setOperatorList(list);
        if (Boolean.TRUE.equals(flowModel.getAutoTransferFlag())) {
            flowMsgModel.setTransfer(true);
        } else {
            flowMsgModel.setAssign(true);
        }
        msgUtil.message(flowMsgModel);

        operatorUtil.addOperatorList(list, flowModel);
    }

    @Override
    public TaskUserListModel getTaskUserList(String taskId) {
        return flowUtil.getTaskUserList(taskId);
    }

    @Override
    public List<BeforeInfoVo> subFlowInfo(FlowModel flowModel) throws WorkFlowException {
        List<BeforeInfoVo> list = new ArrayList<>();
        List<TaskEntity> entityList = this.baseMapper.subFlowInfo(flowModel);
        if (CollUtil.isNotEmpty(entityList)) {
            for (TaskEntity entity : entityList) {
                FlowModel model = new FlowModel();
                model.setFlowId(entity.getFlowId());
                BeforeInfoVo info = this.getInfo(entity.getId(), model);
                list.add(info);
            }
        }
        return list;
    }

    // 消息跳转流程时，校验以及返回opType
    @Override
    public String checkInfo(String id) throws WorkFlowException {
        CirculateEntity circulate = circulateMapper.selectById(id);
        if (null != circulate) {
            return OpTypeEnum.CIRCULATE.getType();
        }
        OperatorEntity operator = operatorMapper.getInfo(id);
        if (null == operator) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        if (operator.getStatus().equals(OperatorStateEnum.FUTILITY.getCode())) {
            throw new WorkFlowException("该流程无权限查看");
        }

        TaskEntity taskEntity = this.getById(operator.getTaskId());
        if (null == taskEntity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        if (TaskStatusEnum.CANCEL.getCode().equals(taskEntity.getStatus())) {
            throw new WorkFlowException(MsgCode.WF041.get());
        }
        if (TaskStatusEnum.RECALL.getCode().equals(taskEntity.getStatus())) {
            throw new WorkFlowException(MsgCode.WF042.get());
        }
        operatorUtil.checkTemplateHide(taskEntity.getTemplateId());

        String handleId = operator.getHandleId();
        String userId = UserProvider.getLoginUserId();
        List<String> delegateList = flowUtil.getToUser(handleId, taskEntity.getFlowId());
        delegateList.add(handleId);
        if (!delegateList.contains(userId)) {
            throw new WorkFlowException(MsgCode.FA021.get());
        }

        String opType = OpTypeEnum.LAUNCH_DETAIL.getType();
        if (null == operator.getSignTime() && null == operator.getStartHandleTime() && null == operator.getHandleStatus()) {
            opType = OpTypeEnum.SIGN.getType();
        }
        if (null != operator.getSignTime() && null == operator.getStartHandleTime() && null == operator.getHandleStatus()) {
            opType = OpTypeEnum.TODO.getType();
        }
        if (null != operator.getSignTime() && null != operator.getStartHandleTime() && null == operator.getHandleStatus()) {
            opType = OpTypeEnum.DOING.getType();
        }
        if (null != operator.getHandleStatus() || operator.getCompletion().equals(FlowNature.ACTION)) {
            opType = OpTypeEnum.CIRCULATE.getType();
        }
        return opType;
    }

    @Override
    public void updateIsFile(String taskId) throws WorkFlowException {
        TaskEntity taskEntity = this.getInfo(taskId);
        if (null != taskEntity) {
            taskEntity.setIsFile(1);
            this.updateById(taskEntity);
        }
    }

    // 获取发起表单
    @Override
    public ViewFormModel getStartForm(String taskId) throws WorkFlowException {
        ViewFormModel model = new ViewFormModel();
        TaskEntity taskEntity = this.getInfo(taskId);
        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(taskEntity.getFlowId());
        TemplateNodeEntity nodeEntity = nodeEntityList.stream()
                .filter(e -> ObjectUtil.equals(e.getNodeType(), NodeEnum.START.getType())).findFirst().orElse(null);
        if (null != nodeEntity) {
            String formId = nodeEntity.getFormId();
            VisualdevEntity formInfo = serviceUtil.getFormInfo(formId);
            model.setFormInfo(formInfo);
            Map<String, Object> map = serviceUtil.infoData(formId, taskId);
            model.setFormData(map);
        }
        return model;
    }

    @Override
    public TaskTo getFlowTodoCount(TaskTo taskTo) {
        TaskPagination pagination = new TaskPagination();
        pagination.setDelegateType(true);
        pagination.setPageSize(1L);
        pagination.setCurrentPage(1L);
        String appCode = RequestContext.getAppCode();
        boolean isMainSystem = Objects.equals(JnpfConst.MAIN_SYSTEM_CODE, appCode);
        SystemEntity systemEntity = serviceUtil.getInfoByEnCode(!isMainSystem ? appCode : JnpfConst.WORK_FLOW_CODE);
        if (!isMainSystem) {
            pagination.setSystemId(systemEntity.getId());
        }
        TaskTo vo = new TaskTo();
        boolean isPc = RequestContext.isOrignPc();
        String webType = isPc ? JnpfConst.WEB : JnpfConst.APP;
        List<ModuleModel> moduleListAll = taskTo.getModuleList() != null ? taskTo.getModuleList() : new ArrayList<>();
        List<ModuleModel> moduleList = moduleListAll.stream().
                filter(e -> Objects.equals(e.getSystemId(), systemEntity.getId()) && Objects.equals(webType, e.getCategory()))
                .collect(Collectors.toList());
        Map<String, List<ModuleModel>> encodeMap = moduleList.stream().collect(Collectors.groupingBy(ModuleModel::getEnCode));

        vo.setIsLaunch(ObjectUtil.isNotEmpty(encodeMap.get(JnpfConst.WORK_FLOWLAUNCH)));
        if (Boolean.TRUE.equals(vo.getIsLaunch())) {
            getList(pagination);
            vo.setFlowLaunch(pagination.getTotal());
        }

        vo.setIsToSign(ObjectUtil.isNotEmpty(encodeMap.get(JnpfConst.WORK_FLOWSIGN)));
        if (Boolean.TRUE.equals(vo.getIsToSign())) {
            if (ObjectUtil.isNotEmpty(taskTo.getFlowToSignType())) {
                pagination.setFlowCategory(String.join(",", taskTo.getFlowToSignType()));
            }
            pagination.setCategory(CategoryEnum.SIGN.getType());
            flowUtil.getOperatorList(pagination);
            vo.setFlowToSign(pagination.getTotal());
        }

        vo.setIsTodo(ObjectUtil.isNotEmpty(encodeMap.get(JnpfConst.WORK_FLOWTODO)));
        if (Boolean.TRUE.equals(vo.getIsTodo())) {
            if (ObjectUtil.isNotEmpty(taskTo.getFlowTodoType())) {
                pagination.setFlowCategory(String.join(",", taskTo.getFlowTodoType()));
            }
            pagination.setCategory(CategoryEnum.TODO.getType());
            flowUtil.getOperatorList(pagination);
            vo.setFlowTodo(pagination.getTotal());
        }

        vo.setIsDoing(ObjectUtil.isNotEmpty(encodeMap.get(JnpfConst.WORK_FLOWDOING)));
        if (Boolean.TRUE.equals(vo.getIsDoing())) {
            if (ObjectUtil.isNotEmpty(taskTo.getFlowDoingType())) {
                pagination.setFlowCategory(String.join(",", taskTo.getFlowDoingType()));
            }
            pagination.setCategory(CategoryEnum.DOING.getType());
            flowUtil.getOperatorList(pagination);
            vo.setFlowDoing(pagination.getTotal());
        }

        vo.setIsDone(ObjectUtil.isNotEmpty(encodeMap.get(JnpfConst.WORK_FLOWDONE)));
        if (Boolean.TRUE.equals(vo.getIsDone())) {
            if (ObjectUtil.isNotEmpty(taskTo.getFlowDoneType())) {
                pagination.setFlowCategory(String.join(",", taskTo.getFlowDoneType()));
            }
            flowUtil.getRecordList(pagination);
            vo.setFlowDone(pagination.getTotal());
        }

        vo.setIsCirculate(ObjectUtil.isNotEmpty(encodeMap.get(JnpfConst.WORK_FLOWCIRCULATE)));
        if (Boolean.TRUE.equals(vo.getIsCirculate())) {
            if (ObjectUtil.isNotEmpty(taskTo.getFlowCirculateType())) {
                pagination.setFlowCategory(String.join(",", taskTo.getFlowCirculateType()));
            }
            circulateMapper.getList(pagination);
            vo.setFlowCirculate(pagination.getTotal());
        }

        return vo;
    }

    @Override
    public FlowTodoVO getFlowTodo(TaskPagination pagination) {
        FlowTodoVO flowTodoVO = new FlowTodoVO();
        String appCode = RequestContext.getAppCode();
        String category = pagination.getCategory();
        pagination.setSystemId(serviceUtil.getSystemCodeById(appCode));
        if (Objects.equals(JnpfConst.MAIN_SYSTEM_CODE, appCode)) {
            appCode = JnpfConst.WORK_FLOW_CODE;
        }
        boolean isPc = RequestContext.isOrignPc();
        String webType = isPc ? JnpfConst.WEB : JnpfConst.APP;
        SystemEntity systemEntity = serviceUtil.getInfoByEnCode(appCode);
        AuthorizeVO authorize = serviceUtil.getAuthorizeByUser();
        List<ModuleModel> moduleList = authorize.getModuleList().stream().
                filter(e -> Objects.equals(e.getSystemId(), systemEntity.getId()) && Objects.equals(webType, e.getCategory()))
                .collect(Collectors.toList());
        Map<String, List<ModuleModel>> encodeMap = moduleList.stream().collect(Collectors.groupingBy(ModuleModel::getEnCode));
        Map<String, String> flowType = ImmutableMap.of(
                CategoryEnum.SIGN.getType(), JnpfConst.WORK_FLOWSIGN,
                CategoryEnum.TODO.getType(), JnpfConst.WORK_FLOWTODO,
                CategoryEnum.DOING.getType(), JnpfConst.WORK_FLOWDOING
        );
        String module = flowType.get(category);
        flowTodoVO.setIsAuthorize(ObjectUtil.isNotEmpty(encodeMap.get(module)));
        if (Boolean.TRUE.equals(flowTodoVO.getIsAuthorize())) {
            List<OperatorVo> waitList = flowUtil.getOperatorList(pagination);
            List<TaskFlowTodoVO> list = new ArrayList<>();
            for (OperatorVo operatorVo : waitList) {
                TaskFlowTodoVO vo = JsonUtil.getJsonToBean(operatorVo, TaskFlowTodoVO.class);
                vo.setTaskNodeId(operatorVo.getNodeCode());
                vo.setTaskOperatorId(operatorVo.getId());
                vo.setType(2);
                list.add(vo);
            }
            flowTodoVO.setList(list);
        }
        return flowTodoVO;
    }
}

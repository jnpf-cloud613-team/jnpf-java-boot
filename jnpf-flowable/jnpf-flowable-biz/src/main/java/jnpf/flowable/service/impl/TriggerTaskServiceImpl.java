package jnpf.flowable.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.ImmutableList;
import jnpf.base.UserInfo;
import jnpf.base.service.SuperServiceImpl;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.*;
import jnpf.flowable.mapper.*;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.templatejson.TemplateJsonInfoVO;
import jnpf.flowable.model.templatenode.ButtonModel;
import jnpf.flowable.model.templatenode.TaskNodeModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.trigger.*;
import jnpf.flowable.model.util.FlowNature;
import jnpf.flowable.service.TriggerTaskService;
import jnpf.flowable.util.FlowUtil;
import jnpf.flowable.util.OperatorUtil;
import jnpf.flowable.util.TaskUtil;
import jnpf.util.JsonUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/10 17:13
 */
@Service
@RequiredArgsConstructor
public class TriggerTaskServiceImpl extends SuperServiceImpl<TriggerTaskMapper, TriggerTaskEntity> implements TriggerTaskService {


    
    private final FlowUtil flowUtil;
    
    private final  TaskUtil taskUtil;
    
    private final  OperatorUtil operatorUtil;

    
    private final  TriggerRecordMapper triggerRecordMapper;
    
    private final  TemplateMapper templateMapper;
    
    private final  TemplateJsonMapper templateJsonMapper;
    
    private final  TemplateNodeMapper templateNodeMapper;
    
    private final  TaskLineMapper taskLineMapper;

    @Override
    public List<TriggerInfoListModel> getListByTaskId(String taskId, String nodeCode) {
        List<TriggerTaskEntity> list = this.baseMapper.getListByTaskId(taskId,nodeCode);
        List<TriggerInfoListModel> modelList = new ArrayList<>();
        if (CollUtil.isNotEmpty(list)) {
            for (TriggerTaskEntity triggerTask : list) {
                TriggerInfoListModel model = JsonUtil.getJsonToBean(triggerTask, TriggerInfoListModel.class);
                List<TriggerRecordEntity> recordList = triggerRecordMapper.getList(triggerTask.getId());
                model.setRecordList(recordList);
                modelList.add(model);
            }
        }
        return modelList;
    }

    @Override
    public boolean existTriggerTask(String taskId, String nodeId) {
        return this.baseMapper.existTriggerTask(taskId, nodeId);
    }

    @Override
    public List<TriggerTaskModel> getList(TriggerPagination pagination) {
        return this.baseMapper.getList(pagination);
    }

    @Override
    public TriggerInfoModel getInfo(String id) throws WorkFlowException {
        TriggerTaskEntity triggerTask = this.getById(id);
        if (null == triggerTask) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        TriggerInfoModel model = new TriggerInfoModel();
        model.setTaskInfo(JsonUtil.getJsonToBean(triggerTask, TriggerTaskModel.class));
        TemplateJsonInfoVO jsonInfoVO = flowUtil.getInfoVo(triggerTask.getFlowId());
        model.setFlowInfo(jsonInfoVO);
        List<TriggerRecordEntity> recordList = triggerRecordMapper.getList(triggerTask.getId());
        List<TaskNodeModel> nodeList = this.getNodeList(triggerTask, recordList);
        model.setNodeList(nodeList);
        recordList = recordList.stream().filter(e -> e.getStatus() != null).collect(Collectors.toList());
        model.setRecordList(recordList);
        ButtonModel btnInfo = model.getBtnInfo();
        if (ObjectUtil.equals(triggerTask.getStatus(), TaskStatusEnum.RUNNING.getCode())) {
            btnInfo.setHasCancelBtn(true);
        }
        List<String> lineKeyList = taskLineMapper.getLineKeyList(id);
        model.setLineKeyList(lineKeyList);
        return model;
    }

    public List<TaskNodeModel> getNodeList(TriggerTaskEntity triggerTask, List<TriggerRecordEntity> recordList) {
        List<TaskNodeModel> list = new ArrayList<>();
        if (ObjectUtil.equals(triggerTask.getStatus(), TaskStatusEnum.CANCEL.getCode())) {
            return list;
        }
        String flowId = triggerTask.getFlowId();
        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(flowId);
        List<String> typeList = ImmutableList.of(NodeEnum.GLOBAL.getType(), NodeEnum.CONNECT.getType(), "confluence", "gateway");
        List<String> divideList = Arrays.stream(DivideRuleEnum.values()).map(DivideRuleEnum::getType).collect(Collectors.toList());
        divideList.addAll(typeList);
        for (TemplateNodeEntity node : nodeEntityList) {
            if (divideList.contains(node.getNodeType())) {
                continue;
            }
            TaskNodeModel model = new TaskNodeModel();
            NodeModel nodeModel = JsonUtil.getJsonToBean(node.getNodeJson(), NodeModel.class);
            model.setNodeName(nodeModel.getNodeName());
            model.setNodeCode(nodeModel.getNodeId());
            model.setNodeType(nodeModel.getType());

            String nodeCode = node.getNodeCode();
            if (ObjectUtil.equals(node.getNodeType(), NodeEnum.START.getType())) {
                nodeCode = FlowNature.START_CODE;
            } else if (ObjectUtil.equals(node.getNodeType(), NodeEnum.END.getType())) {
                nodeCode = FlowNature.END_CODE;
            }
            String finalNodeCode = nodeCode;
            TriggerRecordEntity triggerRecordEntity = recordList.stream().filter(e -> ObjectUtil.equals(e.getNodeCode(), finalNodeCode)).findFirst().orElse(null);
            if (null != triggerRecordEntity) {
                String type = NodeTypeEnum.CURRENT.getType();
                if (ObjectUtil.equals(triggerRecordEntity.getStatus(), TriggerRecordEnum.PASSED.getCode())) {
                    type = NodeTypeEnum.PASS.getType();
                } else if (ObjectUtil.equals(triggerRecordEntity.getStatus(), TriggerRecordEnum.EXCEPTION.getCode())) {
                    type = NodeTypeEnum.EXCEPTION.getType();
                }
                model.setType(type);
            }
            list.add(model);
        }
        return list;
    }

    @Override
    public void retry(String id) throws WorkFlowException {
        TriggerTaskEntity triggerTask = this.getById(id);
        if (null == triggerTask) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        TemplateJsonEntity jsonEntity = templateJsonMapper.getInfo(triggerTask.getFlowId());
        TemplateEntity template = templateMapper.getInfo(jsonEntity.getTemplateId());
        if (!ObjectUtil.equals(template.getStatus(), TemplateStatueEnum.UP.getCode())) {
            throw new WorkFlowException(MsgCode.WF146.get());
        }
        ExecuteModel model = new ExecuteModel();
        model.setFlowId(triggerTask.getFlowId());
        List<Map<String, Object>> dataList = JsonUtil.getJsonToListMap(triggerTask.getData());
        model.setDataList(dataList);
        FlowModel flowModel = new FlowModel();
        UserInfo userInfo = UserProvider.getUser();
        flowModel.setUserInfo(userInfo);
        model.setParentId(triggerTask.getId());
        model.setFlowModel(flowModel);
        operatorUtil.execute(model);
    }

    @Override
    public void saveTriggerTask(TriggerTaskEntity entity) {
        this.save(entity);
    }

    @Override
    public void updateTriggerTask(TriggerTaskEntity entity) {
        this.updateById(entity);
    }

    @Override
    public void batchDelete(List<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        taskUtil.delete(ids);
    }

    @Override
    public boolean checkByFlowIds(List<String> flowIds) {
        return this.baseMapper.checkByFlowIds(flowIds);
    }
}

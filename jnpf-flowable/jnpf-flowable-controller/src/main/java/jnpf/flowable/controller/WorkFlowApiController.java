package jnpf.flowable.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.google.common.collect.ImmutableList;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.NodeEnum;
import jnpf.flowable.enums.OperatorStateEnum;
import jnpf.flowable.mapper.*;
import jnpf.flowable.model.task.FileModel;
import jnpf.flowable.model.templatenode.nodejson.FileConfig;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.util.FlowNature;
import jnpf.flowable.util.TaskUtil;
import jnpf.util.DateUtil;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import jnpf.workflow.service.WorkFlowApi;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WorkFlowApiController implements WorkFlowApi {



    private final  TaskUtil taskUtil;


    private final  TaskMapper taskMapper;

    private final  TemplateNodeMapper templateNodeMapper;

    private final  TemplateJsonMapper templateJsonMapper;

    private final  TemplateMapper templateMapper;

    private final  RecordMapper recordMapper;

    private final  LaunchUserMapper launchUserMapper;

    private final  OperatorMapper operatorMapper;

    // 获取归档信息
    @Override
    public FileModel getFileModel(String taskId) throws WorkFlowException {
        TaskEntity taskEntity = taskMapper.getInfo(taskId);
        if (null == taskEntity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        FileModel model = new FileModel();

        List<TemplateNodeEntity> nodeEntityList = templateNodeMapper.getList(taskEntity.getFlowId());
        TemplateNodeEntity globalEntity = nodeEntityList.stream().filter(e -> StringUtils.equals(NodeEnum.GLOBAL.getType(), e.getNodeType())).findFirst().orElse(new TemplateNodeEntity());
        NodeModel global = JsonUtil.getJsonToBean(globalEntity.getNodeJson(), NodeModel.class);
        if (null != global) {
            FileConfig fileConfig = global.getFileConfig();
            // 归档路径
            model.setParentId(fileConfig.getParentId());

            String userId = taskEntity.getCreatorUserId();
            if (fileConfig.getPermissionType().equals(FlowNature.FLOW_ALL)) {
                // 创建人
                model.setUserId(userId);
                // 分享人
                List<String> list = taskUtil.getListOfFile(taskId);
                List<String> userList = new ArrayList<>();
                for (String id : list) {
                    if (!id.equals(userId)) {
                        userList.add(id + "--" + PermissionConst.USER);
                    }
                }
                model.setUserList(userList);
            } else if (fileConfig.getPermissionType().equals(FlowNature.FLOW_LAST)) {
                // 最后节点审批人
                List<String> list = taskUtil.getListOfLast(taskId);
                if (CollUtil.isNotEmpty(list)) {
                    model.setUserId(list.get(0));
                    list.remove(0);
                    if (CollUtil.isNotEmpty(list)) {
                        List<String> userList = new ArrayList<>();
                        for (String id : list) {
                            userList.add(id + "--" + PermissionConst.USER);
                        }
                        model.setUserList(userList);
                    }
                } else {
                    model.setUserId(userId);
                }
            } else {
                // 创建人
                model.setUserId(userId);
            }
        }

        // 文件名称
        String filename = taskEntity.getFullName();
        String datetime;
        if (null != taskEntity.getEndTime()) {
            datetime = DateUtil.dateToString(taskEntity.getEndTime(), "yyyyMMddHHmmss");
        } else {
            datetime = DateUtil.dateToString(new Date(), "yyyyMMddHHmmss");
        }
        filename += "-" + datetime;
        model.setFilename(filename + ".pdf");

        return model;
    }

    @Override
    public TaskEntity getInfoSubmit(String id, SFunction<TaskEntity, ?>... columns) {
        return taskMapper.getInfoSubmit(id, columns);
    }

    @Override
    public List<TaskEntity> getInfosSubmit(String[] ids, SFunction<TaskEntity, ?>... columns) {
        return taskMapper.getInfosSubmit(ids, columns);
    }

    @Override
    public void delete(TaskEntity taskEntity) throws WorkFlowException {
        taskUtil.delete(ImmutableList.of(taskEntity.getId()), false);
    }

    @Override
    public void updateIsFile(String taskId) {
        TaskEntity taskEntity = taskMapper.selectById(taskId);
        if (null != taskEntity) {
            taskEntity.setIsFile(1);
            taskMapper.updateById(taskEntity);
        }
    }

    @Override
    public List<RecordEntity> getRecordList(String taskId) {
        return recordMapper.getList(taskId);
    }


    @Override
    public List<String> getFlowIdsByTemplateId(String templateId) {
        List<TemplateJsonEntity> list = templateJsonMapper.getList(templateId);
        return list.stream().map(TemplateJsonEntity::getId).distinct().collect(Collectors.toList());
    }

    @Override
    public String getTemplateByVersionId(String flowId) {
        String templateId = "";
        TemplateJsonEntity byId = templateJsonMapper.selectById(flowId);
        if (byId != null) {
            templateId = byId.getTemplateId();
        }
        return templateId;
    }


    @Override
    public List<TemplateJsonEntity> getFlowIdsByTemplate(String templateId) {
        return templateJsonMapper.getList(templateId);
    }

    @Override
    public List<String> getFormList() {
        List<String> resList = new ArrayList<>();
        List<TemplateJsonEntity> list = templateJsonMapper.getListOfEnable();
        if (CollUtil.isNotEmpty(list)) {
            List<String> flowIds = list.stream().map(TemplateJsonEntity::getId).distinct().collect(Collectors.toList());
            List<TemplateNodeEntity> startNodeList = templateNodeMapper.getList(flowIds, NodeEnum.START.getType());
            resList = startNodeList.stream().map(TemplateNodeEntity::getFormId).distinct().collect(Collectors.toList());
        }
        return resList;
    }

    @Override
    public List<String> getStepList() {
        QueryWrapper<LaunchUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(LaunchUserEntity::getType, FlowNature.STEP_INITIATION);
        List<LaunchUserEntity> list = launchUserMapper.selectList(queryWrapper);
        List<String> stepList = new ArrayList<>();
        for (LaunchUserEntity entity : list) {
            String organizeId = entity.getOrganizeId();
            if (StringUtil.isNotEmpty(organizeId)) {
                stepList.addAll(Arrays.asList(organizeId.split(",")));
            }
            String positionId = entity.getPositionId();
            if (StringUtil.isNotEmpty(positionId)) {
                stepList.addAll(Arrays.asList(positionId.split(",")));
            }
        }
        return stepList;
    }

    @Override
    public Map<String, String> getFlowFormMap() {
        Map<String, String> map = new HashMap<>();
        List<TemplateJsonEntity> listOfEnable = templateJsonMapper.getListOfEnable();
        List<TemplateNodeEntity> listStart = templateNodeMapper.getListStart(listOfEnable);
        List<String> collect = listStart.stream().map(TemplateNodeEntity::getFlowId).collect(Collectors.toList());
        if (ObjectUtil.isNotEmpty(collect)) {
            QueryWrapper<TemplateEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().select(TemplateEntity::getId, TemplateEntity::getFlowId);
            queryWrapper.lambda().in(TemplateEntity::getFlowId, collect);
            Map<String, String> flowTempMap = templateMapper.selectList(queryWrapper).stream().collect(Collectors.toMap(TemplateEntity::getFlowId, TemplateEntity::getId));
            for (TemplateNodeEntity templateNodeEntity : listStart) {
                if (ObjectUtil.isNotEmpty(flowTempMap.get(templateNodeEntity.getFlowId()))) {
                    map.put(flowTempMap.get(templateNodeEntity.getFlowId()), templateNodeEntity.getFormId());
                }
            }
        }
        return map;
    }


    @Override
    public boolean checkSign() {
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getCompletion, FlowNature.NORMAL)
                .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode())
                .isNull(OperatorEntity::getSignTime);
        long count = operatorMapper.selectCount(queryWrapper);
        return count > 0;
    }

    @Override
    public boolean checkTodo() {
        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getCompletion, FlowNature.NORMAL)
                .ne(OperatorEntity::getStatus, OperatorStateEnum.FUTILITY.getCode())
                .isNotNull(OperatorEntity::getSignTime).isNull(OperatorEntity::getStartHandleTime);
        long count = operatorMapper.selectCount(queryWrapper);
        return count > 0;
    }
}

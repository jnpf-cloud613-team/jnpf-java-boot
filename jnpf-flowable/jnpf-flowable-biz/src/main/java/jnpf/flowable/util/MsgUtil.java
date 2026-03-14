package jnpf.flowable.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.ImmutableList;
import jnpf.base.UserInfo;
import jnpf.base.model.systemconfig.SysConfigModel;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.*;
import jnpf.flowable.model.message.ContModel;
import jnpf.flowable.model.message.DelegateModel;
import jnpf.flowable.model.message.FlowEventModel;
import jnpf.flowable.model.message.FlowMsgModel;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.templatenode.nodejson.*;
import jnpf.flowable.model.util.*;
import jnpf.message.model.SentMessageForm;
import jnpf.permission.entity.UserEntity;
import jnpf.util.JsonUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/30 11:45
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MsgUtil {


    private final ServiceUtil serviceUtil;

    private final FlowUtil flowUtil;

    /**
     * 发送消息
     *
     * @param flowMsgModel
     */
    public void message(FlowMsgModel flowMsgModel) {
        List<SentMessageForm> messageListAll = new ArrayList<>();
        TaskEntity task = flowMsgModel.getTaskEntity();
        String taskId = task.getId();
        UserEntity user = StringUtil.isNotEmpty(task.getCreatorUserId()) ? serviceUtil.getUserInfo(task.getCreatorUserId()) : null;
        flowMsgModel.setCreateUser(user);
        UserEntity delegate = StringUtil.isNotEmpty(task.getDelegateUserId()) ? serviceUtil.getUserInfo(task.getDelegateUserId()) : null;
        flowMsgModel.setDelegate(delegate);
        Map<String, Map<String, Object>> formData = flowMsgModel.getFormData();
        String nodeCode = flowMsgModel.getNodeCode();
        List<TemplateNodeEntity> nodeList = flowMsgModel.getNodeList();
        List<OperatorEntity> operatorList = flowMsgModel.getOperatorList();
        List<CirculateEntity> circulateList = flowMsgModel.getCirculateList();
        TemplateNodeEntity startNodeEntity = nodeList.stream().filter(t -> NodeEnum.START.getType().equals(t.getNodeType())).findFirst().orElse(null);
        String nodeJson = startNodeEntity != null ? startNodeEntity.getNodeJson() : "{}";
        NodeModel startNode = JsonUtil.getJsonToBean(nodeJson, NodeModel.class);
        //同意
        if (Boolean.TRUE.equals(flowMsgModel.getApprove())) {
            MsgConfig msgConfig = startNode.getApproveMsgConfig();
            TemplateNodeEntity taskNode = nodeList.stream().filter(t -> t.getNodeCode().equals(nodeCode)).findFirst().orElse(null);
            NodeModel taskChildNode = JsonUtil.getJsonToBean(taskNode.getNodeJson(), NodeModel.class);
            MsgConfig taskMsgConfig = taskChildNode.getApproveMsgConfig();
            if (taskMsgConfig.getOn() == 2) {
                taskMsgConfig = msgConfig;
            }
            if (taskMsgConfig.getOn() == 3) {
                taskMsgConfig.setMsgId("PZXTLC002");
            }
            List<SentMessageForm> messageList = new ArrayList<>();
            flowMsgModel.setOpType(OpTypeEnum.LAUNCH_DETAIL.getType());
            Map<String, Object> data = flowUtil.infoData(startNode.getFormId(), taskId, formData);
            flowMsgModel.setData(data);
            flowMsgModel.setMsgConfig(taskMsgConfig);
            List<OperatorEntity> taskOperatorList = new ArrayList<>();
            OperatorEntity operatorEntity = new OperatorEntity();
            operatorEntity.setTaskId(task.getId());
            operatorEntity.setHandleId(task.getCreatorUserId());
            taskOperatorList.add(operatorEntity);
            FlowMsgModel msgModel = messageModel(taskOperatorList, flowMsgModel);
            setMessageList(messageList, msgModel);
            messageListAll.addAll(messageList);
        }
        // 拒绝
        if (Boolean.TRUE.equals(flowMsgModel.getReject())) {
            MsgConfig msgConfig = startNode.getRejectMsgConfig();
            TemplateNodeEntity taskNode = nodeList.stream().filter(t -> t.getNodeCode().equals(nodeCode)).findFirst().orElse(null);
            NodeModel taskChildNode = JsonUtil.getJsonToBean(taskNode.getNodeJson(), NodeModel.class);
            MsgConfig taskMsgConfig = taskChildNode.getRejectMsgConfig();
            if (taskMsgConfig.getOn() == 2) {
                taskMsgConfig = msgConfig;
            }
            if (taskMsgConfig.getOn() == 3) {
                taskMsgConfig.setMsgId("PZXTLC018");
            }
            List<SentMessageForm> messageList = new ArrayList<>();
            flowMsgModel.setOpType(OpTypeEnum.LAUNCH_DETAIL.getType());
            Map<String, Object> data = flowUtil.infoData(startNode.getFormId(), taskId, formData);
            flowMsgModel.setData(data);
            flowMsgModel.setMsgConfig(taskMsgConfig);
            List<OperatorEntity> taskOperatorList = new ArrayList<>();
            OperatorEntity operatorEntity = new OperatorEntity();
            operatorEntity.setTaskId(task.getId());
            operatorEntity.setHandleId(task.getCreatorUserId());
            taskOperatorList.add(operatorEntity);
            FlowMsgModel msgModel = messageModel(taskOperatorList, flowMsgModel);
            setMessageList(messageList, msgModel);
            messageListAll.addAll(messageList);
        }
        //等待
        if (flowMsgModel.getWait().equals(Boolean.TRUE)) {
            MsgConfig taskMsgConfig = startNode.getWaitMsgConfig();
            if (taskMsgConfig.getOn() == 3) {
                if (Boolean.TRUE.equals(flowMsgModel.getTransfer())) {
                    taskMsgConfig.setMsgId("PZXTLC006");
                } else if (Boolean.TRUE.equals(flowMsgModel.getAssign())) {
                    taskMsgConfig.setMsgId("PZXTLC005");
                } else if (Boolean.TRUE.equals(flowMsgModel.getPress())) {
                    taskMsgConfig.setMsgId("PZXTLC004");
                } else {
                    taskMsgConfig.setMsgId("PZXTLC001");
                }
            }
            Map<String, List<OperatorEntity>> operatorMap = operatorList.stream().collect(Collectors.groupingBy(OperatorEntity::getNodeCode));
            for (Map.Entry<String, List<OperatorEntity>> stringListEntry : operatorMap.entrySet()) {
                String key = stringListEntry.getKey();

                List<SentMessageForm> messageList = new ArrayList<>();
                List<OperatorEntity> taskOperatorList = operatorMap.get(key);
                flowMsgModel.setOpType(OpTypeEnum.SIGN.getType());
                TemplateNodeEntity taskNode = nodeList.stream().filter(t -> t.getNodeCode().equals(key)).findFirst().orElse(null);
                // 获取表单数据
                Map<String, Object> data = flowUtil.infoData(taskNode.getFormId(), taskId, formData);
                flowMsgModel.setData(data);
                flowMsgModel.setMsgConfig(taskMsgConfig);
                FlowMsgModel msgModel = messageModel(taskOperatorList, flowMsgModel);
                setMessageList(messageList, msgModel);
                messageListAll.addAll(messageList);
                for (OperatorEntity operator : taskOperatorList) {
                    List<SentMessageForm> delegationMsg = delegationMsg(operator, msgModel);
                    messageListAll.addAll(delegationMsg);
                }

            }


        }
        //结束
        if (Boolean.TRUE.equals(flowMsgModel.getEnd())) {
            MsgConfig taskMsgConfig = startNode.getEndMsgConfig();
            if (taskMsgConfig.getOn() == 3) {
                taskMsgConfig.setMsgId("PZXTLC010");
            }
            List<SentMessageForm> messageList = new ArrayList<>();
            flowMsgModel.setOpType(OpTypeEnum.LAUNCH_DETAIL.getType());
            Map<String, Object> data = flowUtil.infoData(startNode.getFormId(), taskId, formData);
            flowMsgModel.setData(data);
            flowMsgModel.setMsgConfig(taskMsgConfig);
            List<OperatorEntity> taskOperatorList = new ArrayList<>();
            OperatorEntity operatorEntity = new OperatorEntity();
            operatorEntity.setTaskId(task.getId());
            operatorEntity.setHandleId(task.getCreatorUserId());
            taskOperatorList.add(operatorEntity);
            FlowMsgModel msgModel = messageModel(taskOperatorList, flowMsgModel);
            setMessageList(messageList, msgModel);
            messageListAll.addAll(messageList);
        }
        //退回
        if (Boolean.TRUE.equals(flowMsgModel.getBack())) {
            MsgConfig msgConfig = startNode.getBackMsgConfig();
            Map<String, List<OperatorEntity>> operatorMap = operatorList.stream().collect(Collectors.groupingBy(OperatorEntity::getNodeCode));
            for (Map.Entry<String, List<OperatorEntity>> stringListEntry : operatorMap.entrySet()) {
                String key = stringListEntry.getKey();
                TemplateNodeEntity taskNode = nodeList.stream().filter(t -> t.getNodeCode().equals(nodeCode)).findFirst().orElse(null);
                NodeModel taskChildNode = JsonUtil.getJsonToBean(taskNode.getNodeJson(), NodeModel.class);
                MsgConfig taskMsgConfig = taskChildNode.getBackMsgConfig();
                if (taskMsgConfig.getOn() == 2) {
                    taskMsgConfig = msgConfig;
                }
                if (taskMsgConfig.getOn() == 3) {
                    taskMsgConfig.setMsgId("PZXTLC003");
                }
                List<SentMessageForm> messageList = new ArrayList<>();
                List<OperatorEntity> taskOperatorList = operatorMap.get(key);
                if (ObjectUtil.equals(flowMsgModel.getWait(), true)) {
                    flowMsgModel.setOpType(OpTypeEnum.SIGN.getType());
                }
                Map<String, Object> data = flowUtil.infoData(taskNode.getFormId(), taskId, formData);
                flowMsgModel.setData(data);
                flowMsgModel.setMsgConfig(taskMsgConfig);
                FlowMsgModel msgModel = messageModel(taskOperatorList, flowMsgModel);
                setMessageList(messageList, msgModel);
                messageListAll.addAll(messageList);
                for (OperatorEntity operator : taskOperatorList) {
                    List<SentMessageForm> delegationMsg = delegationMsg(operator, msgModel);
                    messageListAll.addAll(delegationMsg);
                }
            }
        }
        //抄送
        if (Boolean.TRUE.equals(flowMsgModel.getCopy())) {
            MsgConfig msgConfig = startNode.getCopyMsgConfig();
            Map<String, List<CirculateEntity>> circulateMap = circulateList.stream().collect(Collectors.groupingBy(CirculateEntity::getNodeCode));
            for (Map.Entry<String, List<CirculateEntity>> stringListEntry : circulateMap.entrySet()) {
                String key = stringListEntry.getKey();
                TemplateNodeEntity taskNode = nodeList.stream().filter(t -> t.getNodeCode().equals(key)).findFirst().orElse(null);
                NodeModel taskChildNode = JsonUtil.getJsonToBean(taskNode.getNodeJson(), NodeModel.class);
                MsgConfig taskMsgConfig = taskChildNode.getCopyMsgConfig();
                if (taskMsgConfig.getOn() == 2) {
                    taskMsgConfig = msgConfig;
                }
                if (taskMsgConfig.getOn() == 3) {
                    taskMsgConfig.setMsgId("PZXTLC007");
                }
                List<SentMessageForm> messageList = new ArrayList<>();
                List<OperatorEntity> taskOperatorList = new ArrayList<>();
                for (CirculateEntity circulateEntity : circulateMap.get(key)) {
                    OperatorEntity operatorEntity = JsonUtil.getJsonToBean(circulateEntity, OperatorEntity.class);
                    operatorEntity.setHandleId(circulateEntity.getUserId());
                    taskOperatorList.add(operatorEntity);
                }
                flowMsgModel.setOpType(OpTypeEnum.CIRCULATE.getType());
                Map<String, Object> data = flowUtil.infoData(taskNode.getFormId(), taskId, formData);
                flowMsgModel.setData(data);
                flowMsgModel.setMsgConfig(taskMsgConfig);
                FlowMsgModel msgModel = messageModel(taskOperatorList, flowMsgModel);
                setMessageList(messageList, msgModel);
                messageListAll.addAll(messageList);
            }
        }
        //子流程
        if (Boolean.TRUE.equals(flowMsgModel.getLaunch())) {
            MsgConfig msgConfig = startNode.getLaunchMsgConfig();
            Map<String, List<OperatorEntity>> operatorMap = operatorList.stream().collect(Collectors.groupingBy(OperatorEntity::getNodeCode));
            for (Map.Entry<String, List<OperatorEntity>> stringListEntry : operatorMap.entrySet()) {
                String key = stringListEntry.getKey();
                TemplateNodeEntity taskNode = nodeList.stream().filter(t -> t.getNodeCode().equals(key)).findFirst().orElse(null);
                NodeModel taskChildNode = JsonUtil.getJsonToBean(taskNode.getNodeJson(), NodeModel.class);
                MsgConfig taskMsgConfig = taskChildNode.getLaunchMsgConfig();
                if (taskMsgConfig.getOn() == 2) {
                    taskMsgConfig = msgConfig;
                }
                if (taskMsgConfig.getOn() == 3) {
                    taskMsgConfig.setMsgId("PZXTLC011");
                }
                List<SentMessageForm> messageList = new ArrayList<>();
                List<OperatorEntity> taskOperatorList = operatorMap.get(key);
                Map<String, Object> data = flowUtil.infoData(taskNode.getFormId(), taskId, formData);
                flowMsgModel.setData(data);
                flowMsgModel.setMsgConfig(taskMsgConfig);
                flowMsgModel.setOpType(taskChildNode.getAutoSubmit().equals(1) ? OpTypeEnum.LAUNCH_DETAIL.getType() : OpTypeEnum.LAUNCH_CREATE.getType());
                FlowMsgModel msgModel = messageModel(taskOperatorList, flowMsgModel);
                setMessageList(messageList, msgModel);
                messageListAll.addAll(messageList);
            }
        }
        //超时
        if (Boolean.TRUE.equals(flowMsgModel.getOvertime())) {
            MsgConfig msgConfig = startNode.getOverTimeMsgConfig();
            Map<String, List<OperatorEntity>> operatorMap = operatorList.stream().collect(Collectors.groupingBy(OperatorEntity::getNodeCode));
            for (Map.Entry<String, List<OperatorEntity>> stringListEntry : operatorMap.entrySet()) {
                String key = stringListEntry.getKey();
                TemplateNodeEntity taskNode = nodeList.stream().filter(t -> t.getNodeCode().equals(key)).findFirst().orElse(null);
                NodeModel taskChildNode = JsonUtil.getJsonToBean(taskNode.getNodeJson(), NodeModel.class);
                MsgConfig taskMsgConfig = taskChildNode.getOverTimeMsgConfig();
                if (taskMsgConfig.getOn() == 2) {
                    taskMsgConfig = msgConfig;
                }
                if (taskMsgConfig.getOn() == 3) {
                    taskMsgConfig.setMsgId("PZXTLC009");
                }
                List<SentMessageForm> messageList = new ArrayList<>();
                List<OperatorEntity> taskOperatorList = operatorMap.get(key);
                flowMsgModel.setOpType(OpTypeEnum.SIGN.getType());
                Map<String, Object> data = flowUtil.infoData(taskNode.getFormId(), taskId, formData);
                flowMsgModel.setData(data);
                flowMsgModel.setMsgConfig(taskMsgConfig);
                FlowMsgModel msgModel = messageModel(taskOperatorList, flowMsgModel);
                setMessageList(messageList, msgModel);
                messageListAll.addAll(messageList);
                for (OperatorEntity operator : taskOperatorList) {
                    List<SentMessageForm> delegationMsg = delegationMsg(operator, flowMsgModel);
                    messageListAll.addAll(delegationMsg);
                }
            }
        }
        //提醒
        if (Boolean.TRUE.equals(flowMsgModel.getNotice())) {
            MsgConfig msgConfig = startNode.getNoticeMsgConfig();
            Map<String, List<OperatorEntity>> operatorMap = operatorList.stream().collect(Collectors.groupingBy(OperatorEntity::getNodeCode));
            for (Map.Entry<String, List<OperatorEntity>> stringListEntry : operatorMap.entrySet()) {
                String key = stringListEntry.getKey();
                TemplateNodeEntity taskNode = nodeList.stream().filter(t -> t.getNodeCode().equals(key)).findFirst().orElse(null);
                NodeModel taskChildNode = JsonUtil.getJsonToBean(taskNode.getNodeJson(), NodeModel.class);
                MsgConfig taskMsgConfig = taskChildNode.getNoticeMsgConfig();
                if (taskMsgConfig.getOn() == 2) {
                    taskMsgConfig = msgConfig;
                }
                if (taskMsgConfig.getOn() == 3) {
                    taskMsgConfig.setMsgId("PZXTLC008");
                }
                List<SentMessageForm> messageList = new ArrayList<>();
                List<OperatorEntity> taskOperatorList = operatorMap.get(key);
                flowMsgModel.setOpType(OpTypeEnum.SIGN.getType());
                Map<String, Object> data = flowUtil.infoData(taskNode.getFormId(), taskId, formData);
                flowMsgModel.setData(data);
                flowMsgModel.setMsgConfig(taskMsgConfig);
                FlowMsgModel msgModel = messageModel(taskOperatorList, flowMsgModel);
                setMessageList(messageList, msgModel);
                messageListAll.addAll(messageList);
                for (OperatorEntity operator : taskOperatorList) {
                    List<SentMessageForm> delegationMsg = delegationMsg(operator, msgModel);
                    messageListAll.addAll(delegationMsg);
                }
            }
        }
        //评论
        if (Boolean.TRUE.equals(flowMsgModel.getComment())) {
            MsgConfig msgConfig = startNode.getCommentMsgConfig();
            if (msgConfig.getOn() == 3) {
                msgConfig.setMsgId("PZXTLC017");
            }
            Map<String, List<OperatorEntity>> operatorMap = operatorList.stream().collect(Collectors.groupingBy(OperatorEntity::getNodeCode));
            for (Map.Entry<String, List<OperatorEntity>> stringListEntry : operatorMap.entrySet()) {
                String key = stringListEntry.getKey();
                List<SentMessageForm> messageList = new ArrayList<>();
                List<OperatorEntity> taskOperatorList = operatorMap.get(key);
                flowMsgModel.setOpType(OpTypeEnum.CIRCULATE.getType());

                TemplateNodeEntity taskNode = nodeList.stream().filter(t -> t.getNodeCode().equals(key)).findFirst().orElse(null);
                if (BeanUtil.isNotEmpty(taskNode)) {
                    assert taskNode != null;
                    Map<String, Object> data = flowUtil.infoData(taskNode.getFormId(), taskId, formData);
                    flowMsgModel.setData(data);

                }
                flowMsgModel.setMsgConfig(msgConfig);
                FlowMsgModel msgModel = messageModel(taskOperatorList, flowMsgModel);
                setMessageList(messageList, msgModel);
                messageListAll.addAll(messageList);
            }
            //抄送
            Map<String, List<CirculateEntity>> circulateMap = circulateList.stream().collect(Collectors.groupingBy(CirculateEntity::getNodeCode));
            for (Map.Entry<String, List<CirculateEntity>> stringListEntry : circulateMap.entrySet()) {
                String key = stringListEntry.getKey();
                TemplateNodeEntity taskNode = nodeList.stream().filter(t -> t.getNodeCode().equals(key)).findFirst().orElse(null);
                List<SentMessageForm> messageList = new ArrayList<>();
                List<OperatorEntity> taskOperatorList = new ArrayList<>();
                for (CirculateEntity circulateEntity : circulateMap.get(key)) {
                    OperatorEntity operatorEntity = JsonUtil.getJsonToBean(circulateEntity, OperatorEntity.class);
                    operatorEntity.setHandleId(circulateEntity.getUserId());
                    taskOperatorList.add(operatorEntity);
                }
                flowMsgModel.setOpType(OpTypeEnum.CIRCULATE.getType());
                if (BeanUtil.isNotEmpty(taskNode)) {
                    assert taskNode != null;
                    Map<String, Object> data = flowUtil.infoData(taskNode.getFormId(), taskId, formData);
                    flowMsgModel.setData(data);
                }
                flowMsgModel.setMsgConfig(msgConfig);
                FlowMsgModel msgModel = messageModel(taskOperatorList, flowMsgModel);
                setMessageList(messageList, msgModel);
                messageListAll.addAll(messageList);

            }
            if (Boolean.TRUE.equals(flowMsgModel.getStartHandId())) {
                List<SentMessageForm> messageList = new ArrayList<>();
                flowMsgModel.setOpType(OpTypeEnum.LAUNCH_DETAIL.getType());
                Map<String, Object> data = flowUtil.infoData(startNode.getFormId(), taskId, formData);
                flowMsgModel.setData(data);
                OperatorEntity operatorEntity = new OperatorEntity();
                operatorEntity.setTaskId(task.getId());
                operatorEntity.setHandleId(task.getCreatorUserId());
                List<OperatorEntity> meOperatorList = new ArrayList<>();
                meOperatorList.add(operatorEntity);
                FlowMsgModel msgModel = messageModel(meOperatorList, flowMsgModel);
                setMessageList(messageList, msgModel);
                messageListAll.addAll(messageList);
            }
        }
        for (SentMessageForm sentMessageForm : messageListAll) {
            sentMessageForm.setFlowName(task.getFullName());
            sentMessageForm.setUserName(user != null ? user.getRealName() : "");
        }
        serviceUtil.sendMessage(messageListAll);
    }

    private void setMessageList(List<SentMessageForm> messageList, FlowMsgModel flowMsgModel) {
        HashMap<String, Object> data = new HashMap<>(flowMsgModel.getData());
        MsgConfig msgConfig = flowMsgModel.getMsgConfig() != null ? flowMsgModel.getMsgConfig() : new MsgConfig();
        List<String> userList = flowMsgModel.getUserList();
        UserInfo userInfo = flowMsgModel.getUserInfo();
        TaskEntity task = flowMsgModel.getTaskEntity();
        UserEntity createUser = flowMsgModel.getCreateUser();
        UserEntity delegate = flowMsgModel.getDelegate();
        if (!userList.isEmpty()) {
            String templateId = msgConfig.getOn() == 0 ? "0" : msgConfig.getMsgId();
            boolean sysMessage = msgConfig.getOn() != 0;
            //解析发送配置json，获取消息模板参数
            List<TemplateJsonModel> templateJson = new ArrayList<>();
            for (SendConfigJson configJson : msgConfig.getTemplateJson()) {
                List<TemplateJsonModel> paramJson = configJson.getParamJson();
                templateJson.addAll(paramJson);
                List<String> list = ImmutableList.of(FlowConstant.MANDATOR, FlowConstant.MANDATARY, FlowConstant.CREATORUSERNAME, FlowConstant.SENDTIME);
                for (String field : list) {
                    TemplateJsonModel jsonModel = new TemplateJsonModel();
                    jsonModel.setMsgTemplateId(configJson.getId());
                    jsonModel.setRelationField(field);
                    jsonModel.setField(field);
                    jsonModel.setSourceType(FieldEnum.SYSTEM.getCode());
                    templateJson.add(jsonModel);
                }
            }
            SentMessageForm sentMessageForm = new SentMessageForm();
            sentMessageForm.setSysMessage(sysMessage);
            sentMessageForm.setTemplateId(templateId);
            sentMessageForm.setToUserIds(userList);
            RecordEntity recordEntity = new RecordEntity();
            recordEntity.setNodeCode(flowMsgModel.getNodeCode());
            FlowModel parameterModel = new FlowModel();
            parameterModel.setFormData(data);
            parameterModel.setRecordEntity(recordEntity);
            parameterModel.setTaskEntity(task);
            Map<String, String> parameterMap = FlowUtil.parameterMap(parameterModel, templateJson, createUser, delegate);
            data.putAll(parameterMap);
            sentMessageForm.setUserInfo(userInfo);
            sentMessageForm.setParameterMap(data);
            sentMessageForm.setContentMsg(flowMsgModel.getContMsg());
            sentMessageForm.setTitle(task.getFullName());
            messageList.add(sentMessageForm);
        }
    }

    private FlowMsgModel messageModel(List<OperatorEntity> taskOperatorList, FlowMsgModel flowMsgModel) {
        FlowMsgModel msgModel = JsonUtil.getJsonToBean(flowMsgModel, FlowMsgModel.class);
        List<String> userList = new ArrayList<>();
        TaskEntity task = flowMsgModel.getTaskEntity();
        Map<String, String> contMsg = new HashMap<>();
        for (OperatorEntity taskOperator : taskOperatorList) {
            ContModel contModel = new ContModel();
            contModel.setFlowId(task.getFlowId());
            contModel.setOperatorId(taskOperator.getId());
            contModel.setTaskId(task.getId());
            contModel.setOpType(flowMsgModel.getOpType());
            if (StringUtils.equals(flowMsgModel.getOpType(), OpTypeEnum.SIGN.getType())) {
                if (null == taskOperator.getSignTime() && null == taskOperator.getStartHandleTime() && null == taskOperator.getHandleStatus()) {
                    contModel.setOpType(OpTypeEnum.SIGN.getType());
                } else if (null != taskOperator.getSignTime() && null == taskOperator.getStartHandleTime() && null == taskOperator.getHandleStatus()) {
                    contModel.setOpType(OpTypeEnum.TODO.getType());
                } else if (null != taskOperator.getSignTime() && null != taskOperator.getStartHandleTime() && null == taskOperator.getHandleStatus()) {
                    contModel.setOpType(OpTypeEnum.DOING.getType());
                }
            }
            contMsg.put(taskOperator.getHandleId(), JsonUtil.getObjectToString(contModel));
            userList.add(taskOperator.getHandleId());
        }
        msgModel.setUserList(userList);
        msgModel.setContMsg(contMsg);
        return msgModel;
    }

    private List<SentMessageForm> delegationMsg(OperatorEntity operator, FlowMsgModel flowMsgModel) {
        List<SentMessageForm> messageList = new ArrayList<>();
        TaskEntity task = flowMsgModel.getTaskEntity();
        if (task != null) {
            // 获取委托人
            List<String> userList = flowUtil.getToUser(operator.getHandleId(), task.getFlowId());
            List<OperatorEntity> operatorList = new ArrayList<>();
            for (String user : userList) {
                OperatorEntity delegate = JsonUtil.getJsonToBean(operator, OperatorEntity.class);
                delegate.setHandleId(user);
                operatorList.add(delegate);
                FlowMsgModel msgModel = messageModel(operatorList, flowMsgModel);
                setMessageList(messageList, msgModel);
            }
        }
        return messageList;
    }


    //--------------------------------------委托消息------------------------------------------------------
    public void delegateMsg(DelegateModel flowDelegate) {
        SysConfigModel sysConfig = serviceUtil.getSysConfig();
        // 委托/代理确认通知
        Integer ack = Boolean.TRUE.equals(flowDelegate.getDelegate()) ? sysConfig.getDelegateAck() : sysConfig.getProxyAck();
        flowDelegate.setAck(ack);
        List<String> toUserIds = flowDelegate.getToUserIds();
        if (!toUserIds.isEmpty()) {
            UserInfo userInfo = flowDelegate.getUserInfo();
            TaskEntity flowTask = flowDelegate.getFlowTask();
            Map<String, String> contentMsg = new HashMap<>();
            Boolean delegate = flowDelegate.getDelegate();
            boolean approve = flowDelegate.getApprove();
            if (approve) {
                SentMessageForm flowMsgModel = new SentMessageForm();
                flowMsgModel.setToUserIds(toUserIds);
                flowMsgModel.setUserInfo(flowDelegate.getUserInfo());
                Map<String, Object> parameterMap = new HashMap<>();
                parameterMap.put(FlowConstant.MANDATOR, userInfo.getUserName());
                UserEntity mandatary = StringUtil.isNotEmpty(flowTask.getDelegateUserId()) ? serviceUtil.getUserInfo(flowTask.getDelegateUserId()) : null;
                parameterMap.put(FlowConstant.MANDATARY, mandatary != null ? mandatary.getRealName() : "");
                parameterMap.put(FlowConstant.TITLE, Boolean.TRUE.equals(delegate) ? "委托" : "代理");
                flowMsgModel.setParameterMap(parameterMap);
                //1.委托设置 2.委托给我
                String s = ObjectUtil.equals(ack, 1) ? "PZXTLG020" : "PZXTLG019";
                Integer type = flowDelegate.getType();
                String templateId = FlowNature.END_MSG.equals(type) ? "PZXTLG021" : s;
                flowMsgModel.setTemplateId(templateId);
                int i = Boolean.TRUE.equals(delegate) ? 2 : 4;
                Integer delegateType = FlowNature.END_MSG.equals(type) ? 0 : i;
                contentMsg.put("type", delegateType + "");

                flowMsgModel.setContentMsg(contentMsg);
                flowMsgModel.setFlowType(2);
                flowMsgModel.setType(2);
                List<SentMessageForm> messageListAll = new ArrayList<>();
                messageListAll.add(flowMsgModel);
                serviceUtil.sendDelegateMsg(messageListAll);
            }
        }
    }


    // 接口调用的参数需转为字符串
    public Map<String, String> intefaceParameterMap(Map<String, Object> data, List<TemplateJsonModel> templateJsonModelList, RecordEntity recordEntity, TaskEntity task, UserEntity createUser, UserEntity delegate) {
        Map<String, String> parameterMap = new HashMap<>();
        for (TemplateJsonModel templateJsonModel : templateJsonModelList) {
            String fieldId = templateJsonModel.getField();
            String msgTemplateId = templateJsonModel.getMsgTemplateId();
            String relationField = templateJsonModel.getRelationField();
            boolean isList = data.get(relationField) instanceof List;
            String s = isList ? JsonUtil.getObjectToString(data.get(relationField)) : String.valueOf(data.get(relationField));
            String dataValue = data.get(relationField) != null ? s : null;
            String dataFieldValue = relationField;
            String dataJson = Objects.equals(FieldEnum.FIELD.getCode(), templateJsonModel.getSourceType()) ? dataValue : dataFieldValue;
            FlowEventModel eventModel = FlowEventModel.builder().data(data).dataJson(dataJson).recordEntity(recordEntity).templateJson(templateJsonModel).taskEntity(task).createUser(createUser).delegate(delegate).build();
            dataJson = FlowUtil.data(eventModel);
            parameterMap.put(StringUtil.isNotEmpty(msgTemplateId) ? msgTemplateId : "" + fieldId, dataJson);
        }
        return parameterMap;
    }
}

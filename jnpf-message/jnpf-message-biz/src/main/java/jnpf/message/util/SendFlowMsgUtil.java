package jnpf.message.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.Method;
import com.alibaba.fastjson.JSONObject;
import jnpf.base.UserInfo;
import jnpf.base.entity.SysConfigEntity;
import jnpf.base.service.SysconfigService;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.KeyConst;
import jnpf.constant.MsgSysParam;
import jnpf.exception.DataException;
import jnpf.flowable.model.trigger.TriggerModel;
import jnpf.message.entity.*;
import jnpf.message.enums.MessageTypeEnum;
import jnpf.message.mapper.*;
import jnpf.message.model.SentMessageForm;
import jnpf.message.service.SendMsgService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class SendFlowMsgUtil implements SendMsgService {
    private final RedisUtil redisUtil;
    private final SentMessageUtil sentMessageUtil;
    private final ConfigValueUtil configValueUtil;
    private final SysconfigService sysconfigService;
    private final MessageMapper messageMapper;
    private final MessagereceiveMapper messagereceiveMapper;
    private final MessageMonitorMapper messageMonitorMapper;
    private final SendMessageConfigMapper sendMessageConfigMapper;
    private final SendConfigTemplateMapper sendConfigTemplateMapper;
    private final MessageTemplateConfigMapper messageTemplateConfigMapper;
    private final ShortLInkMapper shortLInkMapper;
    private final AccountConfigMapper accountConfigMapper;

    private static final String MSG_ERR1 = "发送企业微信消息失败，错误：";
    private static final String FLOWBEFORE_INDEX = "/pages/workFlow/flowBefore/index?config=";


    public void sendMessage(SentMessageForm sentMessageForm) {
        List<String> toUserIdsList = sentMessageForm.getToUserIds();
        // 模板id
        String templateId = sentMessageForm.getTemplateId();
        // 参数
        Map<String, Object> parameterMap = sentMessageForm.getParameterMap();
        UserInfo userInfo = sentMessageForm.getUserInfo();
        boolean flag = true;
        if (!(toUserIdsList != null && !toUserIdsList.isEmpty())) {
            log.error("接收人员为空");
            flag = false;
        }
        if (StringUtil.isEmpty(templateId)) {
            log.error("模板Id为空");
            flag = false;
        }
        if (flag) {
            // 获取发送配置详情
            SendMessageConfigEntity entity = sendMessageConfigMapper.getInfoByEnCode(templateId);
            if (entity != null) {
                templateId = entity.getId();
            } else {
                entity = sendMessageConfigMapper.getInfo(templateId);
            }
            if (entity != null) {
                List<SendConfigTemplateEntity> list = sendConfigTemplateMapper.getDetailListByParentId(templateId);
                if (list != null && !list.isEmpty()) {
                    for (SendConfigTemplateEntity entity1 : list) {
                        if (parameterMap.get(entity1.getId() + MsgSysParam.TITLE) == null) {
                            parameterMap.put(entity1.getId() + MsgSysParam.TITLE, sentMessageForm.getTitle());
                        }
                        if (parameterMap.get(entity1.getId() + MsgSysParam.CREATOR_USER_NAME) == null || StringUtil.isEmpty(String.valueOf(parameterMap.get(entity1.getId() + MsgSysParam.CREATOR_USER_NAME)))) {
                            parameterMap.put(entity1.getId() + MsgSysParam.CREATOR_USER_NAME, sentMessageForm.getUserInfo().getUserName());
                        }
                        if (parameterMap.get(entity1.getId() + MsgSysParam.SEND_TIME) == null || StringUtil.isEmpty(String.valueOf(parameterMap.get(entity1.getId() + MsgSysParam.SEND_TIME)))) {
                            parameterMap.put(entity1.getId() + MsgSysParam.SEND_TIME, DateUtil.getNow().substring(11));
                        }
                        if (parameterMap.get(entity1.getId() + MsgSysParam.FLOW_LINK) == null) {
                            parameterMap.put(entity1.getId() + MsgSysParam.FLOW_LINK, "");
                        }
                        if ("1".equals(String.valueOf(entity1.getEnabledMark()))) {
                            String sendType = entity1.getMessageType();
                            MessageTypeEnum typeEnum = MessageTypeEnum.getByCode(sendType);
                            Map<String, String> contentMsg = sentMessageForm.getContentMsg();
                            switch (typeEnum) {
                                case SYS_MESSAGE:
                                    // 站内消息、
                                    for (String toUserId : toUserIdsList) {
                                        List<String> toUser = new ArrayList<>();
                                        String content = sentMessageForm.getContent();
                                        MessageTemplateConfigEntity templateConfigEntity = messageTemplateConfigMapper.getInfo(entity1.getTemplateId());
                                        String title = sentMessageForm.getTitle();
                                        String appLink = "";
                                        if (templateConfigEntity != null) {
                                            title = templateConfigEntity.getTitle();
                                            String msg = contentMsg.get(toUserId) != null ? contentMsg.get(toUserId) : "{}";
                                            byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
                                            String encode = Base64.getEncoder().encodeToString(bytes);
                                            //流程审批页面链接地址
                                            //流程审批页面链接地址
                                            String pcLink = "/workFlowDetail?config=" + encode;
                                            appLink = FLOWBEFORE_INDEX + encode;
                                            //转换为短链
                                            String shortLink = shortLInkMapper.shortLink(pcLink + toUserId + templateConfigEntity.getMessageType());
                                            String link = configValueUtil.getApiDomain() + "/api/message/ShortLink/" + shortLink;
                                            if (StringUtils.isNotBlank(userInfo.getTenantId())) {
                                                link = link + "/" + userInfo.getTenantId();
                                            }
                                            if (title.contains("{@FlowLink}")) {
                                                title = title.replace("{@FlowLink}", link + " ");
                                                //链接数据保存
                                                sentMessageUtil.saveShortLink(pcLink, appLink, shortLink, userInfo, toUserId, msg);
                                            }
                                            Map<String, Object> msgMap = SentMessageUtil.getParamMap(entity1.getId(), parameterMap);
                                            if (StringUtil.isNotEmpty(title)) {
                                                StringSubstitutor strSubstitutor = new StringSubstitutor(msgMap, "{", "}");
                                                title = strSubstitutor.replace(title);
                                            }
                                        }
                                        toUser.add(toUserId);
                                        messagereceiveMapper.sentMessage(toUser, title, content, contentMsg, userInfo);
                                        //消息监控写入
                                        MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
                                        monitorEntity.setId(RandomUtil.uuId());
                                        monitorEntity.setReceiveUser(JsonUtil.getObjectToString(toUser));
                                        monitorEntity.setSendTime(DateUtil.getNowDate());
                                        monitorEntity.setCreatorTime(DateUtil.getNowDate());
                                        monitorEntity.setCreatorUserId(userInfo.getUserId());
                                        SentMessageUtil.createMessageMonitor(monitorEntity, templateConfigEntity, null, null, userInfo, toUser, title);
                                        messageMonitorMapper.insert(monitorEntity);
                                    }
                                    String url = configValueUtil.getApiDomain() + "/api/workflow/trigger/MsgExecute";
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("id", entity1.getTemplateId());
                                    map.put("userInfo", userInfo);
                                    HttpRequest request = HttpRequest.of(url).method(Method.POST).body(JsonUtil.getObjectToString(map));
                                    request.header(Constants.AUTHORIZATION, userInfo.getToken());
                                    request.execute().body();
                                    break;
                                case SMS_MESSAGE:
                                    // 发送短信
                                    sentMessageUtil.sendSms(toUserIdsList, userInfo, entity1, parameterMap, contentMsg);
                                    break;
                                case MAIL_MESSAGE:
                                    // 邮件
                                    sentMessageUtil.sendMail(toUserIdsList, userInfo, sendType, entity1, parameterMap, contentMsg);
                                    break;
                                case QY_MESSAGE:
                                    // 企业微信
                                    JSONObject jsonObject = sentMessageUtil.sendQyWebChat(toUserIdsList, userInfo, sendType, entity1, parameterMap, contentMsg);
                                    if (!jsonObject.getBooleanValue(KeyConst.CODE)) {
                                        log.error(MSG_ERR1 + jsonObject.get(KeyConst.ERROR));
                                    }
                                    break;
                                case DING_MESSAGE:
                                    // 钉钉
                                    JSONObject jsonObject1 = sentMessageUtil.sendDingTalk(toUserIdsList, userInfo, sendType, entity1, parameterMap, contentMsg);
                                    if (!jsonObject1.getBooleanValue(KeyConst.CODE)) {
                                        log.error(MSG_ERR1 + jsonObject1.get(KeyConst.ERROR));
                                    }
                                    break;
                                case WEB_HOOK_MESSAGE:
                                    // webhook
                                    this.sendWebHook(userInfo, entity1, parameterMap, new HashMap<>());
                                    break;
                                case WECHAT_MESSAGE:
                                    // 微信公众号
                                    sentMessageUtil.sendWXGzhChat(toUserIdsList, userInfo, entity1, contentMsg, parameterMap);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }

    public List<String> sendScheduleMessage(SentMessageForm sentMessageForm) {
        List<String> errList = new ArrayList<>();
        UserInfo userInfo = sentMessageForm.getUserInfo();
        String templateId = sentMessageForm.getTemplateId();
        List<String> toUserIds = sentMessageForm.getToUserIds();
        //获取发送配置详情
        SendMessageConfigEntity configEntity = sendMessageConfigMapper.getInfoByEnCode(templateId);
        if (configEntity != null) {
            templateId = configEntity.getId();
        } else {
            configEntity = sendMessageConfigMapper.getInfo(templateId);
        }
        List<SendConfigTemplateEntity> list = sendConfigTemplateMapper.getDetailListByParentId(templateId);
        if (configEntity != null) {
            if (configEntity.getMessageSource() != null) {
                sentMessageForm.setType(Integer.valueOf(configEntity.getMessageSource()));
            }
            for (SendConfigTemplateEntity sendConfigTemplateEntity : list) {
                Map<String, Object> objectMap = new HashMap<>(sentMessageForm.getParameterMap());
                Map<String, Object> parameterMap = new HashMap<>();
                for (Map.Entry<String, Object> objItem : objectMap.entrySet()) {
                    if (objItem.getKey().contains(sendConfigTemplateEntity.getId())) {
                        parameterMap.put(objItem.getKey().substring(sendConfigTemplateEntity.getId().length()), objItem.getValue());
                    }
                }
                parameterMap.putAll(objectMap);
                Map<String, String> contentMsg = new HashMap<>();
                for (Map.Entry<String, Object> paramItem : parameterMap.entrySet()) {
                    contentMsg.put(paramItem.getKey(), String.valueOf(paramItem.getValue()));
                }
                if (Objects.equals(sentMessageForm.getType(), 5)) {
                    Map<String, String> formDataContentMsg = new HashMap<>();
                    formDataContentMsg.put("formDataId", contentMsg.get(MsgSysParam.FORM_DATA_ID));
                    formDataContentMsg.put("formTemplateId", contentMsg.get(MsgSysParam.FORM_TEMPLATE_ID));
                    sentMessageForm.setContentMsg(formDataContentMsg);
                }
                String sendType = sendConfigTemplateEntity.getMessageType();
                switch (sendType) {
                    case "1":
                        MessageTemplateConfigEntity templateConfigEntity = messageTemplateConfigMapper.getInfo(sendConfigTemplateEntity.getTemplateId());
                        String messageTitle = StringUtil.isNotEmpty(templateConfigEntity.getTitle()) ? templateConfigEntity.getTitle() : "";
                        String content = StringUtil.isNotEmpty(templateConfigEntity.getContent()) ? templateConfigEntity.getContent() : "";
                        StringSubstitutor strSubstitutor = new StringSubstitutor(parameterMap, "{", "}");
                        messageTitle = strSubstitutor.replace(messageTitle);
                        content = strSubstitutor.replace(content);
                        sentMessageForm.setTitle(messageTitle);
                        sentMessageForm.setContent(content);
                        // 站内消息
                        message(sentMessageForm);
                        // 通知触发
                        try {
                            String url = configValueUtil.getApiDomain() + "/api/workflow/trigger/MsgExecute";
                            TriggerModel model = new TriggerModel();
                            model.setUserInfo(userInfo);
                            model.setId(templateConfigEntity.getId());
                            HttpRequest request = HttpRequest.of(url).method(Method.POST).body(JsonUtil.getObjectToString(model));
                            request.header(Constants.AUTHORIZATION, userInfo.getToken());
                            request.execute().body();
                        } catch (Exception e) {
                            log.error("消息触发流程报错信息:" + e.getMessage());
                        }
                        break;
                    case "2":
                        // 邮件
                        List<String> mailErrs = sentMessageUtil.sendMail(toUserIds, userInfo, sendType, sendConfigTemplateEntity, new HashMap<>(), contentMsg);
                        errList.addAll(mailErrs.stream().distinct().collect(Collectors.toList()));
                        break;
                    case "3":
                        // 发送短信
                        List<String> smsErrs = sentMessageUtil.sendSms(toUserIds, userInfo, sendConfigTemplateEntity, parameterMap, new HashMap<>());
                        errList.addAll(smsErrs.stream().distinct().collect(Collectors.toList()));
                        break;
                    case "4":
                        // 钉钉
                        JSONObject jsonObject1 = sentMessageUtil.sendDingTalk(toUserIds, userInfo, sendType, sendConfigTemplateEntity, new HashMap<>(), contentMsg);
                        if (!jsonObject1.getBooleanValue(KeyConst.CODE)) {
                            log.error(MSG_ERR1 + jsonObject1.get(KeyConst.ERROR));
                            errList.add("发送钉钉消息失败，错误：" + jsonObject1.get(KeyConst.ERROR));
                        }
                        break;
                    case "5":
                        // 企业微信
                        JSONObject jsonObject = sentMessageUtil.sendQyWebChat(toUserIds, userInfo, sendType, sendConfigTemplateEntity, new HashMap<>(), contentMsg);
                        if (!jsonObject.getBooleanValue(KeyConst.CODE)) {
                            log.error(MSG_ERR1 + jsonObject.get(KeyConst.ERROR));
                            errList.add(MSG_ERR1 + jsonObject.get(KeyConst.ERROR));
                        }
                        break;
                    case "6":
                        // webhook
                        this.sendWebHook(userInfo, sendConfigTemplateEntity, new HashMap<>(), contentMsg);
                        break;
                    case "7":
                        // 微信公众号
                        JSONObject jsonObject2 = sentMessageUtil.sendWXGzhChat(toUserIds, userInfo, sendConfigTemplateEntity, new HashMap<>(), parameterMap);
                        if (!jsonObject2.getBooleanValue(KeyConst.CODE)) {
                            errList.add("发送微信公众号消息失败，错误：" + jsonObject2.get(KeyConst.ERROR));
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return errList;
    }

    public void sendWebHook(UserInfo userInfo, SendConfigTemplateEntity entity, Map<String, Object> parameterMap, Map<String, String> contentMsg) {
        MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigMapper.getInfo(entity.getTemplateId());
        AccountConfigEntity accountEntity = accountConfigMapper.getInfo(entity.getAccountConfigId());
        MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
        monitorEntity.setId(RandomUtil.uuId());
        monitorEntity.setSendTime(DateUtil.getNowDate());
        monitorEntity.setCreatorTime(DateUtil.getNowDate());
        monitorEntity.setCreatorUserId(userInfo.getUserId());
        String content = msgTemEntity.getContent();
        //获取消息模板参数
        parameterMap = SentMessageUtil.getParamMap(entity.getId(), parameterMap);
        // 替换参数
        if (StringUtil.isNotEmpty(content)) {
            StringSubstitutor strSubstitutor = new StringSubstitutor(parameterMap, "{", "}");
            content = strSubstitutor.replace(content);
        }
        String title = msgTemEntity.getTitle();
        if (StringUtil.isNotEmpty(title)) {
            StringSubstitutor strSubstitutor = new StringSubstitutor(parameterMap, "{", "}");
            title = strSubstitutor.replace(title);
        }
        title = SentMessageUtil.systemParam(parameterMap, contentMsg, title, userInfo);
        content = SentMessageUtil.systemParam(parameterMap, contentMsg, content, userInfo);

        if (accountEntity != null) {
            //创建消息监控
            monitorEntity = SentMessageUtil.createMessageMonitor(monitorEntity, msgTemEntity, accountEntity, content, userInfo, null, title);
            messageMonitorMapper.create(monitorEntity);
            switch (accountEntity.getWebhookType()) {
                case 1:
                    //钉钉
                    if (Objects.equals(1, accountEntity.getApproveType())) {
                        WebHookUtil.sendDDMessage(accountEntity.getWebhookAddress(), content);
                    } else if (Objects.equals(2, accountEntity.getApproveType())) {
                        WebHookUtil.sendDingDing(accountEntity.getWebhookAddress(), accountEntity.getBearer(), content);
                    }
                    break;
                case 2:
                    if (Objects.equals(1, accountEntity.getApproveType())) {
                        WebHookUtil.callWeChatBot(accountEntity.getWebhookAddress(), content);
                    }
                    break;
                default:
                    break;
            }
        } else {
            monitorEntity = SentMessageUtil.createMessageMonitor(monitorEntity, msgTemEntity, null, content, userInfo, null, title);
            messageMonitorMapper.create(monitorEntity);
        }
    }


    public boolean sentNotice(List<String> toUserIds, MessageEntity entity) {
        // 存到redis中的key对象
        UserInfo userInfo = UserProvider.getUser();
        List<String> idList = new ArrayList<>();
        // 修改发送状态
        entity.setEnabledMark(1);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(userInfo.getUserId());
        messageMapper.updateById(entity);
        // 存到redis，生成Redis的key
        Callable<Boolean> executeInsert = () -> {
            executeInsert(toUserIds, idList);
            return true;
        };
        Future<Boolean> submit = ThreadPoolExecutorUtil.getExecutor().submit(executeInsert);
        try {
            if (Boolean.TRUE.equals(submit.get())) {
                // 执行发送公告操作
                Runnable runnable = () -> executeBatch(idList, entity, userInfo);
                ThreadPoolExecutorUtil.getExecutor().submit(runnable);
            }
            return true;
        } catch (Exception e) {
            // 还原公告状态
            entity.setEnabledMark(0);
            entity.setLastModifyTime(null);
            entity.setLastModifyUserId(null);
            messageMapper.updateById(entity);
            // 如果是中断异常，恢复中断状态
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private void executeBatch(List<String> idList, MessageEntity entity, UserInfo userInfo) {
        if (idList.isEmpty() || "3".equals(String.valueOf(entity.getRemindCategory()))) {
            return;
        }
        SentMessageForm sentMessageForm = new SentMessageForm();
        List<String> toUserId = new ArrayList<>();
        for (String cacheKey : idList) {
            List<String> cacheValue = (List) redisUtil.get(cacheKey, 0, -1);
            toUserId.addAll(cacheValue);
        }
        sentMessageForm.setToUserIds(toUserId);
        sentMessageForm.setTitle(entity.getTitle());
        sentMessageForm.setContent(entity.getBodyText());
        sentMessageForm.setContentMsg(Collections.emptyMap());
        sentMessageForm.setUserInfo(userInfo);
        sentMessageForm.setType(1);
        sentMessageForm.setId(entity.getId());

        // 站内信
        if ("1".equals(String.valueOf(entity.getRemindCategory()))) {
            message(sentMessageForm);
        } else if ("2".equals(String.valueOf(entity.getRemindCategory()))) {
            SendMessageConfigEntity sendMessageConfigEntity = sendMessageConfigMapper.getInfo(entity.getSendConfigId());
            if (sendMessageConfigEntity != null) {
                List<SendConfigTemplateEntity> configTemplateEntityList = sendConfigTemplateMapper.getDetailListByParentId(sendMessageConfigEntity.getId());
                for (SendConfigTemplateEntity sendConfigTemplateEntity : configTemplateEntityList) {
                    Map<String, String> map = new HashMap<>();
                    map.put("Title", entity.getTitle());
                    map.put("Content", entity.getBodyText());
                    map.put("Remark", entity.getExcerpt());
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put(MsgSysParam.TITLE, entity.getTitle());
                    paramMap.put(MsgSysParam.CONTENT, entity.getBodyText());
                    paramMap.put(MsgSysParam.REMARK, entity.getExcerpt());
                    paramMap.put(MsgSysParam.CREATOR_USER_NAME, userInfo.getUserName());
                    paramMap.put(MsgSysParam.SEND_TIME, DateUtil.getNow().substring(11));
                    paramMap.put(sendConfigTemplateEntity.getId() + MsgSysParam.TITLE, entity.getTitle());
                    paramMap.put(sendConfigTemplateEntity.getId() + MsgSysParam.CONTENT, entity.getBodyText());
                    paramMap.put(sendConfigTemplateEntity.getId() + MsgSysParam.REMARK, entity.getExcerpt());
                    paramMap.put(sendConfigTemplateEntity.getId() + MsgSysParam.CREATOR_USER_NAME, userInfo.getUserName());
                    paramMap.put(sendConfigTemplateEntity.getId() + MsgSysParam.SEND_TIME, DateUtil.getNow().substring(11));
                    switch (sendConfigTemplateEntity.getMessageType()) {
                        case "1":
                            MessageTemplateConfigEntity configEntity = messageTemplateConfigMapper.getInfo(sendConfigTemplateEntity.getTemplateId());
                            if (configEntity != null) {
                                sentMessageForm.setTitle(configEntity.getTitle());
                            }
                            message(sentMessageForm);
                            break;
                        case "2":
                            // 邮件
                            sentMessageUtil.sendMail(toUserId, userInfo, "2", sendConfigTemplateEntity, new HashMap<>(), map);
                            break;
                        case "3":
                            // 发送短信
                            sentMessageUtil.sendSms(toUserId, userInfo, sendConfigTemplateEntity, paramMap, new HashMap<>());
                            break;
                        case "4":
                            // 钉钉
                            JSONObject jsonObject1 = sentMessageUtil.sendDingTalk(toUserId, userInfo, "4", sendConfigTemplateEntity, new HashMap<>(), map);
                            if (!jsonObject1.getBooleanValue(KeyConst.CODE)) {
                                log.error(MSG_ERR1 + jsonObject1.get(KeyConst.ERROR));
                            }
                            break;
                        case "5":
                            // 企业微信
                            JSONObject jsonObject = sentMessageUtil.sendQyWebChat(toUserId, userInfo, "5", sendConfigTemplateEntity, new HashMap<>(), map);
                            if (!jsonObject.getBooleanValue(KeyConst.CODE)) {
                                log.error(MSG_ERR1 + jsonObject.get(KeyConst.ERROR));
                            }
                            break;
                        case "6":
                            // webhook
                            this.sendWebHook(userInfo, sendConfigTemplateEntity, new HashMap<>(), map);
                            break;
                        case "7":
                            // 微信公众号
                            sentMessageUtil.sendWXGzhChat(toUserId, userInfo, sendConfigTemplateEntity, new HashMap<>(), paramMap);
                            break;
                        default:
                            break;
                    }
                }

            }
        }
    }

    private void message(SentMessageForm sentMessageForm) {
        List<String> toUserIds = sentMessageForm.getToUserIds();
        Integer type = sentMessageForm.getType();
        String title = sentMessageForm.getTitle();
        String content = sentMessageForm.getContent();
        String bodyText = Objects.equals(type, 3) ? content : JsonUtil.getObjectToString(sentMessageForm.getContentMsg());
        UserInfo userInfo = sentMessageForm.getUserInfo();
        MessageReceiveEntity messageReceiveEntity = new MessageReceiveEntity();
        messageReceiveEntity.setIsRead(0);
        messageReceiveEntity.setId(RandomUtil.uuId());
        messageReceiveEntity.setType(sentMessageForm.getType());
        if (type != null) {
            messageReceiveEntity.setId(sentMessageForm.getId());
            messageReceiveEntity.setType(type);
            messageReceiveEntity.setCreatorUserId(userInfo.getUserId());
            messageReceiveEntity.setCreatorTime(DateUtil.getNowDate());
        }
        //消息监控写入
        MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
        MessageEntity messageEntity = messageMapper.getInfo(sentMessageForm.getId());
        if (!"1".equals(String.valueOf(messageReceiveEntity.getType()))) {
            monitorEntity.setMessageSource(sentMessageForm.getType() + "");
            messageReceiveEntity.setFlowType(sentMessageForm.getFlowType());
            monitorEntity.setTitle(title);
        } else {
            monitorEntity.setMessageSource("1");
            title = title.replace("\\{@Title}", messageEntity.getTitle())
                    .replace("\\{@CreatorUserName}", userInfo.getUserName())
                    .replace("\\{@SendTime}", DateUtil.getNow().substring(11))
                    .replace("\\{@Content}", messageEntity.getBodyText())
                    .replace("\\{@Remark}", StringUtil.isNotEmpty(messageEntity.getExcerpt()) ? messageEntity.getExcerpt() : "");
            monitorEntity.setTitle(title);

            MessageEntity messageEntity2 = new MessageEntity();
            messageEntity2.setId(messageEntity.getId());
            bodyText = JsonUtil.getObjectToString(messageEntity2);
        }
        Map<String, MessageReceiveEntity> map = new HashMap<>();
        for (String item : toUserIds) {
            MessageReceiveEntity messageReceiveEntitys = new MessageReceiveEntity();
            BeanUtils.copyProperties(messageReceiveEntity, messageReceiveEntitys);
            messageReceiveEntitys.setId(RandomUtil.uuId());
            messageReceiveEntitys.setUserId(item);
            messageReceiveEntitys.setTitle(title);
            messageReceiveEntitys.setBodyText(bodyText);
            messagereceiveMapper.insert(messageReceiveEntitys);
            map.put(messageReceiveEntitys.getUserId(), messageReceiveEntitys);
        }
        monitorEntity.setId(RandomUtil.uuId());
        monitorEntity.setMessageType("1");
        monitorEntity.setReceiveUser(JsonUtil.getObjectToString(toUserIds));
        monitorEntity.setSendTime(DateUtil.getNowDate());
        monitorEntity.setCreatorTime(DateUtil.getNowDate());
        monitorEntity.setCreatorUserId(userInfo.getUserId());
        monitorEntity.setContent(content);
        messageMonitorMapper.insert(monitorEntity);
        //消息推送 - PC端
        PushMessageUtil.pushMessage(map, userInfo, type != null ? type : 2);
    }


    /**
     * 数据存到redis中
     *
     * @param toUserIds 接受者id
     */
    private void executeInsert(List<String> toUserIds, List<String> idList) {
        List<String> key = new ArrayList<>();
        try {
            int frequency = 10000;
            int count = toUserIds.size() / frequency + 1;
            if (toUserIds.isEmpty()) return;
            for (int i = 0; i < count; i++) {
                // 生成redis的key
                String cacheKey = RandomUtil.uuId() + toUserIds.get(i);
                // 存到redis
                int endSize = Math.min(((i + 1) * frequency), toUserIds.size());
                redisUtil.insert(cacheKey, toUserIds.subList(i * frequency, endSize));
                key.add(cacheKey);
            }
        } catch (Exception e) {
            key.forEach(redisUtil::remove);
            key.clear();
            throw new DataException();
        }
        idList.addAll(key);
    }

    public void saveShortLink(String pcLink, String appLink, String shortLink, UserInfo userInfo, String userId, String bodyText) {
        ShortLinkEntity shortLinkEntity = shortLInkMapper.getInfoByLink(shortLink);
        if (shortLinkEntity == null) {
            ShortLinkEntity entity = new ShortLinkEntity();
            Map<String, String> sysConfig = getSystemConfig();
            String linkTime = sysConfig.get("linkTime");
            Integer isClick = 0;
            if (StringUtils.isNotBlank(sysConfig.get(KeyConst.IS_CLICK)) && !"null".equals(sysConfig.get(KeyConst.IS_CLICK))) {
                isClick = Integer.parseInt(sysConfig.get(KeyConst.IS_CLICK));
            }
            int unClickNum = 20;
            if (StringUtils.isNotBlank(sysConfig.get(KeyConst.UN_CLICK_NUM)) && !"null".equals(sysConfig.get(KeyConst.UN_CLICK_NUM))) {
                unClickNum = Integer.parseInt(sysConfig.get(KeyConst.UN_CLICK_NUM));
            }
            entity.setId(RandomUtil.uuId());
            entity.setRealPcLink(pcLink);
            entity.setRealAppLink(appLink);
            entity.setShortLink(shortLink);
            entity.setBodyText(bodyText);
            entity.setUserId(userId);
            entity.setIsUsed(isClick);
            entity.setUnableNum(unClickNum);
            entity.setClickNum(0);
            if (StringUtil.isNotEmpty(linkTime)) {
                Date unableTime = SentMessageUtil.getUnableTime(linkTime);
                entity.setUnableTime(unableTime);
            } else {
                entity.setUnableTime(DateUtil.dateAddHours(DateUtil.getNowDate(), 24));
            }
            entity.setCreatorTime(DateUtil.getNowDate());
            entity.setCreatorUserId(userInfo.getUserId());
            shortLInkMapper.insert(entity);
        }
    }

    private Map<String, String> getSystemConfig() {
        // 获取系统配置
        List<SysConfigEntity> configList = sysconfigService.getList("SysConfig");
        Map<String, String> objModel = new HashMap<>(16);
        for (SysConfigEntity entity : configList) {
            objModel.put(entity.getFkey(), entity.getValue());
        }
        return objModel;
    }

    private String getShortLink(String pcLink, String userId, String shortLink, String type) {
        if (StringUtils.isNotBlank(shortLink)) {
            ShortLinkEntity entity = shortLInkMapper.getInfoByLink(shortLink);
            if (entity != null) {
                if (pcLink.equals(entity.getRealPcLink())) {
                    return shortLink;
                } else {
                    shortLink = shortLInkMapper.shortLink(pcLink + userId + type);
                    return getShortLink(pcLink, userId, shortLink, type);
                }
            } else {
                return shortLink;
            }
        } else {
            shortLink = shortLInkMapper.shortLink(pcLink + userId + type);
            return getShortLink(pcLink, userId, shortLink, type);
        }
    }
}

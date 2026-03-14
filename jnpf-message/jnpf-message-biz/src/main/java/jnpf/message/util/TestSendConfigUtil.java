package jnpf.message.util;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import jnpf.base.SmsModel;
import jnpf.base.UserInfo;
import jnpf.base.service.SysconfigService;
import jnpf.constant.KeyConst;
import jnpf.constant.MsgCode;
import jnpf.constant.MsgSysParam;
import jnpf.exception.DataException;
import jnpf.message.entity.*;
import jnpf.message.enums.MessageTypeEnum;
import jnpf.message.model.WxgzhMessageModel;
import jnpf.message.model.message.EmailModel;
import jnpf.message.model.messagetemplateconfig.TemplateParamModel;
import jnpf.message.model.sendmessageconfig.SendConfigTemplateModel;
import jnpf.message.service.*;
import jnpf.model.SocialsSysConfig;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import jnpf.util.message.SmsUtil;
import jnpf.util.third.DingTalkUtil;
import jnpf.util.wxutil.mp.WXGZHWebChatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestSendConfigUtil {

    private final UserService userService;
    private final SysconfigService sysconfigService;
    private final MessageService messageService;
    private final SynThirdInfoService synThirdInfoService;
    private final MessageTemplateConfigService messageTemplateConfigService;
    private final SmsFieldService smsFieldService;
    private final AccountConfigService accountConfigService;
    private final MessageMonitorService messageMonitorService;
    private final WechatUserService wechatUserService;

    private static final String MSG_NO_USER = "接收人为空！";
    private static final String ERRCODE = "errcode";
    private static final String ERRMSG = "errmsg";

    /**
     * 测试发送配置
     */
    public String sendMessage(SendConfigTemplateModel model, UserInfo userInfo) {
        // 获取消息模板详情
        MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigService.getInfo(model.getTemplateId());
        List<String> toUserIdsList = model.getToUser();
        if (msgTemEntity == null || toUserIdsList == null) {
            throw new DataException("消息模板数据不存在！");
        }
        // 参数
        Map<String, Object> parameterMap = new HashMap<>();
        List<TemplateParamModel> paramModelList = JsonUtil.getJsonToList(model.getParamJson(), TemplateParamModel.class);
        if (paramModelList != null && !paramModelList.isEmpty()) {
            for (TemplateParamModel paramModel : paramModelList) {
                parameterMap.put(paramModel.getField(), paramModel.getValue());
            }
        }
        // 替换参数
        String content = msgTemEntity.getContent();
        if ("5".equals(msgTemEntity.getMessageSource()) && "1".equals(msgTemEntity.getMessageType())) {
            Map<String, Object> map = new HashMap<>();
            map.put("formDataId", parameterMap.get(MsgSysParam.FORM_DATA_ID));
            map.put("formTemplateId", parameterMap.get(MsgSysParam.FORM_TEMPLATE_ID));
            content = JsonUtil.getObjectToString(map);
        }
        // 替换参数
        if (StringUtil.isNotEmpty(content)) {
            StringSubstitutor strSubstitutor = new StringSubstitutor(parameterMap, "{", "}");
            content = strSubstitutor.replace(content);
        }
        MessageTypeEnum typeEnum = MessageTypeEnum.getByCode(msgTemEntity.getMessageType());
        String sendType = msgTemEntity.getMessageType();
        switch (typeEnum) {
            case SYS_MESSAGE:
                // 站内消息
                String title = msgTemEntity.getTitle();
                if (StringUtil.isNotEmpty(title)) {
                    StringSubstitutor strSubstitutor = new StringSubstitutor(parameterMap, "{", "}");
                    title = strSubstitutor.replace(title);
                }

                Integer source = Integer.parseInt(msgTemEntity.getMessageSource());
                Integer type = Integer.parseInt(msgTemEntity.getMessageType());
                messageService.sentMessage(toUserIdsList, title, content, userInfo, source, type, true);
                break;
            case SMS_MESSAGE:
                // 发送短信
                JSONObject jsonObject1 = sendSms(toUserIdsList, model, parameterMap);
                if (!jsonObject1.getBooleanValue(KeyConst.CODE)) {
                    return "发送短信消息失败，错误：" + jsonObject1.get(KeyConst.ERROR);
                }
                break;
            case MAIL_MESSAGE:
                // 邮件
                JSONObject jsonObject2 = sendMail(toUserIdsList, userInfo, sendType, model, parameterMap);
                if (!jsonObject2.getBooleanValue(KeyConst.CODE)) {
                    return "发送邮件消息失败，错误：" + jsonObject2.get(KeyConst.ERROR);
                }
                break;
            case QY_MESSAGE:
                // 企业微信
                JSONObject jsonObject3 = sendQyWebChat(toUserIdsList, userInfo, sendType, model, parameterMap);
                if (!jsonObject3.getBooleanValue(KeyConst.CODE)) {
                    return "发送企业微信消息失败，错误：" + jsonObject3.get(KeyConst.ERROR);
                }
                break;
            case DING_MESSAGE:
                // 钉钉
                JSONObject jsonObject4 = sendDingTalk(toUserIdsList, userInfo, sendType, model, parameterMap);
                if (!jsonObject4.getBooleanValue(KeyConst.CODE)) {
                    return "发送钉钉消息失败，错误：" + jsonObject4.get(KeyConst.ERROR);
                }
                break;
            case WEB_HOOK_MESSAGE:
                // webhook
                JSONObject jsonObject5 = sendWebHook(sendType, userInfo, model, parameterMap);
                if (!jsonObject5.getBooleanValue(KeyConst.CODE)) {
                    return "发送webhook消息失败，错误：" + jsonObject5.get(KeyConst.ERROR);
                }
                break;
            case WECHAT_MESSAGE:
                // 微信公众号
                JSONObject jsonObject6 = sendWXGzhChat(toUserIdsList, userInfo, sendType, model, parameterMap);
                if (!jsonObject6.getBooleanValue(KeyConst.CODE)) {
                    return "发送微信公众号消息失败，错误：" + jsonObject6.get(KeyConst.ERROR);
                }
                break;
            default:
                break;
        }
        return null;
    }

    private JSONObject sendWebHook(String sendType, UserInfo userInfo, SendConfigTemplateModel model, Map<String, Object> parameterMap) {
        MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigService.getInfo(model.getTemplateId());
        if (msgTemEntity == null) {
            log.info(sendType);
            throw new DataException("消息模板数据不存在！");
        }
        String content = msgTemEntity.getContent();
        JSONObject retJson = new JSONObject();
        MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
        monitorEntity.setId(RandomUtil.uuId());
        monitorEntity.setSendTime(DateUtil.getNowDate());
        monitorEntity.setCreatorTime(DateUtil.getNowDate());
        monitorEntity.setCreatorUserId(userInfo.getUserId());

        // 替换参数
        if (StringUtil.isNotEmpty(content)) {
            StringSubstitutor strSubstitutor = new StringSubstitutor(parameterMap, "{", "}");
            content = strSubstitutor.replace(content);
        }
        AccountConfigEntity accountEntity = accountConfigService.getInfo(model.getAccountConfigId());
        if (accountEntity == null) {
            throw new DataException("webhook账号地址配置错误配置数据不存在！");
        }
        //消息监控-消息模板写入
        monitorEntity.setMessageType(msgTemEntity.getMessageType());
        monitorEntity.setMessageTemplateId(msgTemEntity.getId());
        String title = msgTemEntity.getTitle();
        if (StringUtil.isNotEmpty(title)) {
            StringSubstitutor strSubstitutor = new StringSubstitutor(parameterMap, "{", "}");
            title = strSubstitutor.replace(title);
        }
        monitorEntity.setTitle(title);
        monitorEntity.setReceiveUser(accountEntity.getWebhookAddress());
        monitorEntity.setContent(content);
        monitorEntity.setMessageSource(msgTemEntity.getMessageSource());
        //消息监控-账号配置写入
        monitorEntity.setAccountId(accountEntity.getId());
        monitorEntity.setAccountCode(accountEntity.getEnCode());
        monitorEntity.setAccountName(accountEntity.getFullName());
        switch (accountEntity.getWebhookType()) {
            case 1:
                //钉钉
                if (Objects.equals(1, accountEntity.getApproveType())) {
                    JSONObject result = WebHookUtil.sendDDMessage(accountEntity.getWebhookAddress(), content);
                    messageMonitorService.create(monitorEntity);
                    if (ObjectUtil.isNotEmpty(result)) {
                        if (!"0".equals(result.get(ERRCODE).toString())) {
                            retJson.put(KeyConst.CODE, false);
                            retJson.put(KeyConst.ERROR, result.get(ERRMSG));
                            return retJson;
                        }
                    } else {
                        retJson.put(KeyConst.CODE, false);
                        retJson.put(KeyConst.ERROR, "webhook账号地址配置错误！");
                        return retJson;
                    }
                } else if (Objects.equals(2, accountEntity.getApproveType())) {
                    JSONObject result = WebHookUtil.sendDingDing(accountEntity.getWebhookAddress(), accountEntity.getBearer(), content);
                    messageMonitorService.create(monitorEntity);
                    if (ObjectUtil.isNotEmpty(result)) {
                        if (!"0".equals(result.get(ERRCODE).toString())) {
                            retJson.put(KeyConst.CODE, false);
                            retJson.put(KeyConst.ERROR, result.get(ERRMSG));
                            return retJson;
                        }
                    } else {
                        retJson.put(KeyConst.CODE, false);
                        retJson.put(KeyConst.ERROR, "webhook账号地址配置错误！");
                        return retJson;
                    }
                }
                break;
            case 2:
                if (Objects.equals(1, accountEntity.getApproveType())) {
                    JSONObject result = WebHookUtil.callWeChatBot(accountEntity.getWebhookAddress(), content);
                    messageMonitorService.create(monitorEntity);
                    if (!"0".equals(result.get(ERRCODE).toString())) {
                        retJson.put(KeyConst.CODE, false);
                        retJson.put(KeyConst.ERROR, result.get(ERRMSG));
                        return retJson;
                    }
                }
                break;
            default:
                break;
        }
        retJson.put(KeyConst.CODE, true);
        retJson.put(KeyConst.ERROR, MsgCode.SU012.get());
        return retJson;
    }

    /**
     * 发送企业微信消息
     *
     * @param toUserIdsList
     * @param userInfo
     * @param sendType
     * @param parameterMap
     * @return
     */
    private JSONObject sendQyWebChat(List<String> toUserIdsList, UserInfo userInfo, String sendType, SendConfigTemplateModel model, Map<String, Object> parameterMap) {
        JSONObject retJson = new JSONObject();
        boolean code = true;
        StringBuilder error = new StringBuilder();
        MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigService.getInfo(model.getTemplateId());
        if (msgTemEntity == null) {
            throw new DataException("消息模板数据不存在！");
        }
        //创建消息监控
        MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
        monitorEntity.setId(RandomUtil.uuId());
        monitorEntity.setSendTime(DateUtil.getNowDate());
        monitorEntity.setReceiveUser(JsonUtil.getObjectToString(toUserIdsList));
        monitorEntity.setCreatorTime(DateUtil.getNowDate());
        monitorEntity.setCreatorUserId(userInfo.getUserId());
        //消息监控-消息模板写入
        monitorEntity.setMessageType(msgTemEntity.getMessageType());
        monitorEntity.setMessageSource(msgTemEntity.getMessageSource());
        monitorEntity.setMessageTemplateId(msgTemEntity.getId());

        String content = msgTemEntity.getContent();
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
        monitorEntity.setTitle(title);
        monitorEntity.setContent(content);
        // 获取系统配置
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String corpId = config.getQyhCorpId();
        String agentId = config.getQyhAgentId();
        // 获取的应用的Secret值(某个修复导致俩个秘钥反了，只能反正修复了)
        String corpSecret = config.getQyhCorpSecret();
        // 相关参数验证
        if (StringUtil.isEmpty(corpId)) {
            throw new DataException("企业ID为空！");
        }
        if (StringUtil.isEmpty(corpSecret)) {
            throw new DataException("凭证密钥为空！");
        }
        if (StringUtil.isEmpty(agentId)) {
            throw new DataException("企业微信应用凭证为空！");
        }
        if (StringUtil.isEmpty(content)) {
            throw new DataException("内容为空！");
        }

        for (String userId : toUserIdsList) {
            UserEntity userEntity = userService.getInfo(userId);
            if (userEntity != null) {
                String wxUserId = "";
                StringBuilder toWxUserId = new StringBuilder();
                String toUserIdAll = "";
                StringBuilder nullUserInfo = new StringBuilder();

                // 创建消息实体
                MessageEntity messageEntity = JnpfMessageUtil.setMessageEntity(userInfo.getUserId(), content, null, Integer.parseInt(sendType));

                // 获取接收人员的企业微信号、创建消息用户实体
                wxUserId = "";
                // 从同步表获取对应的企业微信ID
                SynThirdInfoEntity synThirdInfoEntity = synThirdInfoService.getInfoBySysObjId("1", "2", userId);
                if (synThirdInfoEntity == null) {
                    synThirdInfoEntity = synThirdInfoService.getInfoBySysObjId("11", "2", userId);
                }
                if (synThirdInfoEntity != null) {
                    wxUserId = synThirdInfoEntity.getThirdObjId();
                }
                if (StringUtil.isEmpty(wxUserId)) {
                    nullUserInfo = nullUserInfo.append(",").append(userId);
                } else {
                    toWxUserId = toWxUserId.append("|").append(wxUserId);
                }
                JnpfMessageUtil.setMessageReceiveEntity(userId, title, Integer.valueOf(sendType));

                // 处理企业微信号信息串并验证
                toUserIdAll = toWxUserId.toString();
                if (StringUtil.isNotEmpty(toUserIdAll)) {
                    toUserIdAll = toUserIdAll.substring(1);
                }
                if (StringUtil.isEmpty(toUserIdAll)) {
                    code = false;
                    error = error.append("；").append(userEntity.getRealName()).append("：").append("接收人对应的企业微信号全部为空！");
                    messageMonitorService.create(monitorEntity);
                } else {
                    // 批量发送企业信息信息
                    retJson = QyWebChatUtil.sendWxMessage(corpId, corpSecret, agentId, toUserIdAll, content);
                    if (!retJson.getBooleanValue(KeyConst.CODE)) {
                        code = false;
                        error = error.append("；").append(userEntity.getRealName()).append("：").append(retJson.get(KeyConst.ERROR));
                        messageMonitorService.create(monitorEntity);
                    } else {
                        // 企业微信号为空的信息写入备注
                        if (StringUtil.isNotEmpty(nullUserInfo.toString())) {
                            messageEntity.setExcerpt(nullUserInfo.substring(1) + "对应的企业微信号为空");
                            messageMonitorService.create(monitorEntity);
                        }
                    }
                }
            } else {
                code = false;
                error = error.append("；").append("用户不存在！");
                messageMonitorService.create(monitorEntity);
            }
        }

        if (code) {
            retJson.put(KeyConst.CODE, true);
            retJson.put(KeyConst.ERROR, MsgCode.SU012.get());
        } else {
            String msg = error.toString();
            msg = msg.substring(1);
            retJson.put(KeyConst.CODE, false);
            retJson.put(KeyConst.ERROR, msg);
        }
        return retJson;
    }

    /**
     * List<String> toUserIdsList, UserInfo userInfo, String sendType, MessageTemplateEntity entity, Map<String, String> parameterMap
     *
     * @param toUserIdsList
     * @param userInfo
     * @param sendType
     * @param parameterMap
     * @return
     */
    private JSONObject sendDingTalk(List<String> toUserIdsList, UserInfo userInfo, String sendType, SendConfigTemplateModel model, Map<String, Object> parameterMap) {
        JSONObject retJson = new JSONObject();
        boolean code = true;
        StringBuilder error = new StringBuilder();
        //创建消息监控
        MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigService.getInfo(model.getTemplateId());
        if (msgTemEntity == null) {
            throw new DataException("消息模板数据不存在！");
        }
        MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
        monitorEntity.setId(RandomUtil.uuId());
        monitorEntity.setSendTime(DateUtil.getNowDate());
        monitorEntity.setReceiveUser(JsonUtil.getObjectToString(toUserIdsList));
        monitorEntity.setCreatorTime(DateUtil.getNowDate());
        monitorEntity.setCreatorUserId(userInfo.getUserId());
        //消息监控-消息模板写入
        monitorEntity.setMessageType(msgTemEntity.getMessageType());
        monitorEntity.setMessageSource(msgTemEntity.getMessageSource());
        monitorEntity.setMessageTemplateId(msgTemEntity.getId());

        String content = msgTemEntity.getContent();
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
        monitorEntity.setTitle(title);
        monitorEntity.setContent(content);
        // 获取系统配置
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String appKey = config.getDingSynAppKey();
        String appSecret = config.getDingSynAppSecret();
        String agentId = config.getDingAgentId();
        // 相关参数验证
        if (StringUtil.isEmpty(appKey)) {
            throw new DataException("AppKey为空！");
        }
        if (StringUtil.isEmpty(appSecret)) {
            throw new DataException("AppSecret为空！");
        }
        if (StringUtil.isEmpty(agentId)) {
            throw new DataException("AgentId为空！");
        }
        if (StringUtil.isEmpty(content)) {
            throw new DataException("内容为空！");
        }

        for (String userId : toUserIdsList) {
            UserEntity userEntity = userService.getInfo(userId);
            if (userEntity != null) {
                String dingUserId = "";
                StringBuilder toDingUserId = new StringBuilder();
                String toUserIdAll = "";
                StringBuilder nullUserInfo = new StringBuilder();
                // 创建消息实体
                MessageEntity messageEntity = JnpfMessageUtil.setMessageEntity(userInfo.getUserId(), content, null, Integer.parseInt(sendType));

                // 获取接收人员的钉钉号、创建消息用户实体
                dingUserId = "";
                // 从同步表获取对应用户的钉钉ID
                SynThirdInfoEntity synThirdInfoEntity = synThirdInfoService.getInfoBySysObjId("2", "2", userId);
                if (synThirdInfoEntity == null) {
                    synThirdInfoEntity = synThirdInfoService.getInfoBySysObjId("22", "2", userId);
                }
                if (synThirdInfoEntity != null) {
                    dingUserId = synThirdInfoEntity.getThirdObjId();
                }
                if (StringUtil.isEmpty(dingUserId)) {
                    nullUserInfo = nullUserInfo.append(",").append(userId);
                } else {
                    toDingUserId = toDingUserId.append(",").append(dingUserId);
                }
                JnpfMessageUtil.setMessageReceiveEntity(userId, title, Integer.valueOf(sendType));

                // 处理接收人员的钉钉号信息串并验证
                toUserIdAll = toDingUserId.toString();
                if (StringUtil.isNotEmpty(toUserIdAll)) {
                    toUserIdAll = toUserIdAll.substring(1);
                }
                if (StringUtil.isEmpty(toUserIdAll)) {
                    code = false;
                    error = error.append("；").append(userEntity.getRealName()).append("：").append("接收人对应的钉钉号为空！");
                    messageMonitorService.create(monitorEntity);
                } else {
                    // 批量发送钉钉信息
                    retJson = DingTalkUtil.sendDingMessage(appKey, appSecret, agentId, toUserIdAll, content);
                    if (!retJson.getBooleanValue(KeyConst.CODE)) {
                        code = false;
                        error = error.append("；").append(userEntity.getRealName()).append("：").append(retJson.get(KeyConst.ERROR));
                        messageMonitorService.create(monitorEntity);
                    } else {
                        // 钉钉号为空的信息写入备注
                        if (StringUtil.isNotEmpty(nullUserInfo.toString())) {
                            messageEntity.setExcerpt(nullUserInfo.toString().substring(1) + "对应的钉钉号为空！");
                            messageMonitorService.create(monitorEntity);
                        }
                    }
                }
            } else {
                code = false;
                error = error.append("；").append("用户不存在！");
                messageMonitorService.create(monitorEntity);
            }
        }

        if (code) {
            retJson.put(KeyConst.CODE, true);
            retJson.put(KeyConst.ERROR, MsgCode.SU012.get());
        } else {
            String msg = error.toString();
            msg = msg.substring(1);
            retJson.put(KeyConst.CODE, false);
            retJson.put(KeyConst.ERROR, msg);
        }
        return retJson;
    }

    /**
     * 发送邮件
     *
     * @param toUserIdsList
     * @param userInfo
     * @param sendType
     * @param parameterMap
     * @return
     */
    private JSONObject sendMail(List<String> toUserIdsList, UserInfo userInfo, String sendType, SendConfigTemplateModel model, Map<String, Object> parameterMap) {
        JSONObject retJson = new JSONObject();
        boolean code = true;
        StringBuilder error = new StringBuilder();
        //创建消息监控
        MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigService.getInfo(model.getTemplateId());
        if (msgTemEntity == null) {
            throw new DataException("消息模板数据不存在！");
        }
        AccountConfigEntity accountEntity = accountConfigService.getInfo(model.getAccountConfigId());
        if (accountEntity == null) {
            throw new DataException("webhook账号地址配置错误配置数据不存在！");
        }
        MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
        monitorEntity.setId(RandomUtil.uuId());
        monitorEntity.setSendTime(DateUtil.getNowDate());
        monitorEntity.setReceiveUser(JsonUtil.getObjectToString(toUserIdsList));
        monitorEntity.setCreatorTime(DateUtil.getNowDate());
        monitorEntity.setCreatorUserId(userInfo.getUserId());
        //消息监控-消息模板写入
        monitorEntity.setMessageType(msgTemEntity.getMessageType());
        monitorEntity.setMessageSource(msgTemEntity.getMessageSource());
        monitorEntity.setMessageTemplateId(msgTemEntity.getId());
        //消息监控-账号配置写入
        monitorEntity.setAccountId(accountEntity.getId());
        monitorEntity.setAccountCode(accountEntity.getEnCode());
        monitorEntity.setAccountName(accountEntity.getFullName());
        String content = msgTemEntity.getContent();
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
        monitorEntity.setTitle(title);
        monitorEntity.setContent(content);

        // 相关参数验证
        Map<String, String> objModel = new HashMap<>();
        objModel.put("emailSmtpHost", accountEntity.getSmtpServer());
        objModel.put("emailSmtpPort", accountEntity.getSmtpPort().toString());
        objModel.put("emailSenderName", accountEntity.getAddressorName());
        objModel.put("emailAccount", accountEntity.getSmtpUser());
        objModel.put("emailPassword", accountEntity.getSmtpPassword());
        objModel.put("emailSsl", accountEntity.getSslLink().equals(1) ? "true" : "false");

        EmailModel emailModel = JsonUtil.getJsonToBean(objModel, EmailModel.class);
        if (StringUtil.isEmpty(emailModel.getEmailSmtpHost())) {
            throw new DataException("SMTP服务为空！");
        }
        if (StringUtil.isEmpty(emailModel.getEmailSmtpPort())) {
            throw new DataException("SMTP端口为空！");
        }
        if (StringUtil.isEmpty(emailModel.getEmailAccount())) {
            throw new DataException("发件人邮箱为空！");
        }
        if (StringUtil.isEmpty(emailModel.getEmailPassword())) {
            throw new DataException("发件人密码为空！");
        }
        for (String userId : toUserIdsList) {
            UserEntity userEntity = userService.getInfo(userId);
            if (userEntity != null) {
                StringBuilder nullUserInfo = new StringBuilder();
                StringBuilder toUserMail = new StringBuilder();
                String userEmailAll = "";
                String userEmail = "";
                String userName = "";

                // 设置邮件标题
                emailModel.setEmailTitle(title);
                // 设置邮件内容
                emailModel.setEmailContent(content);
                // 创建消息实体
                MessageEntity messageEntity = JnpfMessageUtil.setMessageEntity(userInfo.getUserId(), emailModel.getEmailTitle(), emailModel.getEmailContent(), Integer.parseInt(sendType));
                // 获取收件人的邮箱地址、创建消息用户实体
                if (userEntity != null) {
                    userEmail = StringUtil.isEmpty(userEntity.getEmail()) ? "" : userEntity.getEmail();
                    userName = userEntity.getRealName();
                }
                if (userEmail != null && !"".equals(userEmail)) {
                    if (EmailUtil.isEmail(userEmail)) {
                        toUserMail = toUserMail.append(",").append(userName).append("<").append(userEmail).append(">");
                    }
                } else {
                    nullUserInfo = nullUserInfo.append(",").append(userId);
                }
                JnpfMessageUtil.setMessageReceiveEntity(userId, title, Integer.valueOf(sendType));

                // 处理接收人员的邮箱信息串并验证
                userEmailAll = toUserMail.toString();
                if (StringUtil.isNotEmpty(userEmailAll)) {
                    userEmailAll = userEmailAll.substring(1);
                }
                if (StringUtil.isEmpty(userEmailAll)) {
                    code = false;
                    error = error.append("；").append(userEntity.getRealName()).append("：").append(MSG_NO_USER);
                    messageMonitorService.create(monitorEntity);
                } else {
                    // 设置接收人员
                    emailModel.setEmailToUsers(userEmailAll);
                    // 发送邮件
                    retJson = EmailUtil.sendMail(emailModel);
                    if (!retJson.getBooleanValue(KeyConst.CODE)) {
                        code = false;
                        error = error.append("；").append(userEntity.getRealName()).append("：").append(retJson.get(KeyConst.ERROR));
                        messageMonitorService.create(monitorEntity);
                    } else {
                        // 邮箱地址为空的信息写入备注
                        if (StringUtil.isNotEmpty(nullUserInfo.toString())) {
                            messageEntity.setExcerpt(nullUserInfo.substring(1) + "对应的邮箱为空");
                            messageMonitorService.create(monitorEntity);
                        }
                    }
                }
            }
        }

        if (code) {
            retJson.put(KeyConst.CODE, true);
            retJson.put(KeyConst.ERROR, MsgCode.SU012.get());
        } else {
            String msg = error.toString();
            msg = msg.substring(1);
            retJson.put(KeyConst.CODE, false);
            retJson.put(KeyConst.ERROR, msg);
        }
        return retJson;
    }

    /**
     * 发送短信
     *
     * @param toUserIdsList
     * @param parameterMap
     * @return
     */
    private JSONObject sendSms(List<String> toUserIdsList, SendConfigTemplateModel model, Map<String, Object> parameterMap) {
        UserInfo userInfo = UserProvider.getUser();
        JSONObject retJson = new JSONObject();
        boolean code = true;
        StringBuilder error = new StringBuilder();

        //创建消息监控
        MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigService.getInfo(model.getTemplateId());
        if (msgTemEntity == null) {
            throw new DataException("消息模板数据不存在！");
        }
        AccountConfigEntity accountEntity = accountConfigService.getInfo(model.getAccountConfigId());
        if (accountEntity == null) {
            throw new DataException("webhook账号地址配置错误配置数据不存在！");
        }
        //账号配置——短信
        Map<String, String> objModel = new HashMap<>(16);
        objModel.put("aliAccessKey", accountEntity.getAppId());
        objModel.put("aliSecret", accountEntity.getAppSecret());
        objModel.put("tencentSecretId", accountEntity.getAppId());
        objModel.put("tencentSecretKey", accountEntity.getAppSecret());
        objModel.put("tencentAppId", accountEntity.getSdkAppId());
        objModel.put("tencentAppKey", accountEntity.getAppKey());
        SmsModel smsConfig = JsonUtil.getJsonToBean(objModel, SmsModel.class);
        Map<String, Object> smsMap = smsFieldService.getParamMap(msgTemEntity.getId(), parameterMap);

        String content = msgTemEntity.getContent();
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
        MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
        monitorEntity.setId(RandomUtil.uuId());
        monitorEntity.setSendTime(DateUtil.getNowDate());
        monitorEntity.setReceiveUser(JsonUtil.getObjectToString(toUserIdsList));
        monitorEntity.setCreatorTime(DateUtil.getNowDate());
        monitorEntity.setCreatorUserId(userInfo.getUserId());
        //消息监控-消息模板写入
        monitorEntity.setMessageType(msgTemEntity.getMessageType());
        monitorEntity.setMessageSource(msgTemEntity.getMessageSource());
        monitorEntity.setMessageTemplateId(msgTemEntity.getId());
        //消息监控-账号配置写入
        monitorEntity.setAccountId(accountEntity.getId());
        monitorEntity.setAccountCode(accountEntity.getEnCode());
        monitorEntity.setAccountName(accountEntity.getFullName());
        monitorEntity.setTitle(title);
        monitorEntity.setContent(content);

        for (String userId : toUserIdsList) {
            UserEntity userEntity = userService.getInfo(userId);
            if (userEntity != null) {
                int company = accountEntity.getChannel();
                // 组装接受用户
                StringBuilder toUserIdList = new StringBuilder();
                if (isPhone(userEntity.getMobilePhone())) {
                    toUserIdList.append(userEntity.getMobilePhone());
                    toUserIdList.append(",");
                }
                // 发送短信
                String endPoint = "";
                if (Objects.equals(1, accountEntity.getChannel())) {
                    endPoint = accountEntity.getEndPoint();
                } else if (Objects.equals(2, accountEntity.getChannel())) {
                    endPoint = accountEntity.getZoneName();
                }
                content = SmsUtil.querySmsTemplateContent(company, smsConfig, endPoint, accountEntity.getZoneParam(), msgTemEntity.getTemplateCode());
                if (StringUtils.isNotBlank(content) && !"null".equals(content)) {
                    if (Objects.equals(1, accountEntity.getChannel())) {
                        if (content.contains("${")) {
                            for (Map.Entry<String, Object> keyItem : smsMap.entrySet()) {
                                if (keyItem.getValue() != null && StringUtils.isNotBlank(keyItem.getValue().toString())) {
                                    content = content.replace("{" + keyItem.getKey() + "}", keyItem.getValue().toString());
                                }
                            }
                        }
                    } else if (Objects.equals(2, accountEntity.getChannel()) && content.contains("{")) {
                        for (Map.Entry<String, Object> keyItem : smsMap.entrySet()) {
                            if (keyItem.getValue() != null && StringUtils.isNotBlank(keyItem.getValue().toString())) {
                                content = content.replace("{" + keyItem.getKey() + "}", keyItem.getValue().toString());
                            }
                        }
                    }
                }
                monitorEntity.setContent(content);
                if (StringUtils.isEmpty(toUserIdList)) {
                    code = false;
                    error = error.append("；").append(userEntity.getRealName()).append("：").append("手机号码格式错误！");
                    messageMonitorService.create(monitorEntity);
                } else {
                    String result = SmsUtil.sentSms(company, smsConfig, endPoint, accountEntity.getZoneParam(), toUserIdList.toString(), accountEntity.getSmsSignature(), msgTemEntity.getTemplateCode(), smsMap);
                    if (!"Ok".equalsIgnoreCase(result)) {
                        code = false;
                        error = error.append("；").append(userEntity.getRealName()).append("：").append(result);
                        messageMonitorService.create(monitorEntity);
                    }
                }
            }
        }
        if (code) {
            retJson.put(KeyConst.CODE, true);
            retJson.put(KeyConst.ERROR, MsgCode.SU012.get());
        } else {
            String msg = error.toString();
            msg = msg.substring(1);
            retJson.put(KeyConst.CODE, false);
            retJson.put(KeyConst.ERROR, msg);
        }
        return retJson;
    }

    /**
     * 发送微信公众号消息
     *
     * @param toUserIdsList
     * @param userInfo
     * @param sendType
     * @param parameterMap
     * @return
     */
    public JSONObject sendWXGzhChat(List<String> toUserIdsList, UserInfo userInfo, String
            sendType, SendConfigTemplateModel model, Map<String, Object> parameterMap) {
        JSONObject retJson = new JSONObject();
        boolean code = true;
        StringBuilder error = new StringBuilder();
        //创建消息监控
        MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigService.getInfo(model.getTemplateId());
        if (msgTemEntity == null) {
            log.error(sendType);
            throw new DataException("消息模板数据不存在！");
        }
        AccountConfigEntity accountEntity = accountConfigService.getInfo(model.getAccountConfigId());
        if (accountEntity == null) {
            throw new DataException("账号配置数据不存在！");
        }
        MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
        monitorEntity.setId(RandomUtil.uuId());
        monitorEntity.setSendTime(DateUtil.getNowDate());
        monitorEntity.setReceiveUser(JsonUtil.getObjectToString(toUserIdsList));
        monitorEntity.setCreatorTime(DateUtil.getNowDate());
        monitorEntity.setCreatorUserId(userInfo.getUserId());
        //消息监控-消息模板写入
        monitorEntity.setMessageType(msgTemEntity.getMessageType());
        monitorEntity.setMessageSource(msgTemEntity.getMessageSource());
        monitorEntity.setMessageTemplateId(msgTemEntity.getId());
        //消息监控-账号配置写入
        monitorEntity.setAccountId(accountEntity.getId());
        monitorEntity.setAccountCode(accountEntity.getEnCode());
        monitorEntity.setAccountName(accountEntity.getFullName());
        String content = msgTemEntity.getContent();
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
        monitorEntity.setTitle(title);
        monitorEntity.setContent(content);
        //微信公众号参数
        Map<String, Object> smsMap = new HashMap<>();
        if (model != null) {
            smsMap = smsFieldService.getParamMap(model.getTemplateId(), parameterMap);
        }
        if (smsMap.containsKey(KeyConst.TITLE)) {
            title = smsMap.get(KeyConst.TITLE).toString();
            smsMap.keySet().removeIf(k -> k.equals(KeyConst.TITLE));
        }

        // 获取系统配置
        String appId = accountEntity.getAppId();
        String appsecret = accountEntity.getAppSecret();
        String wxxcxAppId = msgTemEntity.getXcxAppId();
        String templateKId = msgTemEntity.getTemplateCode();
        // 相关参数验证
        if (StringUtil.isEmpty(templateKId)) {
            throw new DataException("微信公众号模板id未创建！");
        }
        if (StringUtil.isEmpty(appId)) {
            throw new DataException("公众号appid为空为空！");
        }
        if (StringUtil.isEmpty(appsecret)) {
            throw new DataException("公众号appsecret为空为空！");
        }
        // 获取微信公众号的token
        String token = WXGZHWebChatUtil.getAccessToken(appId, appsecret);
        if (StringUtil.isEmpty(token)) {
            throw new DataException("获取微信公众号token失败！");
        }
        for (String userId : toUserIdsList) {
            UserEntity userEntity = userService.getById(userId);
            monitorEntity.setTitle(title);
            if (userEntity != null) {
                //获取用户在对应微信公众号上的openid
                WechatUserEntity wechatUserEntity = wechatUserService.getInfoByGzhId(userId, accountEntity.getAppKey());
                if (wechatUserEntity != null) {
                    if (StringUtils.isNotBlank(wechatUserEntity.getOpenId())) {
                        String openid = wechatUserEntity.getOpenId();
                        String pagepath = "pages/login/index?tag=1&flowId=";
                        //参数封装
                        String message = WXGZHWebChatUtil.messageJson(templateKId, openid, wxxcxAppId, pagepath, smsMap, "2", null);
                        //发送信息
                        retJson = WXGZHWebChatUtil.sendMessage(token, message);
                        JSONObject rstObj = WXGZHWebChatUtil.getMessageList(token);
                        List<WxgzhMessageModel> wxgzhMessageModelList = JsonUtil.getJsonToList(rstObj.get("template_list"), WxgzhMessageModel.class);
                        WxgzhMessageModel messageModel = wxgzhMessageModelList.stream().filter(t -> t.getTemplateId().equals(templateKId)).findFirst().orElse(null);
                        if (messageModel != null) {
                            content = messageModel.getContent();
                            if (StringUtils.isNotBlank(content) && !"null".equals(content) && ObjectUtil.isNotEmpty(smsMap) && content.contains(".DATA}")) {
                                for (Map.Entry<String, Object> keyItem : smsMap.entrySet()) {
                                    content = content.replace(keyItem.getKey(), keyItem.getValue().toString());
                                }

                            }
                        }
                        //创建消息监控
                        monitorEntity.setContent(content);
                        if (!retJson.getBooleanValue(KeyConst.CODE)) {
                            code = false;
                            error = error.append("；").append(userEntity.getRealName()).append("：").append(retJson.get(KeyConst.ERROR));
                            messageMonitorService.create(monitorEntity);
                            continue;
                        }
                        messageMonitorService.create(monitorEntity);
                    } else {
                        code = false;
                        error = error.append("；").append(userEntity.getRealName()).append("：").append("账号未绑定公众号！");
                        messageMonitorService.create(monitorEntity);
                    }
                } else {
                    code = false;
                    error = error.append("；").append(userEntity.getRealName()).append("：").append("账号未绑定公众号！");
                    messageMonitorService.create(monitorEntity);
                }
            }
        }
        if (code) {
            retJson.put(KeyConst.CODE, true);
            retJson.put(KeyConst.ERROR, MsgCode.SU012.get());
        } else {
            String msg = error.toString();
            msg = msg.substring(1);
            retJson.put(KeyConst.CODE, false);
            retJson.put(KeyConst.ERROR, msg);
        }
        return retJson;
    }

    public static boolean isPhone(String phone) {
        if (StringUtils.isNotBlank(phone) && !"null".equals(phone)) {
            return Pattern.matches("^1[3-9]\\d{9}$", phone);
        }
        return false;
    }
}

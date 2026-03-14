package jnpf.message.util;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import jnpf.base.SmsModel;
import jnpf.base.UserInfo;
import jnpf.base.entity.SysConfigEntity;
import jnpf.base.service.SysconfigService;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.KeyConst;
import jnpf.constant.MsgCode;
import jnpf.constant.MsgSysParam;
import jnpf.exception.DataException;
import jnpf.message.entity.*;
import jnpf.message.mapper.*;
import jnpf.message.model.WxgzhMessageModel;
import jnpf.message.model.message.DingTalkModel;
import jnpf.message.model.message.EmailModel;
import jnpf.message.service.SynThirdInfoService;
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

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 消息实体类
 *
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2021/4/22 9:06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SentMessageUtil {

    private final ConfigValueUtil configValueUtil;
    private final SysconfigService sysconfigService;
    private final UserService userApi;
    private final SynThirdInfoService synThirdInfoApi;
    private final AccountConfigMapper accountConfigMapper;
    private final MessageTemplateConfigMapper messageTemplateConfigMapper;
    private final MessageMonitorMapper messageMonitorMapper;
    private final SmsFieldMapper smsFieldMapper;
    private final ShortLInkMapper shortLInkMapper;
    private final WechatUserMapper wechatUserMapper;

    private static final String SHORT_LINK_URL = "/api/message/ShortLink/";
    private static final String WORK_FLOW_DETAIL = "/workFlowDetail?config=";
    private static final String FLOW_LINK_1 = "{@FlowLink}";
    private static final String FLOW_BEFORE = "/pages/workFlow/flowBefore/index?config=";

    private static final String MSG_NOUSER = "；用户不存在！";

    /**
     * 发送企业微信消息
     *
     * @param toUserIdsList
     * @param userInfo
     * @param sendType
     * @param entity
     * @param parameterMap
     * @return
     */
    public JSONObject sendQyWebChat(List<String> toUserIdsList, UserInfo userInfo, String sendType, SendConfigTemplateEntity entity, Map<String, Object> parameterMap, Map<String, String> contentMsg) {
        JSONObject retJson = new JSONObject();
        boolean code = true;
        StringBuilder error = new StringBuilder();

        MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigMapper.getInfo(entity.getTemplateId());
        if (msgTemEntity == null) {
            throw new DataException("消息模板数据不存在！");
        }
        // 获取系统配置
        SocialsSysConfig config = sysconfigService.getSocialsConfig();
        String corpId = config.getQyhCorpId();
        String agentId = config.getQyhAgentId();
        // 获取的应用的Secret值(某个修复导致俩个秘钥反了，只能反正修复了)
        String corpSecret = config.getQyhCorpSecret();
        String content = msgTemEntity.getContent();
        // 相关参数验证
        if (StringUtils.isEmpty(corpId)) {
            throw new DataException("企业ID为空！");
        }
        if (StringUtils.isEmpty(corpSecret)) {
            throw new DataException("Secret为空！");
        }
        if (StringUtils.isEmpty(agentId)) {
            throw new DataException("AgentId为空！");
        }
        if (StringUtils.isEmpty(content)) {
            throw new DataException("内容为空！");
        }

        // 获取接收人员的企业微信号、创建消息用户实体
        for (String userId : toUserIdsList) {
            error = new StringBuilder();
            MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
            monitorEntity.setId(RandomUtil.uuId());
            monitorEntity.setSendTime(DateUtil.getNowDate());
            monitorEntity.setCreatorTime(DateUtil.getNowDate());
            monitorEntity.setCreatorUserId(userInfo.getUserId());
            monitorEntity.setReceiveUser(userId);
            UserEntity userEntity = userApi.getInfo(userId);
            if (StringUtils.isEmpty(userId) || ObjectUtil.isEmpty(userEntity)) {
                code = false;
                error = error.append(MSG_NOUSER);
                messageMonitorMapper.create(monitorEntity);
            } else {
                //获取消息模板参数
                Map<String, Object> msgMap = getParamMap(entity.getId(), parameterMap);
                // 替换参数

                String msg = contentMsg.get(userId) != null ? contentMsg.get(userId) : "{}";
                byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
                String encode = Base64.getEncoder().encodeToString(bytes);
                //流程审批页面链接地址
                String pcLink = WORK_FLOW_DETAIL + encode;
                String appLink = FLOW_BEFORE + encode;
                //转换为短链
                String shortLink = shortLInkMapper.shortLink(pcLink + userId + msgTemEntity.getMessageType());
                shortLink = getShortLink(pcLink, userId, shortLink, msgTemEntity.getMessageType());
                String msgTC = msgTemEntity.getTitle() + msgTemEntity.getContent();
                if (StringUtils.isNotBlank(msgTC) && msgTC.contains(FLOW_LINK_1)) {
                    //链接数据保存
                    this.saveShortLink(pcLink, appLink, shortLink, userInfo, userId, msg);
                }
                String link = configValueUtil.getApiDomain() + SHORT_LINK_URL + shortLink;
                if (StringUtils.isNotBlank(userInfo.getTenantId())) {
                    link = link + "/" + userInfo.getTenantId();
                }
                if (StringUtil.isNotEmpty(content)) {
                    if (content.contains(FLOW_LINK_1)) {
                        content = content.replace(FLOW_LINK_1, link + " ");
                    }
                    StringSubstitutor strSubstitutor = new StringSubstitutor(msgMap, "{", "}");
                    content = strSubstitutor.replace(content);
                }

                // 替换参数
                String title = msgTemEntity.getTitle();
                if (StringUtil.isNotEmpty(title)) {
                    if (title.contains(FLOW_LINK_1)) {
                        title = title.replace(FLOW_LINK_1, link + " ");
                    }
                    StringSubstitutor strSubstitutor = new StringSubstitutor(msgMap, "{", "}");
                    title = strSubstitutor.replace(title);
                }
                title = systemParam(parameterMap, contentMsg, title, userInfo);
                content = systemParam(parameterMap, contentMsg, content, userInfo);
                monitorEntity.setTitle(title);
                monitorEntity.setContent(content);
                String wxUserId = "";
                StringBuilder toWxUserId = new StringBuilder();
                String toUserIdAll = "";
                StringBuilder nullUserInfo = new StringBuilder();
                // 创建消息实体
                MessageEntity messageEntity = JnpfMessageUtil.setMessageEntity(userInfo.getUserId(), content, null, Integer.parseInt(sendType));
                //创建消息监控
                monitorEntity = createMessageMonitor(monitorEntity, msgTemEntity, null, content, userInfo, null, title);
                // 获取接收人员的企业微信号、创建消息用户实体
                wxUserId = "";
                // 从同步表获取对应的企业微信ID
                SynThirdInfoEntity synThirdInfoEntity = synThirdInfoApi.getInfoBySysObjId("1", "2", userId);
                if (synThirdInfoEntity == null) {
                    synThirdInfoEntity = synThirdInfoApi.getInfoBySysObjId("11", "2", userId);
                }
                if (synThirdInfoEntity != null) {
                    wxUserId = synThirdInfoEntity.getThirdObjId();
                }
                if (StringUtils.isEmpty(wxUserId)) {
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
                if (StringUtils.isEmpty(toUserIdAll)) {
                    code = false;
                    error = error.append("；").append(userEntity.getRealName()).append("：").append("接收人对应的企业微信号全部为空！");
                    messageMonitorMapper.create(monitorEntity);
                } else {
                    // 发送企业信息信息
                    retJson = QyWebChatUtil.sendWxMessage(corpId, corpSecret, agentId, toUserIdAll, content);
                    if (!retJson.getBooleanValue(KeyConst.CODE)) {
                        code = false;
                        error = error.append("；").append(userEntity.getRealName()).append("：").append(retJson.get(KeyConst.ERROR));
                        messageMonitorMapper.create(monitorEntity);
                    } else {
                        // 企业微信号为空的信息写入备注
                        if (StringUtil.isNotEmpty(nullUserInfo.toString())) {
                            messageEntity.setExcerpt(nullUserInfo.substring(1) + "对应的企业微信号为空");
                        }
                        messageMonitorMapper.create(monitorEntity);
                    }
                }
            }
        }
        if (code) {
            retJson.put(KeyConst.CODE, true);
            retJson.put(KeyConst.ERROR, MsgCode.SU012.get());
        } else {
            String msg = error.toString();
            if (StringUtils.isNotBlank(msg)) {
                msg = msg.substring(1);
            }
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
     * @param entity
     * @param parameterMap
     * @return
     */
    public JSONObject sendDingTalk(List<String> toUserIdsList, UserInfo userInfo, String sendType, SendConfigTemplateEntity entity, Map<String, Object> parameterMap, Map<String, String> contentMsg) {
        boolean code = true;
        StringBuilder error = new StringBuilder();
        JSONObject retJson = new JSONObject();

        MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigMapper.getInfo(entity.getTemplateId());
        if (msgTemEntity == null) {
            throw new DataException("消息模板数据不存在！");
        }
        // 获取系统配置
        String content = msgTemEntity.getContent();

        Map<String, String> objModel = getSystemConfig();
        DingTalkModel dingTalkModel = JsonUtil.getJsonToBean(objModel, DingTalkModel.class);
        String appKey = dingTalkModel.getDingSynAppKey();
        String appSecret = dingTalkModel.getDingSynAppSecret();
        String agentId = dingTalkModel.getDingAgentId();

        // 相关参数验证
        if (StringUtils.isEmpty(appKey)) {
            throw new DataException("AppKey为空！");
        }
        if (StringUtils.isEmpty(appSecret)) {
            throw new DataException("AppSecret为空！");
        }
        if (StringUtils.isEmpty(agentId)) {
            throw new DataException("agentId为空！");
        }
        if (StringUtils.isEmpty(content)) {
            throw new DataException("内容为空！");
        }
        for (String userId : toUserIdsList) {
            error = new StringBuilder();
            //消息监控
            MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
            monitorEntity.setId(RandomUtil.uuId());
            monitorEntity.setSendTime(DateUtil.getNowDate());
            monitorEntity.setCreatorTime(DateUtil.getNowDate());
            monitorEntity.setCreatorUserId(userInfo.getUserId());
            monitorEntity.setReceiveUser(userId);
            UserEntity userEntity = userApi.getInfo(userId);
            if (StringUtils.isEmpty(userId) || ObjectUtil.isEmpty(userEntity)) {
                code = false;
                error = error.append(MSG_NOUSER);
                messageMonitorMapper.create(monitorEntity);
            } else {
                //获取消息模板参数
                Map<String, Object> msgMap = getParamMap(entity.getId(), parameterMap);
                //转换链接
                String msg = contentMsg.get(userId) != null ? contentMsg.get(userId) : "{}";
                byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
                String encode = Base64.getEncoder().encodeToString(bytes);
                //流程审批页面链接地址
                String pcLink = WORK_FLOW_DETAIL + encode;
                String appLink = FLOW_BEFORE + encode;
                //转换为短链
                String shortLink = shortLInkMapper.shortLink(pcLink + userId + msgTemEntity.getMessageType());
                shortLink = getShortLink(pcLink, userId, shortLink, msgTemEntity.getMessageType());
                String msgTC = msgTemEntity.getTitle() + msgTemEntity.getContent();
                if (StringUtils.isNotBlank(msgTC) && msgTC.contains(FLOW_LINK_1)) {
                    //链接数据保存
                    this.saveShortLink(pcLink, appLink, shortLink, userInfo, userId, msg);
                }
                String link = configValueUtil.getApiDomain() + SHORT_LINK_URL + shortLink;
                if (StringUtils.isNotBlank(userInfo.getTenantId())) {
                    link = link + "/" + userInfo.getTenantId();
                }
                if (StringUtil.isNotEmpty(content)) {
                    if (content.contains(FLOW_LINK_1)) {
                        content = content.replace(FLOW_LINK_1, link + " ");
                    }
                    StringSubstitutor strSubstitutor = new StringSubstitutor(msgMap, "{", "}");
                    content = strSubstitutor.replace(content);
                }
                // 替换参数
                String title = msgTemEntity.getTitle();
                if (StringUtil.isNotEmpty(title)) {
                    if (title.contains(FLOW_LINK_1)) {
                        title = title.replace(FLOW_LINK_1, link + " ");
                    }
                    StringSubstitutor strSubstitutor = new StringSubstitutor(msgMap, "{", "}");
                    title = strSubstitutor.replace(title);
                }

                title = systemParam(parameterMap, contentMsg, title, userInfo);
                content = systemParam(parameterMap, contentMsg, content, userInfo);
                monitorEntity.setTitle(title);
                monitorEntity.setContent(content);
                String dingUserId = "";
                StringBuilder toDingUserId = new StringBuilder();
                String toUserIdAll = "";
                StringBuilder nullUserInfo = new StringBuilder();
                // 创建消息实体
                MessageEntity messageEntity = JnpfMessageUtil.setMessageEntity(userInfo.getUserId(), content, null, Integer.parseInt(sendType));
                //创建消息监控
                monitorEntity = createMessageMonitor(monitorEntity, msgTemEntity, null, content, userInfo, null, title);
                // 获取接收人员的钉钉号、创建消息用户实体
                dingUserId = "";
                // 从同步表获取对应用户的钉钉ID
                SynThirdInfoEntity synThirdInfoEntity = synThirdInfoApi.getInfoBySysObjId("2", "2", userId);
                if (synThirdInfoEntity == null) {
                    synThirdInfoEntity = synThirdInfoApi.getInfoBySysObjId("22", "2", userId);
                }
                if (synThirdInfoEntity != null) {
                    dingUserId = synThirdInfoEntity.getThirdObjId();
                }
                if (StringUtils.isEmpty(dingUserId)) {
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
                if (StringUtils.isEmpty(toUserIdAll)) {
                    code = false;
                    error = error.append("；").append(userEntity.getRealName()).append("：").append("接收人对应的钉钉号为空！");
                    messageMonitorMapper.create(monitorEntity);
                } else {
                    // 发送钉钉信息
                    retJson = DingTalkUtil.sendDingMessage(appKey, appSecret, agentId, toUserIdAll, content);
                    if (!retJson.getBooleanValue(KeyConst.CODE)) {
                        code = false;
                        error = error.append("；").append(userEntity.getRealName()).append("：").append(retJson.get(KeyConst.ERROR));
                        messageMonitorMapper.create(monitorEntity);
                    } else {
                        // 钉钉号为空的信息写入备注
                        if (StringUtil.isNotEmpty(nullUserInfo.toString())) {
                            messageEntity.setExcerpt(nullUserInfo.toString().substring(1) + "对应的钉钉号为空");
                        }
                        messageMonitorMapper.create(monitorEntity);
                    }
                }
            }
        }
        if (code) {
            retJson.put(KeyConst.CODE, true);
            retJson.put(KeyConst.ERROR, MsgCode.SU012.get());
        } else {
            String msg = error.toString();
            if (StringUtils.isNotBlank(msg)) {
                msg = msg.substring(1);
            }
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
     * @param entity
     * @param parameterMap
     * @return
     */
    public List<String> sendMail(List<String> toUserIdsList, UserInfo userInfo, String sendType, SendConfigTemplateEntity entity, Map<String, Object> parameterMap, Map<String, String> contentMsg) {
        List<String> errList = new ArrayList<>();
        MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigMapper.getInfo(entity.getTemplateId());
        if (msgTemEntity == null) {
            throw new DataException("消息模板数据不存在！");
        }
        AccountConfigEntity accountEntity = accountConfigMapper.getInfo(entity.getAccountConfigId());
        if (accountEntity == null) {
            throw new DataException("邮箱配置不存在！");
        }
        // 获取系统配置
        Map<String, String> objModel = new HashMap<>();
        objModel.put("emailSmtpHost", accountEntity.getSmtpServer());
        objModel.put("emailSmtpPort", accountEntity.getSmtpPort().toString());
        objModel.put("emailSenderName", accountEntity.getAddressorName());
        objModel.put("emailAccount", accountEntity.getSmtpUser());
        objModel.put("emailPassword", accountEntity.getSmtpPassword());
        objModel.put("emailSsl", accountEntity.getSslLink().equals(1) ? "true" : "false");

        EmailModel emailModel = JsonUtil.getJsonToBean(objModel, EmailModel.class);
        if (StringUtils.isEmpty(emailModel.getEmailSmtpHost())) {
            throw new DataException("SMTP服务为空！");
        }
        if (StringUtils.isEmpty(emailModel.getEmailSmtpPort())) {
            throw new DataException("SMTP端口为空！");
        }
        if (StringUtils.isEmpty(emailModel.getEmailAccount())) {
            throw new DataException("发件人邮箱为空！");
        }
        if (StringUtils.isEmpty(emailModel.getEmailPassword())) {
            throw new DataException("发件人密码为空！");
        }

        for (String userId : toUserIdsList) {
            //消息监控
            MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
            monitorEntity.setId(RandomUtil.uuId());
            monitorEntity.setReceiveUser(JsonUtil.getObjectToString(toUserIdsList));
            monitorEntity.setSendTime(DateUtil.getNowDate());
            monitorEntity.setCreatorTime(DateUtil.getNowDate());
            monitorEntity.setCreatorUserId(userInfo.getUserId());
            monitorEntity.setReceiveUser(userId);
            UserEntity userEntity = userApi.getInfo(userId);
            if (StringUtils.isEmpty(userId) || userEntity == null) {
                log.error("接收人为空");
                messageMonitorMapper.create(monitorEntity);
                errList.add("接收人为空");
            } else {
                String msg = contentMsg.get(userId) != null ? contentMsg.get(userId) : "{}";
                byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
                String encode = Base64.getEncoder().encodeToString(bytes);
                //流程审批页面链接地址
                String pcLink = WORK_FLOW_DETAIL + encode;
                String appLink = FLOW_BEFORE + encode;
                //转换为短链
                String shortLink = shortLInkMapper.shortLink(pcLink + userId + msgTemEntity.getMessageType());
                shortLink = getShortLink(pcLink, userId, shortLink, msgTemEntity.getMessageType());
                String msgTC = msgTemEntity.getTitle() + msgTemEntity.getContent();
                if (StringUtils.isNotBlank(msgTC) && msgTC.contains(FLOW_LINK_1)) {
                    //链接数据保存
                    this.saveShortLink(pcLink, appLink, shortLink, userInfo, userId, msg);
                }
                String link = configValueUtil.getApiDomain() + SHORT_LINK_URL + shortLink;
                if (StringUtils.isNotBlank(userInfo.getTenantId())) {
                    link = link + "/" + userInfo.getTenantId();
                }
                Map<String, Object> msgMap = getParamMap(entity.getId(), parameterMap);
                // 设置邮件标题
                String title = msgTemEntity.getTitle();
                if (title.contains(FLOW_LINK_1)) {
                    title = title.replace(FLOW_LINK_1, link + " ");
                }
                if (StringUtil.isNotEmpty(title)) {
                    StringSubstitutor strSubstitutor = new StringSubstitutor(msgMap, "{", "}");
                    title = strSubstitutor.replace(title);
                }
                // 设置邮件内容
                String content = msgTemEntity.getContent();
                if (content.contains(FLOW_LINK_1)) {
                    content = content.replace(FLOW_LINK_1, link + " ");
                }
                //获取消息模板参数
                if (StringUtil.isNotEmpty(content)) {
                    StringSubstitutor strSubstitutor = new StringSubstitutor(msgMap, "{", "}");
                    content = strSubstitutor.replace(content);
                }
                title = systemParam(parameterMap, contentMsg, title, userInfo);
                content = systemParam(parameterMap, contentMsg, content, userInfo);
                monitorEntity.setTitle(title);
                monitorEntity.setContent(content);
                if (accountEntity != null) {
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
                    MessageEntity messageEntity = JnpfMessageUtil.setMessageEntity(userInfo.getUserId(), title, emailModel.getEmailContent(), Integer.parseInt(sendType));
                    //创建消息监控
                    monitorEntity = createMessageMonitor(monitorEntity, msgTemEntity, accountEntity, content, userInfo, null, title);
                    // 获取收件人的邮箱地址、创建消息用户实体
                    if (userEntity != null) {
                        userEmail = StringUtils.isEmpty(userEntity.getEmail()) ? "" : userEntity.getEmail();
                        userName = userEntity.getRealName();
                    }
                    if (userEmail != null && !"".equals(userEmail)) {
                        if (EmailUtil.isEmail(userEmail)) {
                            toUserMail = toUserMail.append(",").append(userName).append("<").append(userEmail).append(">");
                        }
                    } else {
                        nullUserInfo = nullUserInfo.append(",").append(userId);
                    }
                    JnpfMessageUtil.setMessageReceiveEntity(userId, title, Integer.parseInt(sendType));
                    // 处理接收人员的邮箱信息串并验证
                    userEmailAll = toUserMail.toString();
                    if (StringUtil.isNotEmpty(userEmailAll)) {
                        userEmailAll = userEmailAll.substring(1);
                    }
                    if (StringUtils.isEmpty(userEmailAll)) {
                        log.error("接收人对应的邮箱格式错误");
                        messageMonitorMapper.create(monitorEntity);
                        errList.add("接收人对应的邮箱格式错误");
                    } else {
                        // 设置接收人员
                        emailModel.setEmailToUsers(userEmailAll);
                        // 发送邮件
                        JSONObject retJson = EmailUtil.sendMail(emailModel);
                        messageMonitorMapper.create(monitorEntity);
                        if (!retJson.getBooleanValue(KeyConst.CODE)) {
                            log.error("发送失败");
                            errList.add("发送失败");
                        } else {
                            // 邮箱地址为空的信息写入备注
                            if (StringUtil.isNotEmpty(nullUserInfo.toString())) {
                                messageEntity.setExcerpt(nullUserInfo.substring(1) + "对应的邮箱为空");
                            }
                        }
                    }
                }
            }
        }
        return errList;
    }

    /**
     * 发送短信
     *
     * @param toUserIdsList
     * @param entity
     * @param parameterMap
     * @return
     */
    public List<String> sendSms(List<String> toUserIdsList, UserInfo userInfo, SendConfigTemplateEntity entity, Map<String, Object> parameterMap, Map<String, String> contentMsg) {
        List<String> errList = new ArrayList<>();
        //获取短信配置
        AccountConfigEntity accountEntity = accountConfigMapper.getInfo(entity.getAccountConfigId());
        if (accountEntity == null) {
            throw new DataException("短信配置不存在！");
        }
        // 获取消息模板详情
        MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigMapper.getInfo(entity.getTemplateId());
        if (msgTemEntity == null) {
            throw new DataException("消息模板数据不存在！");
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

        for (String toUserId : toUserIdsList) {
            //消息监控
            MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
            monitorEntity.setId(RandomUtil.uuId());
            monitorEntity.setSendTime(DateUtil.getNowDate());
            monitorEntity.setCreatorTime(DateUtil.getNowDate());
            monitorEntity.setCreatorUserId(userInfo.getUserId());
            monitorEntity.setReceiveUser(toUserId);
            String msg = contentMsg.get(toUserId) != null ? contentMsg.get(toUserId) : "{}";
            byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
            String encode = Base64.getEncoder().encodeToString(bytes);
            //流程审批页面链接地址
            String pcLink = WORK_FLOW_DETAIL + encode;
            String appLink = FLOW_BEFORE + encode;
            //转换为短链
            String shortLink = shortLInkMapper.shortLink(pcLink + toUserId + msgTemEntity.getMessageType());
            shortLink = getShortLink(pcLink, toUserId, shortLink, msgTemEntity.getMessageType());
            //发送给用户的链接
            String link = configValueUtil.getApiDomain() + SHORT_LINK_URL + shortLink;
            if (StringUtils.isNotBlank(userInfo.getTenantId())) {
                link = link + "/" + userInfo.getTenantId();
            }
            //转换为短链
            monitorEntity.setAccountId(accountEntity.getId());
            int company = accountEntity.getChannel();
            // 组装接受用户
            StringBuilder toUserIdList = new StringBuilder();
            UserEntity userEntity = userApi.getInfo(toUserId);
            if (isPhone(userEntity.getMobilePhone())) {
                toUserIdList.append(userEntity.getMobilePhone());
                toUserIdList.append(",");
            }

            //获取消息模板参数
            Map<String, Object> msgMap = getParamMap(entity.getId(), parameterMap);

            //短信参数
            Map<String, Object> smsMap = new HashMap<>();
            if (entity != null) {
                smsMap = smsFieldMapper.getParamMap(entity.getTemplateId(), msgMap);
                if (ObjectUtil.isNotEmpty(smsMap) && smsMap.containsValue(MsgSysParam.FLOW_LINK)) {
                    //链接数据保存
                    this.saveShortLink(pcLink, appLink, shortLink, userInfo, toUserId, msg);
                    for (Map.Entry<String, Object> keyItem : smsMap.entrySet()) {
                        if (keyItem.getValue() != null && MsgSysParam.FLOW_LINK.equals(keyItem.getValue().toString())) {
                            smsMap.put(keyItem.getKey(), link + " ");
                        }
                    }
                }
            }
            if (msgTemEntity != null) {
                monitorEntity.setMessageTemplateId(msgTemEntity.getId());
                String endPoint = "";
                if (Objects.equals(1, accountEntity.getChannel())) {
                    endPoint = accountEntity.getEndPoint();
                } else if (Objects.equals(2, accountEntity.getChannel())) {
                    endPoint = accountEntity.getZoneName();
                }
                String content = SmsUtil.querySmsTemplateContent(company, smsConfig, endPoint, accountEntity.getZoneParam(), msgTemEntity.getTemplateCode());
                if (StringUtils.isNotBlank(content) && !"null".equals(content) && ObjectUtil.isNotEmpty(smsMap)) {
                    if (Objects.equals(1, accountEntity.getChannel())) {
                        if (content.contains("${")) {
                            for (Map.Entry<String, Object> keyItem : smsMap.entrySet()) {
                                content = content.replace("${" + keyItem.getKey() + "}", keyItem.getValue().toString());
                            }
                        }
                    } else if (Objects.equals(2, accountEntity.getChannel()) && content.contains("{")) {
                        for (Map.Entry<String, Object> keyItem : smsMap.entrySet()) {
                            content = content.replace("{" + keyItem.getKey() + "}", keyItem.getValue().toString());
                        }
                    }
                }
                //创建消息监控
                monitorEntity = createMessageMonitor(monitorEntity, msgTemEntity, accountEntity, content, userInfo, null, null);
                if (StringUtils.isEmpty(toUserIdList)) {
                    log.error("全部接收人对应的手机号码格式错误");
                    messageMonitorMapper.create(monitorEntity);
                    errList.add("全部接收人对应的手机号码格式错误");
                } else {
                    SmsUtil.sentSms(company, smsConfig, endPoint, accountEntity.getZoneParam(), toUserIdList.toString(), accountEntity.getSmsSignature(), msgTemEntity.getTemplateCode(), smsMap);
                    messageMonitorMapper.create(monitorEntity);
                }
            }
        }
        return errList;
    }


    public JSONObject sendWXGzhChat(List<String> toUserIdsList, UserInfo userInfo, SendConfigTemplateEntity entity, Map<String, String> contentMsg, Map<String, Object> parameterMap) {
        //消息监控
        JSONObject retJson = new JSONObject();
        boolean code = true;
        StringBuilder error = new StringBuilder();
        //获取短信配置
        AccountConfigEntity accountEntity = accountConfigMapper.getInfo(entity.getAccountConfigId());
        if (ObjectUtil.isEmpty(accountEntity)) {
            throw new DataException("公众号账号配置数据不存在！");
        }
        // 获取消息模板详情
        MessageTemplateConfigEntity msgTemEntity = messageTemplateConfigMapper.getInfo(entity.getTemplateId());
        if (ObjectUtil.isEmpty(msgTemEntity)) {
            throw new DataException("消息模板数据不存在！");
        }
        // 相关参数验证
        // 获取系统配置
        String appId = accountEntity.getAppId();
        String appsecret = accountEntity.getAppSecret();
        String wxxcxAppId = msgTemEntity.getXcxAppId();
        String type = msgTemEntity.getWxSkip();
        String templateKId = msgTemEntity.getTemplateCode();
        String content = msgTemEntity.getContent();
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
        //获取消息模板参数
        Map<String, Object> msgMap = SentMessageUtil.getParamMap(entity.getId(), parameterMap);
        //微信公众号参数
        Map<String, Object> smsMap = new HashMap<>();
        if (entity != null) {
            smsMap = smsFieldMapper.getParamMap(entity.getTemplateId(), msgMap);
        }
        String title = "";
        if (smsMap.containsKey(KeyConst.TITLE)) {
            title = smsMap.get(KeyConst.TITLE).toString();
            smsMap.keySet().removeIf(k -> k.equals(KeyConst.TITLE));
        }
        if (smsMap.isEmpty()) {
            throw new DataException("公众号模板参数为空！");
        }

        for (String userId : toUserIdsList) {
            MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
            monitorEntity.setId(RandomUtil.uuId());
            monitorEntity.setSendTime(DateUtil.getNowDate());
            monitorEntity.setCreatorTime(DateUtil.getNowDate());
            monitorEntity.setCreatorUserId(userInfo.getUserId());
            error = new StringBuilder();
            UserEntity userEntity = userApi.getById(userId);
            if (StringUtil.isEmpty(userId) || ObjectUtil.isEmpty(userApi.getById(userId))) {
                code = false;
                error = error.append(MSG_NOUSER);
                messageMonitorMapper.create(monitorEntity);
            } else {
                monitorEntity.setReceiveUser(userId);
                monitorEntity.setMessageTemplateId(msgTemEntity.getId());
                monitorEntity.setAccountId(accountEntity.getId());
                //创建消息监控
                monitorEntity = SentMessageUtil.createMessageMonitor(monitorEntity, msgTemEntity, accountEntity, content, userInfo, toUserIdsList, null);
                monitorEntity.setTitle(title);

                // 微信公众号发送消息
                String msg = contentMsg.get(userId) != null ? contentMsg.get(userId) : "{}";
                byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
                String encode = Base64.getEncoder().encodeToString(bytes);
                //流程审批页面链接地址
                String pcLink = WORK_FLOW_DETAIL + encode;
                String appLink = FLOW_BEFORE + encode;
                //转换为短链
                String shortLink = shortLInkMapper.shortLink(pcLink + userId + msgTemEntity.getMessageType());
                shortLink = getShortLink(pcLink, userId, shortLink, msgTemEntity.getMessageType());
                if (!"1".equals(type)) {
                    //链接数据保存
                    this.saveShortLink(pcLink, appLink, shortLink, userInfo, userId, msg);
                }
                String link = configValueUtil.getApiDomain() + SHORT_LINK_URL + shortLink;
                if (StringUtils.isNotBlank(userInfo.getTenantId())) {
                    link = link + "/" + userInfo.getTenantId();
                }
                if (ObjectUtil.isNotEmpty(smsMap)) {
                    for (Map.Entry<String, Object> keyItem : smsMap.entrySet()) {
                        if (keyItem.getValue() != null && MsgSysParam.FLOW_LINK.equals(keyItem.getValue().toString())) {
                            smsMap.put(keyItem.getKey(), link);
                        }
                    }
                }
                WechatUserEntity wechatUserEntity = wechatUserMapper.getInfoByGzhId(userId, accountEntity.getAppKey());
                if (wechatUserEntity != null) {
                    if (StringUtils.isNotBlank(wechatUserEntity.getOpenId())) {
                        String openid = wechatUserEntity.getOpenId();
                        String apptoken = AuthUtil.loginTempUser(userId, userInfo.getTenantId());
                        String pagepath = FLOW_BEFORE + encode + "&token=" + apptoken;
                        if (ObjectUtil.isNotEmpty(smsMap)) {
                            //参数封装
                            String message = WXGZHWebChatUtil.messageJson(templateKId, openid, wxxcxAppId, pagepath, smsMap, type, link);
                            //发送信息
                            retJson = WXGZHWebChatUtil.sendMessage(token, message);
                        }
                        JSONObject rstObj = WXGZHWebChatUtil.getMessageList(token);
                        List<WxgzhMessageModel> wxgzhMessageModelList = JsonUtil.getJsonToList(rstObj.get("template_list"), WxgzhMessageModel.class);
                        WxgzhMessageModel messageModel = wxgzhMessageModelList.stream().filter(t -> t.getTemplateId().equals(templateKId)).findFirst().orElse(null);
                        if (messageModel != null) {
                            content = messageModel.getContent();
                            if (StringUtils.isNotBlank(content) && !"null".equals(content)
                                    && ObjectUtil.isNotEmpty(smsMap) && content.contains(".DATA}")) {
                                for (Map.Entry<String, Object> keyItem : smsMap.entrySet()) {
                                    content = content.replace(keyItem.getKey(), keyItem.getValue().toString());
                                }
                            }
                        }
                        if (!retJson.getBooleanValue(KeyConst.CODE)) {
                            code = false;
                            error = error.append("；").append(userEntity.getRealName()).append("：").append(retJson.get(KeyConst.ERROR));
                            messageMonitorMapper.create(monitorEntity);
                        } else {
                            messageMonitorMapper.create(monitorEntity);
                        }
                    } else {
                        code = false;
                        error = error.append("；").append(userEntity.getRealName()).append("：").append("账号未绑定公众号");
                        messageMonitorMapper.create(monitorEntity);
                    }
                } else {
                    code = false;
                    error = error.append("；").append(userEntity.getRealName()).append("：").append("账号未绑定公众号");
                    messageMonitorMapper.create(monitorEntity);
                }
            }
        }

        if (code) {
            retJson.put(KeyConst.CODE, true);
            retJson.put(KeyConst.ERROR, MsgCode.SU012.get());
        } else {
            String msg = error.toString();
            if (StringUtils.isNotBlank(msg)) {
                msg = msg.substring(1);
            }
            retJson.put(KeyConst.CODE, false);
            retJson.put(KeyConst.ERROR, msg);
        }
        return retJson;
    }

    /**
     * 获取系统配置
     */
    private Map<String, String> getSystemConfig() {
        // 获取系统配置
        List<SysConfigEntity> configList = sysconfigService.getList("SysConfig");
        Map<String, String> objModel = new HashMap<>(16);
        for (SysConfigEntity entity : configList) {
            objModel.put(entity.getFkey(), entity.getValue());
        }
        return objModel;
    }

    public static Map<String, Object> getParamMap(String templateId, Map<String, Object> paramMap) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, Object> paramItem : paramMap.entrySet()) {
            if (paramItem.getKey().contains(templateId)) {
                map.put(paramItem.getKey().substring(templateId.length()), paramItem.getValue());
            }
        }
        return map;
    }

    public static MessageMonitorEntity createMessageMonitor(MessageMonitorEntity monitorEntity, MessageTemplateConfigEntity msgTemEntity, AccountConfigEntity accountEntity, String content, UserInfo userInfo, List<String> toUserIdsList, String title) {
        if (msgTemEntity != null) {
            monitorEntity.setMessageTemplateId(msgTemEntity.getId());
            monitorEntity.setMessageSource(msgTemEntity.getMessageSource());
            if (StringUtils.isNotBlank(title)) {
                monitorEntity.setTitle(title);
            } else {
                monitorEntity.setTitle(msgTemEntity.getTitle());
            }
            monitorEntity.setMessageType(msgTemEntity.getMessageType());
            if ("6".equals(msgTemEntity.getMessageType()) && accountEntity != null) {
                monitorEntity.setReceiveUser(accountEntity.getWebhookAddress());
            } else {
                if (toUserIdsList != null && !toUserIdsList.isEmpty()) {
                    monitorEntity.setReceiveUser(JsonUtil.getObjectToString(toUserIdsList));
                }
            }
        } else {
            if (StringUtils.isNotBlank(title)) {
                monitorEntity.setTitle(title);
            }
            monitorEntity.setMessageType("1");
        }
        if (accountEntity != null) {
            monitorEntity.setAccountId(accountEntity.getId());
            monitorEntity.setAccountCode(accountEntity.getEnCode());
            monitorEntity.setAccountName(accountEntity.getFullName());
        }
        monitorEntity.setContent(content);
        log.info(userInfo.getUserId()+"获取消息监控数据");
        return monitorEntity;
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
                Date unableTime = getUnableTime(linkTime);
                entity.setUnableTime(unableTime);
            } else {
                entity.setUnableTime(DateUtil.dateAddHours(DateUtil.getNowDate(), 24));
            }
            entity.setCreatorTime(DateUtil.getNowDate());
            entity.setCreatorUserId(userInfo.getUserId());
            shortLInkMapper.insert(entity);
        }
    }

    public static Date getUnableTime(String linkTime) {
        Double time = Double.parseDouble(linkTime);
        int second = time.intValue() * 60 * 60;
        return DateUtil.dateAddSeconds(DateUtil.getNowDate(), second);
    }

    public static boolean isPhone(String phone) {
        if (StringUtils.isNotBlank(phone) && !"null".equals(phone)) {
            return Pattern.matches("^1[3-9]\\d{9}$", phone);
        }
        return false;
    }

    /**
     * 系统参数替换
     *
     * @param parameterMap
     * @param contentMsg
     * @param title
     * @param userInfo
     * @return
     */
    public static String systemParam(Map<String, Object> parameterMap, Map<String, String> contentMsg, String title, UserInfo userInfo) {
        if (parameterMap.isEmpty()) {
            return title.replace("\\{@Title}", contentMsg.get("Title") != null ? contentMsg.get("Title") : "")
                    .replace("\\{@CreatorUserName}", userInfo.getUserName())
                    .replace("\\{@SendTime}", DateUtil.getNow().substring(11))
                    .replace("\\{@Content}", contentMsg.get("Content") != null ? contentMsg.get("Content") : "")
                    .replace("\\{@Remark}", contentMsg.get("Remark") != null ? contentMsg.get("Remark") : "")
                    .replace("\\{@StartDate}", contentMsg.get("StartDate") != null ? contentMsg.get("StartDate") : "")
                    .replace("\\{@StartTime}", contentMsg.get("StartTime") != null ? contentMsg.get("StartTime") : "")
                    .replace("\\{@EndDate}", contentMsg.get("EndDate") != null ? contentMsg.get("EndDate") : "")
                    .replace("\\{@FlowLink}", "")
                    .replace("\\{@EndTime}", contentMsg.get("EndTime") != null ? contentMsg.get("EndTime") : "");
        }
        return title;
    }

}

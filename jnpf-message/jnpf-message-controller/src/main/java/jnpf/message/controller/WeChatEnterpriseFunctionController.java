package jnpf.message.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.GlobalConst;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.message.entity.AccountConfigEntity;
import jnpf.message.service.AccountConfigService;
import jnpf.socials.config.CustomAuthConfig;
import jnpf.socials.config.SocialsConfig;
import jnpf.socials.utils.AuthSocialsUtil;
import jnpf.util.NoDataSourceBind;
import jnpf.util.StringUtil;
import jnpf.util.XSSEscape;
import jnpf.util.wxutil.mp.WXGZHWebChatUtil;
import jnpf.util.wxutil.mp.aes.WXBizMsgCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 企业微信服务商事件处理
 * </p>
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2025/6/5 15:40:14
 */
@Tag(name = "企业微信事件处理", description = "WeChatEnterpriseOpen")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/message/weopen")
@Slf4j
public class WeChatEnterpriseFunctionController {

    private final AccountConfigService accountConfigService;
    private final ConfigValueUtil configValueUtil;
    private final SocialsConfig socialsConfig;

    /**
     * 企业微信服务商消息接收 官网登录使用
     *
     * @param request
     * @return
     * @throws Exception
     */
    @NoDataSourceBind
    @RequestMapping(value = "/white/loginreceive", method = {RequestMethod.GET, RequestMethod.POST})
    public String loginreceive(HttpServletRequest request, String loginType
            , @RequestParam(value = "type", required = false) String type) throws Exception {
        //获取企业微信账号配置
        CustomAuthConfig config = socialsConfig.getSocialMap().get("wechat_enterprise_ww");
        if (config == null) {
            log.info("未找到企业微信商户配置");
            return "";
        }
        String appId;
        String token;
        String encodingAesKey;
        if (Objects.equals("qrcode", loginType)) {
            appId = config.getClientId();
            token = config.getClientToken();
            encodingAesKey = config.getClientEncodingAesKey();
        } else if (Objects.equals("web", loginType)) {
            appId = config.getAppClientId();
            token = config.getAppClientToken();
            encodingAesKey = config.getAppClientEncodingAesKey();
        } else {
            log.info("企业微信商户配置不正确");
            return "";
        }
        // 回调地址检测传的都是企业ID
        boolean isCheckUrl = "get".equalsIgnoreCase(request.getMethod());
        // 数据回调接口传的是企业ID， 指令回调接口传的是APPID
        boolean isDataRequest = "data".equalsIgnoreCase(type);
        if (isCheckUrl || isDataRequest) {
            appId = config.getCorpId();
        }
        return parseEvent(request, appId, token, encodingAesKey);
    }


    /**
     * 企业微信服务商消息接收
     *
     * @param request
     * @param enCode   微信公账号账号编码
     * @param type     接口回调模式, commond: 指令回调, data: 数据回调(消息事件)
     * @param tenantId
     * @return
     * @throws Exception
     */
    @NoDataSourceBind
    @RequestMapping(value = "/white/receive", method = {RequestMethod.GET, RequestMethod.POST})
    public String receive(HttpServletRequest request,
                          @RequestParam(value = "enCode") String enCode,
                          @RequestParam(value = "type", required = false) String type,
                          @RequestParam(value = "tenantId", required = false) String tenantId) throws Exception {
        if (configValueUtil.isMultiTenancy() && StringUtil.isNotEmpty(tenantId)) {
            TenantDataSourceUtil.switchTenant(tenantId);
        }
        //获取企业微信账号配置
        AccountConfigEntity accountConfigEntity = accountConfigService.getInfoByEnCode(enCode, "7");
        if (ObjectUtil.isEmpty(accountConfigEntity)) {
            log.info("未找到与编码相对应的微信公众号配置");
            return "";
        }
        // 回调地址检测传的都是企业ID
        boolean isCheckUrl = "get".equalsIgnoreCase(request.getMethod());
        // 数据回调接口传的是企业ID， 指令回调接口传的是APPID
        boolean isDataRequest = "data".equalsIgnoreCase(type);
        String appId = isDataRequest || isCheckUrl ? accountConfigEntity.getAppKey() : accountConfigEntity.getAppId();
        return parseEvent(request, appId, accountConfigEntity.getAgentId(), accountConfigEntity.getBearer());
    }

    private String parseEvent(HttpServletRequest request, String appId, String token, String encodingAesKey) throws IOException {
        boolean isCheckUrl = "get".equalsIgnoreCase(request.getMethod());
        String signature = request.getParameter("signature");
        if (StringUtil.isEmpty(signature)) {
            signature = XSSEscape.escape(request.getParameter("msg_signature"));
        }
        String echostr = XSSEscape.escape(request.getParameter("echostr"));
        if (StringUtil.isEmpty(echostr)) {
            echostr = IoUtil.read(request.getInputStream(), GlobalConst.DEFAULT_CHARSET);
        }
        String timestamp = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");

        String result;
        try {
            if (isCheckUrl) {
                WXBizMsgCrypt crypt = new WXBizMsgCrypt(token, encodingAesKey, appId);
                result = crypt.verifyUrl(signature, timestamp, nonce, echostr);
                if (log.isDebugEnabled()) {
                    log.debug("企业微信接口检测：{}", result);
                }
            } else {
                WXBizMsgCrypt crypt = new WXBizMsgCrypt(token, encodingAesKey, appId);
                result = crypt.decryptMsg(signature, timestamp, nonce, echostr);
                if (log.isDebugEnabled()) {
                    log.debug("企业微信接口消息：{}", result);
                }
                Map<String, Object> resultMap = WXGZHWebChatUtil.xmlToMap(result);
                result = handlerEvent(resultMap);
            }
        } catch (Exception e) {
            log.info("企业微信回调失败：{}", e.getMessage());
            result = "";
        }
        return result;
    }

    private String handlerEvent(Map<String, Object> resultMap) {
        String event = (String) resultMap.get("InfoType");
        if (Objects.equals("suite_ticket", event)) {
            String suitId = (String) resultMap.get("SuiteId");
            String suitTicket = (String) resultMap.get("SuiteTicket");
            AuthSocialsUtil.setSuitTicket(suitId, suitTicket);
            if (log.isDebugEnabled()) {
                log.debug("服务商SuitTicket刷新： suitId:{}, suitTicket:{}", suitId, suitTicket);
            }
        }
        return "success";
    }


}

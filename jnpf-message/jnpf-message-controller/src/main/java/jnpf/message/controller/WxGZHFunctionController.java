package jnpf.message.controller;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jnpf.message.entity.AccountConfigEntity;
import jnpf.message.entity.WechatUserEntity;
import jnpf.message.service.AccountConfigService;
import jnpf.message.service.WechatUserService;
import jnpf.permission.entity.SocialsUserEntity;
import jnpf.permission.service.SocialsUserService;
import jnpf.util.DateUtil;
import jnpf.util.RandomUtil;
import jnpf.util.XSSEscape;
import jnpf.util.wxutil.mp.WXGZHWebChatUtil;
import jnpf.util.wxutil.mp.aes.WXBizMsgCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 发送消息模型
 */
@Tag(name = "微信公众号事件接收", description = "WechatOpen")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/message/WechatOpen")
@Slf4j
public class WxGZHFunctionController {

    private final AccountConfigService accountConfigService;
    private final SocialsUserService socialsUserService;
    private final WechatUserService wechatUserService;

    private static final String ERR_MSG = "微信公众号未绑定系统账号，请登录小程序绑定";
    private static final String UNIONID = "unionid";

    /**
     * 服务器基本配置链接微信公众号验证
     *
     * @param request  请求对象
     * @param response 响应对象
     * @return
     */
    @Operation(summary = "服务器基本配置链接微信公众号验证")
    @Parameter(name = "enCode", description = "微信公众号账号配置编码", required = true)
    @GetMapping("/token/{enCode}")
    public String token(@PathVariable("enCode") String enCode, HttpServletRequest request, HttpServletResponse response) throws Exception {
        //获取微信公众号账号配置
        AccountConfigEntity accountConfigEntity = accountConfigService.getInfoByEnCode(enCode, "7");
        if (ObjectUtil.isEmpty(accountConfigEntity)) {
            log.info("未找到与编码相对应的微信公众号配置");
            return "";
        }
        //微信公众号服务器配置token
        String wxToken = accountConfigEntity.getAgentId();
        String signature = request.getParameter("signature");
        String echostr = XSSEscape.escape(request.getParameter("echostr"));
        String timestamp = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");

        String sortStr = WXGZHWebChatUtil.sort(wxToken, timestamp, nonce);
        String mySinStr = WXGZHWebChatUtil.shal(sortStr);
        if (StringUtils.isNotBlank(signature) && mySinStr.equals(signature)) {
            return echostr;
        } else {
            log.info("微信公众号链接失败");
            return echostr;
        }
    }


    /**
     * 微信公众号事件请求
     *
     * @param request  请求对象
     * @param response 响应对象
     * @return
     * @throws Exception
     */
    @Operation(summary = "微信公众号事件请求")
    @PostMapping("/token/{enCode}")
    /**
     * 微信公众号事件请求
     */
    public String tokenPost(@PathVariable("enCode") String enCode, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("微信公众号请求事件");
        //获取微信公众号账号配置
        AccountConfigEntity accountConfigEntity = accountConfigService.getInfoByEnCode(enCode, "7");
        if (ObjectUtil.isEmpty(accountConfigEntity)) {
            log.info("未找到与编码相对应的微信公众号配置");
            return "";
        }
        //微信公众号服务器配置token
        String wxToken = accountConfigEntity.getAgentId();
        //微信公众号服务器配置EncodingAesKey
        String encodingAesKey = accountConfigEntity.getBearer();
        //微信公众号AppId
        String wxAppId = accountConfigEntity.getAppId();

        // 获取系统配置
        String msgSignature = request.getParameter("msg_signature");
        String encryptType = request.getParameter("encrypt_type");
        String signature = request.getParameter("signature");
        String echostr = XSSEscape.escape(request.getParameter("echostr"));
        String timestamp = request.getParameter("timestamp");
        String nonce = request.getParameter("nonce");

        String sortStr = WXGZHWebChatUtil.sort(wxToken, timestamp, nonce);
        String mySinStr = WXGZHWebChatUtil.shal(sortStr);
        //验签
        if (StringUtils.isNotBlank(signature) && mySinStr.equals(signature)) {
            //事件信息
            Map<String, Object> map = WXGZHWebChatUtil.parseXml(request);
            //事件信息
            String event = String.valueOf(map.get("Event"));
            String openid = String.valueOf(map.get("FromUserName"));
            //公众号原始id
            String gzhId = String.valueOf(map.get("ToUserName"));
            if ("aes".equals(encryptType)) {
                WXBizMsgCrypt pc = new WXBizMsgCrypt(wxToken, encodingAesKey, wxAppId);
                String encrypt = String.valueOf(map.get("Encrypt"));
                String format = "<xml><ToUserName><![CDATA[toUser]]></ToUserName><Encrypt><![CDATA[%1$s]]></Encrypt></xml>";
                String fromXML = String.format(format, encrypt);
                // 获取解密后消息明文
                String result = pc.decryptMsg(msgSignature, timestamp, nonce, fromXML);

                Map<String, Object> resultMap = WXGZHWebChatUtil.xmlToMap(result);
                // 获取解密后事件信息
                event = String.valueOf(resultMap.get("Event"));
                openid = String.valueOf(resultMap.get("FromUserName"));
                gzhId = String.valueOf(resultMap.get("ToUserName"));
            }

            String appId = accountConfigEntity.getAppId();
            String appsecret = accountConfigEntity.getAppSecret();
            String token = WXGZHWebChatUtil.getAccessToken(appId, appsecret);
            if ("subscribe".equals(event)) {
                //用户关注事件
                if (StringUtils.isNotBlank(token)) {
                    return webUserHandle(token, openid, gzhId, 1);
                } else {
                    log.error("微信公众号token错误，请查看配置");
                    return "";
                }
            } else if ("unsubscribe".equals(event)) {
                //用户取消关注事件
                if (StringUtils.isNotBlank(token)) {
                    return webUserHandle(token, openid, gzhId, 0);
                } else {
                    log.error("微信公众号token错误，请查看配置");
                    return "";
                }
            } else {
                return "";
            }
        } else {
            log.info("微信公众号事件请求失败");
            return echostr;
        }
    }

    private String webUserHandle(String token, String openid, String gzhId, int targetCloseMark) {
        JSONObject rstObj = WXGZHWebChatUtil.getUsetInfo(token, openid);
        if (!rstObj.containsKey(UNIONID)) {
            log.info(ERR_MSG);
            return "";
        }

        String unionid = rstObj.getString(UNIONID);
        SocialsUserEntity socialsUserEntity = socialsUserService.getInfoBySocialId(unionid, "wechat_open");
        if (socialsUserEntity == null) {
            log.info(ERR_MSG);
            return "";
        }

        WechatUserEntity wechatUserEntity = wechatUserService.getInfoByGzhId(socialsUserEntity.getUserId(), gzhId);
        if (wechatUserEntity == null) {
            WechatUserEntity entity = new WechatUserEntity();
            entity.setId(RandomUtil.uuId());
            entity.setUserId(socialsUserEntity.getUserId());
            entity.setGzhId(gzhId);
            entity.setCloseMark(targetCloseMark);
            entity.setCreatorTime(DateUtil.getNowDate());
            entity.setOpenId(openid);
            wechatUserService.create(entity);
        } else {
            if (wechatUserEntity.getCloseMark() != targetCloseMark) {
                wechatUserEntity.setCloseMark(targetCloseMark);
            }
            wechatUserEntity.setOpenId(openid);
            wechatUserEntity.setLastModifyTime(DateUtil.getNowDate());
            wechatUserService.update(wechatUserEntity.getId(), wechatUserEntity);
        }

        return "";
    }
}

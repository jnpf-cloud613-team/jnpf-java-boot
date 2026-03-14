package jnpf.granter;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaFoxUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.servlet.http.HttpServletResponse;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.constant.MsgCode;
import jnpf.consts.AuthConsts;
import jnpf.consts.LoginTicketStatus;
import jnpf.exception.LoginException;
import jnpf.model.BaseSystemInfo;
import jnpf.model.LoginTicketModel;
import jnpf.model.LoginVO;
import jnpf.model.SocialUnbindModel;
import jnpf.permission.controller.SocialsUserController;
import jnpf.permission.model.socails.SocialsUserInfo;
import jnpf.util.ServletUtil;
import jnpf.util.StringUtil;
import jnpf.util.TicketUtil;
import jnpf.util.UserProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Objects;

import static jnpf.granter.SocialsTokenGranter.GRANT_TYPE;


@Slf4j
@Component(GRANT_TYPE)
public class SocialsTokenGranter extends AbstractTokenGranter {


    public static final String GRANT_TYPE = "socials";
    public static final Integer ORDER = 5;
    private static final String URL_LOGIN = "/Login/socials/**";


    private SocialsUserController socialsUserApi;
    @Autowired
    public void setSocialsUserApi(SocialsUserController socialsUserApi) {
        this.socialsUserApi = socialsUserApi;
    }

    public SocialsTokenGranter() {
        super(URL_LOGIN);
    }

    protected String getGrantType() {
        return GRANT_TYPE;
    }

    public ActionResult granter(Map<String, String> map) throws LoginException {
        SaRequest req = SaHolder.getRequest();
        String code = req.getParam("code");
        String state = req.getParam("state");
        String source = req.getParam("source");
        String uuid = req.getParam("uuid");
        if (StringUtil.isEmpty(code)) {
            code = req.getParam("authCode") != null ? req.getParam("authCode") : req.getParam("auth_code");
        }
        //是否是微信qq唤醒或者小程序登录
        if (StringUtil.isNotEmpty(uuid)) {
            try {
                return loginByCode(source, code, null, uuid, null);
            } catch (Exception e) {
                //更新登录结果
                outError(e.getMessage());
            }
        }

        //租户列表登陆标识
        boolean tenantLogin = "true".equalsIgnoreCase(req.getParam("tenantLogin"));
        if (tenantLogin) {
            //租户多绑定选择登录
            if (configValueUtil.isMultiTenancy()) {
                LoginVO loginVO = tenantLogin(req);
                return ActionResult.success(MsgCode.OA015.get(), loginVO);
            }
        } else if (StringUtil.isEmpty(req.getParam(AuthConsts.PARAMS_JNPF_TICKET))) {
            //个人中心绑定
            if (SaFoxUtil.isNotEmpty(code)) {
                socialsBinding(req, code, state, source);
            }
        } else {
            //票据登陆
            if (!isValidJnpfTicket()) {
                outError(MsgCode.OA016.get());
                return null;
            }
            //接受CODE 进行登录
            if (SaFoxUtil.isNotEmpty(code)) {
                try {
                    String socialName = req.getParam("socialName");
                    ActionResult<Object> actionResult = loginByCode(source, code, state, null, socialName);
                    if (400 == actionResult.getCode() || "wechat_applets".equals(req.getParam("source"))) {
                        return actionResult;
                    }
                    return null;
                } catch (Exception e) {
                    //更新登录结果
                    outError(e.getMessage());
                }
                return null;
            }
        }
        return null;
    }

    /**
     * 租户列表登录
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/9/21
     */
    private LoginVO tenantLogin(SaRequest req) throws LoginException {
        String ticket = getJnpfTicket();
        if (StringUtil.isEmpty(ticket)) {
            throw new LoginException(MsgCode.OA016.get());
        }
        LoginTicketModel loginTicketModel = TicketUtil.parseTicket(ticket);
        if (loginTicketModel == null) {
            throw new LoginException(MsgCode.OA016.get());
        }
        TicketUtil.deleteTicket(ticket);
        JSONArray tenantArr = JSON.parseArray(loginTicketModel.getValue());
        String id = req.getParam("id");
        JSONObject tenantInfo = null;
        for (int i = 0; i < tenantArr.size(); i++) {
            JSONObject o = tenantArr.getJSONObject(i);
            if (Objects.equals(id, o.getString("id"))) {
                tenantInfo = o;
                break;
            }
        }
        if (tenantInfo != null) {
            String userId = tenantInfo.getString("userId");
            String account = tenantInfo.getString("account");
            String tenantId = tenantInfo.getString("tenantId");
            UserInfo userInfo = UserProvider.getLocalLoginUser();
            userInfo.setUserId(userId);
            userInfo.setTenantId(tenantId);
            if (Objects.equals(configValueUtil.getMultiTenancyVersion(), 1)) {
                userInfo.setUserAccount(tenantId + "@" + account);
            } else {
                userInfo.setUserAccount(account + "@" + tenantId);
            }
            //切换租户
            switchTenant(userInfo);
            //获取系统配置
            BaseSystemInfo baseSystemInfo = getSysconfig(userInfo);
            //登录账号
            super.loginAccount(userInfo, baseSystemInfo);
            //返回登录信息
            return getLoginVo(userInfo);
        }
        throw new LoginException(MsgCode.OA016.get());
    }

    /**
     * 第三方绑定
     */
    private void socialsBinding(SaRequest req, String code, String state, String source) {
        String userId = req.getParam("userId");
        String tenantId = req.getParam("tenantId");
        try {
            HttpServletResponse response = ServletUtil.getResponse();
            response.setCharacterEncoding("utf-8");
            response.setHeader("content-type", "text/html;charset=utf-8");
            PrintWriter out = response.getWriter();
            JSONObject binding = socialsUserApi.binding(source, userId, tenantId, code, state);
            out.print(
                    "<script>\n" +
                            "window.opener.postMessage(\'" + binding.toJSONString() + "\', '*');\n" +
                            "window.open('','_self','');\n" +
                            "window.close();\n" + "</script>");
        } catch (Exception e) {
            log.error("socialsBinding", e);
        }
    }


    public int getOrder() {
        return ORDER;
    }

    protected void outError(String message) {
        updateTicketError(message);
    }

    @Override
    protected String getUserDetailKey() {
        return AuthConsts.USERDETAIL_USER_ID;
    }

    protected LoginTicketModel getTicketUnbind(String socialType, String socialUnionid, String socialName) {
        LoginTicketModel loginTicketModel = null;
        SocialUnbindModel obj = new SocialUnbindModel(socialType, socialUnionid, socialName);
        String ticket = this.getJnpfTicket();
        if (!ticket.isEmpty()) {
            loginTicketModel = (new LoginTicketModel()).setStatus(LoginTicketStatus.UN_BIND.getStatus()).setValue(JSON.toJSONString(obj));
        }
        return loginTicketModel;
    }

    protected LoginTicketModel getTicketMultitenancy(JSONArray jsonArray) {
        LoginTicketModel loginTicketModel = null;
        String ticket = this.getJnpfTicket();
        if (!ticket.isEmpty()) {
            loginTicketModel = (new LoginTicketModel()).setStatus(LoginTicketStatus.MULTITENANCY.getStatus()).setValue(jsonArray.toJSONString());
        }
        return loginTicketModel;
    }

    protected LoginTicketModel getTicketSuccessReturn(UserInfo userInfo) {
        LoginTicketModel loginTicketModel = null;
        String ticket = getJnpfTicket();
        if (!ticket.isEmpty()) {
            loginTicketModel = new LoginTicketModel()
                    .setStatus(LoginTicketStatus.SUCCESS.getStatus())
                    .setValue(StpUtil.getTokenValueNotCut())
                    .setTheme(userInfo.getTheme());
            TicketUtil.updateTicket(ticket, loginTicketModel, null);
        }
        return loginTicketModel;
    }

    protected LoginTicketModel updateTicket(LoginTicketModel loginTicketModel) {
        String ticket = getJnpfTicket();
        if (!ticket.isEmpty()) {
            boolean needClose = false;
            String userAgent = ServletUtil.getUserAgent();
            if (userAgent.contains("wxwork")) {
                needClose = true;
            }
            if (needClose) {
                // 浏览器中扫码， 选择客户端打开， 需要把客户端打开的页面关闭
                try {
                    HttpServletResponse response = ServletUtil.getResponse();
                    PrintWriter out = response.getWriter();
                    response.setCharacterEncoding("utf-8");
                    response.setHeader("content-type", "text/html;charset=utf-8");
                    out.print(
                            "<script>\n" +
                                    "window.close();\n" + "</script>");
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            TicketUtil.updateTicket(ticket, loginTicketModel, null);
        }
        return loginTicketModel;
    }

    protected LoginVO getLoginVo(UserInfo userInfo) {
        LoginVO loginVO = new LoginVO();
        loginVO.setTheme(userInfo.getTheme());
        loginVO.setToken(userInfo.getToken());
        return loginVO;
    }

    /**
     * 小程序登录微信授权
     * app微信，qq唤醒
     *
     * @param code
     * @throws LoginException
     */
    protected ActionResult<Object> loginByCode(String source, String code, String state, String uuid, String socialName) throws LoginException {
        log.debug("Auth2 Code: {}", code);
        SocialsUserInfo socialsUserInfo = null;
        if (StringUtil.isNotEmpty(code)) {
            socialsUserInfo = socialsUserApi.getSocialsUserInfo(source, code, state);
        } else if (StringUtil.isNotEmpty(uuid)) {//微信和qq唤醒
            socialsUserInfo = socialsUserApi.getUserInfo(source, uuid, state);
            if (StringUtil.isEmpty(socialsUserInfo.getSocialName()) && StringUtil.isNotEmpty(socialName)) {
                socialsUserInfo.setSocialName(socialName);//小程序名称前端传递
            }
        }
        if (configValueUtil.isMultiTenancy()) {
            if (socialsUserInfo == null || CollUtil.isEmpty(socialsUserInfo.getTenantUserInfo())) {
                //第三方未绑定账号!
                LoginTicketModel ticketModel = getTicketUnbind(source, socialsUserInfo.getSocialUnionid(), socialsUserInfo.getSocialName());
                updateTicket(ticketModel);
                return ActionResult.fail(MsgCode.OA017.get());
            }
            if (socialsUserInfo.getTenantUserInfo().size() == 1) {
                UserInfo userInfo = socialsUserInfo.getUserInfo();
                if (Objects.equals(configValueUtil.getMultiTenancyVersion(), 2)) {
                    String[] split = userInfo.getUserAccount().split("@");
                    userInfo.setUserAccount(split[1] + "@" + split[0]);
                }
                //切换租户
                switchTenant(userInfo);
                //获取系统配置
                BaseSystemInfo baseSystemInfo = getSysconfig(userInfo);
                //登录账号
                super.loginAccount(userInfo, baseSystemInfo);
                //返回登录信息
                LoginTicketModel loginTicketModel = getTicketSuccessReturn(userInfo);
                updateTicket(loginTicketModel);
                return ActionResult.success();
            } else {
                JSONArray tenantUserInfo = socialsUserInfo.getTenantUserInfo();
                for (int i = 0; i < tenantUserInfo.size(); i++) {
                    JSONObject o = tenantUserInfo.getJSONObject(i);
                    o.remove("socialId");
                    o.remove("socialType");
                }
                LoginTicketModel loginTicketModel = getTicketMultitenancy(tenantUserInfo);
                updateTicket(loginTicketModel);
                return ActionResult.success(loginTicketModel);
            }
        } else {
            if (socialsUserInfo == null || socialsUserInfo.getUserInfo() == null) {
                LoginTicketModel ticketModel = getTicketUnbind(source, socialsUserInfo.getSocialUnionid(), socialsUserInfo.getSocialName());//第三方未绑定账号!
                updateTicket(ticketModel);
                return ActionResult.fail(MsgCode.OA017.get());
            }
            UserInfo userInfo = socialsUserInfo.getUserInfo();
            //切换租户
            switchTenant(userInfo);
            //获取系统配置
            BaseSystemInfo baseSystemInfo = getSysconfig(userInfo);
            //登录账号
            super.loginAccount(userInfo, baseSystemInfo);
            LoginTicketModel loginTicketModel = getTicketSuccessReturn(userInfo);
            updateTicket(loginTicketModel);
            return ActionResult.success();
        }
    }
}

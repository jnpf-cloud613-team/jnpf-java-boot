package jnpf.granter;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaRequest;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jnpf.implicit.utils.ImplicitLoginUtil;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.config.JnpfOauthConfig;
import jnpf.constant.MsgCode;
import jnpf.consts.AuthConsts;
import jnpf.consts.LoginTicketStatus;
import jnpf.exception.LoginException;
import jnpf.model.BaseSystemInfo;
import jnpf.model.LoginTicketModel;
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

import java.util.Map;

import static jnpf.granter.ImplicitTokenGranter.GRANT_TYPE;

@Slf4j
@Component(GRANT_TYPE)
public class ImplicitTokenGranter extends AbstractTokenGranter {

    public static final String GRANT_TYPE = "implicit";
    public static final Integer ORDER = 5;
    private static final String URL_LOGIN = "/Login/implicit/**";


    private ImplicitLoginUtil implicitLoginUtil;


    private SocialsUserController socialsUserApi;


    private JnpfOauthConfig oauthConfig;

    @Autowired
    public void setImplicitLoginUtil(ImplicitLoginUtil implicitLoginUtil) {
        this.implicitLoginUtil = implicitLoginUtil;
    }
    @Autowired
    public void setSocialsUserApi(SocialsUserController socialsUserApi) {
        this.socialsUserApi = socialsUserApi;
    }
    @Autowired
    public void setOauthConfig(JnpfOauthConfig oauthConfig) {
        this.oauthConfig = oauthConfig;
    }

    public ImplicitTokenGranter() {
        super(URL_LOGIN);
    }

    protected String getGrantType() {
        return GRANT_TYPE;
    }
    

    public int getOrder() {
        return ORDER;
    }

    

    @Override
    protected String getUserDetailKey() {
        return AuthConsts.USERDETAIL_USER_ID;
    }

    public ActionResult granter(Map<String, String> map) throws LoginException {
        SaRequest req = SaHolder.getRequest();
        String code = req.getParam("code");
        String source = req.getParam("source");
        String state = req.getParam("state");
        if(StringUtil.isEmpty(source)) {
            String userAgent = ServletUtil.getUserAgent();
            if (userAgent.contains("wxwork")) {
                source = "wechat_enterprise";
            }
            if (userAgent.contains("DingTalk")) {
                source = "dingtalk";
            }
        }
        if (StringUtil.isEmpty(code)) {
            code = req.getParam("authCode") != null ? req.getParam("authCode") : req.getParam("auth_code");
        }
        //授权回调，登录接口，重定向携带token的首页
        if (StringUtil.isEmpty(source)) {
            return ActionResult.fail(MsgCode.OA028.get());
        }
        //跳js页面，直接调用授权链接
        if (StringUtil.isEmpty(code)) {
            String authLink = implicitLoginUtil.getAuthLink(source);
            SaHolder.getResponse().redirect(authLink);
            return null;
        }
        String uuid = implicitLoginUtil.loginByCode(source, code, state);
        //uuid登录
        return this.loginByUuid(source, uuid);
    }

    /**
     * 通过第三方用户id登录
     * @param source
     * @param uuid
     * @return
     * @throws LoginException
     */
    protected ActionResult<Object> loginByUuid(String source, String uuid) throws LoginException {
        boolean isApp = "APP".equalsIgnoreCase(UserProvider.getDeviceForAgent().getDevice());
        String url = isApp ? configValueUtil.getAppDomain() : configValueUtil.getFrontDomain();
        SocialsUserInfo socialsUserInfo = socialsUserApi.getUserInfo(source, uuid, null);
        if (configValueUtil.isMultiTenancy()) {
            if (socialsUserInfo == null || CollUtil.isEmpty(socialsUserInfo.getTenantUserInfo())) {
                SocialUnbindModel obj = new SocialUnbindModel(source, uuid, null);
                //未绑定写入缓存
                LoginTicketModel ticketModel = (new LoginTicketModel())
                        .setStatus(LoginTicketStatus.UN_BIND_MES.getStatus())
                        .setTicketTimeout(System.currentTimeMillis() + oauthConfig.getTicketTimeout() * 1000)
                        .setValue(JSON.toJSONString(obj));
                createdTicketState(ticketModel, url, isApp);
                return ActionResult.success();
            }
            if (socialsUserInfo.getTenantUserInfo().size() == 1) {
                UserInfo userInfo = socialsUserInfo.getUserInfo();
                //切换租户
                switchTenant(userInfo);
                //获取系统配置
                BaseSystemInfo baseSystemInfo = getSysconfig(userInfo);
                //登录账号
                String token = super.loginAccount(userInfo, baseSystemInfo);
                //返回登录信息
                String redirectUrl = url + "/sso" + "?token=" + token;
                if (isApp) {
                    redirectUrl = url + "/pages/login/sso-redirect" + "?token=" + token;
                }
                SaHolder.getResponse().redirect(redirectUrl);
                return ActionResult.success();
            } else {
                //多租户信息写入ticket缓存
                JSONArray tenantUserInfo = socialsUserInfo.getTenantUserInfo();
                for (int i = 0; i < tenantUserInfo.size(); i++) {
                    JSONObject o =  tenantUserInfo.getJSONObject(i);
                    o.remove("socialId");
                    o.remove("socialType");
                }
                LoginTicketModel ticketModel = (new LoginTicketModel())
                        .setStatus(LoginTicketStatus.MULTITENANCY.getStatus())
                        .setValue(tenantUserInfo.toJSONString())
                        .setTicketTimeout(System.currentTimeMillis() + oauthConfig.getTicketTimeout() * 1000);
                createdTicketState(ticketModel, url, isApp);
                return ActionResult.success();
            }
        } else {
            if (socialsUserInfo == null || socialsUserInfo.getUserInfo() == null) {
                SocialUnbindModel obj = new SocialUnbindModel(source, uuid, null);
                //未绑定写入缓存
                LoginTicketModel ticketModel = (new LoginTicketModel())
                        .setStatus(LoginTicketStatus.UN_BIND_MES.getStatus())
                        .setTicketTimeout(System.currentTimeMillis() + oauthConfig.getTicketTimeout() * 1000)
                        .setValue(JSON.toJSONString(obj));
                createdTicketState(ticketModel, url, isApp);
                return ActionResult.success();
            }
            UserInfo userInfo = socialsUserInfo.getUserInfo();
            //切换租户
            switchTenant(userInfo);
            //获取系统配置
            BaseSystemInfo baseSystemInfo = getSysconfig(userInfo);
            //登录账号
            String token = super.loginAccount(userInfo, baseSystemInfo);
            String redirectUrl = url + "/sso" + "?token=" + token;
            if (isApp) {
                redirectUrl = url + "/pages/login/sso-redirect" + "?token=" + token;
            }
            SaHolder.getResponse().redirect(redirectUrl);
            return ActionResult.success();
        }
    }

    /**
     * 创建票据
     * @param loginTicketModel
     * @param url
     * @param isApp
     * @return
     */
    private String createdTicketState(LoginTicketModel loginTicketModel, String url, boolean isApp) {
        String ticket = TicketUtil.createTicket(loginTicketModel, oauthConfig.getTicketTimeout());
        String multitenancyUrl = url + "/login?JNPF_TICKET=" + ticket;
        if (isApp) {
            multitenancyUrl = url + "/pages/login/index?JNPF_TICKET=" + ticket;
        }
        SaHolder.getResponse().redirect(multitenancyUrl);
        return ticket;
    }

    /**
     * 未绑定-更新票据缓存
     *
     * @param socialType
     * @param socialUnionid
     * @param socialName
     * @return
     */
    protected LoginTicketModel updateTicketUnbind(String socialType, String socialUnionid, String socialName) {
        LoginTicketModel loginTicketModel = null;
        SocialUnbindModel obj = new SocialUnbindModel(socialType, socialUnionid, socialName);
        String ticket = this.getJnpfTicket();
        if (!ticket.isEmpty()) {
            loginTicketModel = (new LoginTicketModel()).setStatus(LoginTicketStatus.UN_BIND.getStatus()).setValue(JSON.toJSONString(obj));
            TicketUtil.updateTicket(ticket, loginTicketModel,  300L);
        }
        return loginTicketModel;
    }
}

package jnpf.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.text.StrPool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.ActionResultCode;
import jnpf.base.UserInfo;
import jnpf.base.service.SmsTemplateService;
import jnpf.base.service.SysconfigService;
import jnpf.config.ConfigValueUtil;
import jnpf.config.JnpfOauthConfig;
import jnpf.constant.JnpfConst;
import jnpf.constant.MsgCode;
import jnpf.constant.PermissionConst;
import jnpf.consts.AuthConsts;
import jnpf.consts.DeviceType;
import jnpf.consts.LoginTicketStatus;
import jnpf.consts.ScanCodeTicketStatus;
import jnpf.database.util.LoginSaasUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.LoginException;
import jnpf.granter.TenantSaasGranter;
import jnpf.granter.UserDetailsServiceBuilder;
import jnpf.model.*;
import jnpf.model.login.MeInfoVO;
import jnpf.model.login.PcUserVO;
import jnpf.model.login.UserSystemVO;
import jnpf.model.logout.LogoutResultModel;
import jnpf.permission.controller.SocialsUserController;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.socails.SocialsUserVo;
import jnpf.permission.service.UserService;
import jnpf.service.AuthService;
import jnpf.service.LogService;
import jnpf.service.LoginService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

import static jnpf.consts.AuthConsts.PARAMS_JNPF_TICKET;

/**
 * 登录控制器
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Tag(name = "登陆数据", description = "oauth")
@Slf4j
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class LoginController {


    private final UserService userApi;

    private final LoginService loginService;

    private final AuthService authService;

    private final ConfigValueUtil configValueUtil;

    private final JnpfOauthConfig oauthConfig;

    private final SysconfigService sysConfigApi;

    private final RedisUtil redisUtil;

    private final SocialsUserController socialsUserApi;

    private final UserDetailsServiceBuilder userDetailsServiceBuilder;

    private final LogService logApi;

    private final SmsTemplateService smsTemplateService;

    /**
     * 登陆
     *
     * @param parameters 登录参数
     * @return
     * @throws LoginException 登录异常
     */
    @NoDataSourceBind
    @Operation(summary = "登陆")
    @Parameter(name = "parameters", description = "登录参数", required = true)
    @RequestMapping(value = "/Login/**", method = {RequestMethod.GET, RequestMethod.POST})
    public ActionResult<LoginVO> login(@RequestParam Map<String, String> parameters) throws LoginException {
        return authService.login(parameters);
    }


    /**
     * 验证密码
     *
     * @param loginForm 登录模型
     * @return
     * @throws LoginException 登录异常
     */
    @Operation(summary = "锁屏解锁登录")
    @Parameter(name = "loginForm", description = "登录模型", required = true)
    @PostMapping("/LockScreen")
    public ActionResult<Object> lockScreen(@RequestBody LoginForm loginForm) throws LoginException {
        UserEntity userEntity = userApi.getUserByAccount(loginForm.getAccount());
        if (userEntity == null) {
            UserInfo userInfo = UserProvider.getUser();
            if (userInfo.getUserId() != null) {
                UserProvider.logoutByUserId(userInfo.getUserId());
            }
            throw new LoginException(MsgCode.FA001.get());
        }
        if (!Md5Util.getStringMd5(loginForm.getPassword().toLowerCase() + userEntity.getSecretkey().toLowerCase()).equals(userEntity.getPassword())) {
            throw new LoginException(MsgCode.OA020.get());
        }
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 登录注销
     *
     * @param grandtype 登录类型
     * @return
     */
    @NoDataSourceBind
    @Operation(summary = "退出")
    @Parameter(name = "grandtype", description = "登录类型", required = true)
    @RequestMapping(value = {"/Logout", "/Logout/{grandtype}"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ActionResult<LogoutResultModel> logout(@PathVariable(value = "grandtype", required = false) String grandtype) {
        return authService.logout(grandtype);
    }

    /**
     * 踢出指定用户, 推送Websocket用户被强制下线
     *
     * @param tokens   token集合
     * @param userId   租户id
     * @param tenantId 租户id
     */
    @NoDataSourceBind
    @Operation(summary = "踢出指定用户")
    @Parameter(name = "tokens", description = "token集合")
    @Parameter(name = "userId", description = "租户id")
    @Parameter(name = "tenantId", description = "租户id")
    @PostMapping(value = {"/KickoutToken"})
    public void kickoutByToken(@RequestParam(value = "tokens", required = false) String[] tokens, @RequestParam(name = "userId", required = false) String userId, @RequestParam(name = "tenantId", required = false) String tenantId) {
        if (StringUtil.isNotEmpty(tokens)) {
            authService.kickoutByToken(tokens);
        } else {
            authService.kickoutByUserId(userId, tenantId);
        }
    }

    /**
     * 获取用户登录信息
     *
     * @param type Web/App
     * @return
     * @throws LoginException 登录异常
     */
    @Operation(summary = "获取用户登录信息")
    @Parameter(name = "type", description = "Web/App")
    @GetMapping("/CurrentUser")
    public ActionResult<PcUserVO> currentUser(String type, String systemCode, @RequestParam(value = "isBackend", required = false) Integer isBackend) throws LoginException {
        type = StringUtil.isEmpty(type) ? JnpfConst.WEB : JnpfConst.APP;

        if (!Objects.equals(isBackend, 1)) {
            isBackend = 0;
        }
        UserInfo userInfo = UserProvider.getUser();
        if (DeviceType.TEMPUSERLIMITED.getDevice().equals(userInfo.getLoginDevice())) {
            throw new LoginException(MsgCode.OA022.get());
        }
        PcUserVO pcUserVO = loginService.getCurrentUser(type, systemCode, isBackend);
        if (pcUserVO == null) {
            throw new LoginException(MsgCode.LOG001.get());
        }
        //自动切身份
        if (StringUtil.isEmpty(pcUserVO.getCurrentSystemId()) && JnpfConst.APP.equals(type)
                && CollUtil.isNotEmpty(pcUserVO.getUserInfo().getStandingList())) {
            List<UserSystemVO> standingList = pcUserVO.getUserInfo().getStandingList();
            //先切使用者
            UserSystemVO userSystemVO = standingList.stream().filter(t -> PermissionConst.USER.equals(t.getEnCode())).findFirst().orElse(null);
            if (userSystemVO != null) {
                UserEntity user = new UserEntity();
                user.setId(userInfo.getUserId());
                user.setAppStanding(userSystemVO.getId());
                boolean update = userApi.updateById(user);
                if (update) {
                    pcUserVO = loginService.getCurrentUser(type, systemCode, isBackend);
                }
            }
            //没有使用者遍历其他身份
            if (userSystemVO == null || StringUtil.isEmpty(pcUserVO.getCurrentSystemId())) {
                for (UserSystemVO standVo : standingList) {
                    if (!standVo.isCurrentStanding()) {
                        UserEntity user = new UserEntity();
                        user.setId(userInfo.getUserId());
                        user.setAppStanding(standVo.getId());
                        boolean update = userApi.updateById(user);
                        if (update) {
                            pcUserVO = loginService.getCurrentUser(type, systemCode, isBackend);
                        }
                        if (!StringUtil.isEmpty(pcUserVO.getCurrentSystemId())) break;
                    }
                }
            }

            //身份切换后还是没有系统报错
            if (StringUtil.isEmpty(pcUserVO.getCurrentSystemId())) {
                throw new LoginException(MsgCode.LOG004.get());
            }
        }
        pcUserVO.setCurrentSystemId(null);
        //当前身份没有菜单信息时，直接提示权限不足
        if (pcUserVO.getMenuList().isEmpty() && StringUtil.isNotEmpty(systemCode)) {
            if (Objects.equals(isBackend, 1)) {
                throw new LoginException(MsgCode.LOG013.get());
            }
            throw new LoginException(MsgCode.LOG004.get());
        }
        //app没有身份和权限的时候直接退出
        if (JnpfConst.APP.equals(type)
                && CollUtil.isEmpty(pcUserVO.getUserInfo().getStandingList())
                && pcUserVO.getMenuList().isEmpty()) {
            return ActionResult.fail(ActionResultCode.SESSIONOVERDUE.getCode(), MsgCode.LOG004.get());
        }
        return ActionResult.success(pcUserVO);
    }

    /**
     * 修改密码信息发送
     */
    @Operation(summary = "修改密码信息发送")
    @PostMapping("/updatePasswordMessage")
    public ActionResult<Object> updatePasswordMessage() {
        loginService.updatePasswordMessage();
        return ActionResult.success();
    }

    /**
     * 图形验证码
     *
     * @param codeLength 验证码长度
     * @param timestamp  验证码标识
     */
    @NoDataSourceBind()
    @Operation(summary = "图形验证码")
    @Parameter(name = "codeLength", description = "验证码长度", required = true)
    @Parameter(name = "timestamp", description = "验证码标识", required = true)
    @GetMapping("/ImageCode/{codeLength}/{timestamp}")
    public void imageCode(@PathVariable("codeLength") Integer codeLength, @PathVariable("timestamp") String timestamp) {
        DownUtil.downCode(codeLength);
        redisUtil.insert(timestamp, ServletUtil.getSession().getAttribute(CodeUtil.RANDOMCODEKEY), 300);
    }

    /**
     * 注销用户
     *
     * @return
     */
    @Operation(summary = "注销用户")
    @PostMapping("/logoutCurrentUser")
    public ActionResult<Object> logoutCurrentUser() {
        UserInfo userInfo = UserProvider.getUser();
        if (userInfo.getIsAdministrator() != null && UserProvider.getUser().getIsAdministrator()) {
            return ActionResult.fail(MsgCode.OA023.get());
        }
        if (userInfo.getIsAdministrator() != null && Boolean.FALSE.equals(userInfo.getIsAdministrator())) {
                userApi.delete(userApi.getInfo(userInfo.getUserId()));
                UserProvider.kickoutByUserId(userInfo.getUserId(), userInfo.getTenantId());
            }

        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 判断是否需要验证码
     *
     * @param account 账号
     */
    @NoDataSourceBind()
    @Operation(summary = "判断是否需要验证码")
    @Parameter(name = "account", description = "账号", required = true)
    @GetMapping("/getConfig/{account}")
    public ActionResult<LoginModel> check(@PathVariable("account") String account) throws LoginException {
        LoginModel loginModel = new LoginModel();
        if (configValueUtil.isMultiTenancy()) {
            LoginForm loginForm = new LoginForm();
            loginForm.setAccount(account);
            UserInfo userInfo = new UserInfo();
            userInfo.setUserAccount(loginForm.getAccount());
            loginService.getTenantAccount(userInfo);
        }
        // 获取配置
        BaseSystemInfo sysConfigInfo = sysConfigApi.getSysInfo();
        // 是否开启验证码
        if (Objects.nonNull(sysConfigInfo) && "1".equals(String.valueOf(sysConfigInfo.getEnableVerificationCode()))) {
            loginModel.setEnableVerificationCode(1);
            Integer verificationCodeNumber = sysConfigInfo.getVerificationCodeNumber();
            loginModel.setVerificationCodeNumber(verificationCodeNumber == null ? 4 : verificationCodeNumber);
            return ActionResult.success(loginModel);
        }
        loginModel.setEnableVerificationCode(0);
        return ActionResult.success(loginModel);
    }

    /**
     * 获取登录配置, 是否需要跳转、第三方登录信息
     *
     * @return {re}
     * @throws LoginException 登录异常
     */
    @NoDataSourceBind()
    @Operation(summary = "获取登录配置")
    @GetMapping("/getLoginConfig")
    public ActionResult<Object> getLoginConfig() {
        LoginConfigModel<SocialsUserVo> loginConfigModel = new LoginConfigModel<>();
        if (configValueUtil.isMultiTenancy()) {
            loginConfigModel.setIsTenant(true);
            if (Objects.equals(configValueUtil.getMultiTenancyVersion(), 2)) {
                loginConfigModel.setIsSaas(true);
            }
        }

        if (Boolean.TRUE.equals(oauthConfig.getSsoEnabled())) {
            String url = oauthConfig.getLoginPath() + StrPool.SLASH + oauthConfig.getDefaultSSO();
            loginConfigModel.setRedirect(true);
            loginConfigModel.setUrl(url);
            loginConfigModel.setTicketParams(PARAMS_JNPF_TICKET);
        } else {
            //追加第三方登录配置
            List<SocialsUserVo> loginList = socialsUserApi.getLoginList(PARAMS_JNPF_TICKET.toUpperCase());
            if (CollUtil.isNotEmpty(loginList)) {
                loginConfigModel.setSocialsList(loginList);
                loginConfigModel.setRedirect(false);
                loginConfigModel.setTicketParams(PARAMS_JNPF_TICKET);
            }
        }
        return ActionResult.success(loginConfigModel);
    }


    /**
     * 获取登录票据
     *
     * @return {msg:有效期, data:票据}
     */
    @NoDataSourceBind()
    @Operation(summary = "获取登录票据")
    @GetMapping("/getTicket")
    public ActionResult<String> getTicket() {
        LoginTicketModel ticketModel = new LoginTicketModel();
        ticketModel.setTicketTimeout(System.currentTimeMillis() + oauthConfig.getTicketTimeout() * 1000);
        String ticket = TicketUtil.createTicket(ticketModel, oauthConfig.getTicketTimeout());
        return ActionResult.success(ticketModel.getTicketTimeout().toString(), ticket);
    }

    /**
     * 检测票据登录状态
     *
     * @return {re}
     * @throws LoginException
     */
    @NoDataSourceBind()
    @Operation(summary = "获取登录状态")
    @GetMapping("/getTicketStatus/{ticket}")
    public ActionResult<LoginTicketModel> getTicketStatus(@PathVariable("ticket") String ticket) {
        LoginTicketModel ticketModel = TicketUtil.parseTicket(ticket);
        if (ticketModel == null){
            ticketModel = new LoginTicketModel().setStatus(LoginTicketStatus.INVALID.getStatus()).setValue("票据失效！");
        } else if(ticketModel.getStatus() != LoginTicketStatus.UN_LOGIN.getStatus()
                    && ticketModel.getStatus() != LoginTicketStatus.UN_BIND.getStatus()
                    && ticketModel.getStatus() != LoginTicketStatus.UN_BIND_MES.getStatus()
                    && ticketModel.getStatus() != LoginTicketStatus.MULTITENANCY.getStatus()) {
            TicketUtil.deleteTicket(ticket);
        }
        return ActionResult.success(ticketModel);
    }

    /**
     * 官网重置密码专用
     *
     * @return
     */
    @NoDataSourceBind()
    @Operation(summary = "官网重置密码专用")
    @GetMapping("/resetOfficialPassword/{mobile}/{code}")
    public ActionResult<Object> resetOfficialPassword(@PathVariable("mobile") String mobile, @PathVariable("code") String code) throws LoginException {
        //校验验证码
        TenantDataSourceUtil.checkOfficialSmsCode(mobile, code, 2);
        //切换租户
        LoginForm loginForm = new LoginForm();
        loginForm.setAccount(mobile);
        UserInfo userInfo = new UserInfo();
        userInfo.setUserAccount(loginForm.getAccount());
        try {
            userInfo = loginService.getTenantAccount(userInfo);
        } catch (Exception e) {
            return ActionResult.fail(MsgCode.LOG105.get());
        }

        // 重置密码 123456
        UserEntity userEntity = userDetailsServiceBuilder.getUserDetailService(AuthConsts.USERDETAIL_ACCOUNT).loadUserEntity(userInfo);
        userEntity.setPassword("e10adc3949ba59abbe56e057f20f883e");
        userEntity.setPassword(Md5Util.getStringMd5(userEntity.getPassword().toLowerCase() + userEntity.getSecretkey().toLowerCase()));
        boolean result = userApi.updateById(userEntity);
        if (result) {
            return ActionResult.success(MsgCode.LOG205.get());
        } else {
            return ActionResult.fail(MsgCode.LOG206.get());
        }
    }

    /**
     * 生成扫码凭证
     *
     * @return
     */
    @NoDataSourceBind()
    @Operation(summary = "生成扫码凭证")
    @GetMapping("/codeCertificate")
    public ActionResult<Object> codeCertificate() throws LoginException {
        ScanCodeLoginConfigModel ticketModel = new ScanCodeLoginConfigModel();
        ticketModel.setTicketTimeout(System.currentTimeMillis() + configValueUtil.getCodeCertificateTimeout() * 1000);
        String ticket = TicketUtil.createTicket(ticketModel, configValueUtil.getCodeCertificateTimeout());
        return ActionResult.success(ticketModel.getTicketTimeout().toString(), ticket);
    }

    /**
     * 获取扫码凭证状态
     *
     * @param ticket
     * @return
     * @throws LoginException
     */
    @NoDataSourceBind()
    @Operation(summary = "获取扫码凭证状态")
    @GetMapping("/codeCertificateStatus/{ticket}")
    public ActionResult<Object> codeCertificateStatus(@PathVariable("ticket") String ticket) throws LoginException {
        ScanCodeLoginConfigModel ticketModel = TicketUtil.parseTicket(ticket);
        if (ticketModel == null) {
            ticketModel = new ScanCodeLoginConfigModel();
            ticketModel.setStatus(ScanCodeTicketStatus.INVALID.getStatus());
        }
        return ActionResult.success(ticketModel);
    }

    /**
     * 确认登录
     *
     * @param ticket
     * @return
     * @throws LoginException
     */
    @NoDataSourceBind()
    @Operation(summary = "确认登录")
    @GetMapping("/confirmLogin/{ticket}")
    public ActionResult<Object> confirmLogin(@PathVariable("ticket") String ticket) throws LoginException {
        ScanCodeLoginConfigModel ticketModel = TicketUtil.parseTicket(ticket);
        if (ticketModel == null || !Objects.equals(ticketModel.getStatus(), 1)) {
            ticketModel = new ScanCodeLoginConfigModel();
            ticketModel.setStatus(ScanCodeTicketStatus.INVALID.getStatus());
            return ActionResult.success(ticketModel);
        }
        ticketModel.setStatus(ScanCodeTicketStatus.SUCCESS.getStatus());
        Map<String, String> parameters = new HashMap<>();
        parameters.put("grant_type", "scancode");
        parameters.put("token", UserProvider.getToken());
        ticketModel.setValue(authService.login(parameters).getData().getToken());
        TicketUtil.updateTicket(ticket, ticketModel, null);
        return ActionResult.success(ticketModel);
    }

    /**
     * 更改扫码凭证状态
     *
     * @param ticket
     * @return
     * @throws LoginException
     */
    @NoDataSourceBind()
    @Operation(summary = "更改扫码凭证状态")
    @GetMapping("/setCodeCertificateStatus/{ticket}/{status}")
    public ActionResult<Object> setCodeCertificateStatus(@PathVariable("ticket") String ticket, @PathVariable("status") Integer status) throws LoginException {
        ScanCodeLoginConfigModel ticketModel = TicketUtil.parseTicket(ticket);
        if (ticketModel == null) {
            ticketModel = new ScanCodeLoginConfigModel();
            ticketModel.setStatus(ScanCodeTicketStatus.INVALID.getStatus());
        } else {
            ticketModel.setStatus(status);
        }
        ticketModel.setTicketTimeout(System.currentTimeMillis() + configValueUtil.getCodeCertificateTimeout() * 1000);
        TicketUtil.updateTicket(ticket, ticketModel, configValueUtil.getCodeCertificateTimeout());
        return ActionResult.success(ticketModel);
    }


    /**
     * 获取用户登录信息
     *
     * @return
     * @throws LoginException 登录异常
     */
    @Operation(summary = "获取用户基本信息")
    @GetMapping("/me")
    public ActionResult<MeInfoVO> me() throws LoginException {
        UserInfo userInfo = UserProvider.getUser();
        MeInfoVO meInfoVO = new MeInfoVO()
                .setUserName(userInfo.getUserName())
                .setUserId(userInfo.getUserId())
                .setUserAccount(userInfo.getUserAccount())
                .setTenantId(userInfo.getTenantId());
        return ActionResult.success(meInfoVO);
    }

    @GetMapping("/white/jumpIndex")
    @NoDataSourceBind
    @Operation(summary = "跳转前端首页, 判断桌面端或者移动端跳转至不同的地址")
    public void jumpIndex(@RequestParam(value = "from", required = false) String from) throws IOException {
        String url = ServletUtil.getIsMobileDevice() ? configValueUtil.getAppDomain() : configValueUtil.getFrontDomain();
        UrlBuilder urlBuilder = UrlBuilder.of(url);
        if (StringUtils.isNotBlank(from)) {
            urlBuilder.addQuery("from", from);
        }
        ServletUtil.getResponse().sendRedirect(urlBuilder.build());
    }

    @NoDataSourceBind
    @Operation(summary = "登陆")
    @Parameter(name = "parameters", description = "登录参数", required = true)
    @RequestMapping(value = "/Login/saas", method = {RequestMethod.GET, RequestMethod.POST})
    public ActionResult<LoginVO> loginsaas(@RequestBody Map<String, String> parameters) throws LoginException {
        parameters.put("grant_type", TenantSaasGranter.GRANT_TYPE);
        return authService.login(parameters);
    }

    @NoDataSourceBind
    @Operation(summary = "申请租户")
    @PostMapping(value = "/apply/saas")
    public ActionResult<Object> applySaas(@RequestBody JoinTenantModel model) {
        model.setAppCode(configValueUtil.getMultiTenancyAppCode());
        LoginSaasUtil.saasApply(JsonUtil.getObjectToString(model));
        return ActionResult.success("申请成功");
    }

    @NoDataSourceBind
    @Operation(summary = "租户申请短信验证码")
    @GetMapping(value = "/mobilecode/saas")
    public ActionResult<Object> mobileCode(@RequestParam("mobile") String mobile) {
        String s = LoginSaasUtil.mobileCode(mobile);
        return ActionResult.success(s);
    }


}

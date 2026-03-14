package jnpf.granter;

import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.constant.MsgCode;
import jnpf.consts.AuthConsts;
import jnpf.consts.DeviceType;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.LoginException;
import jnpf.model.BaseSystemInfo;
import jnpf.model.LoginVO;
import jnpf.model.tenant.TenantVO;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.UserProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static jnpf.granter.TenantSaasGranter.GRANT_TYPE;

@Slf4j
@Component(GRANT_TYPE)
public class TenantSaasGranter extends AbstractTokenGranter {

    public static final String GRANT_TYPE = "saas";
    public static final Integer ORDER = 1;
    private static final String URL_LOGIN = "";


    private UserService userApi;
    @Autowired
    public void setUserApi(UserService userApi) {
        this.userApi = userApi;
    }

    public TenantSaasGranter() {
        super(URL_LOGIN);
    }

    public TenantSaasGranter(String authenticationUrl) {
        super(authenticationUrl);
    }

    @Override
    public ActionResult<LoginVO> granter(Map<String, String> loginParameters) throws LoginException {
        String account = loginParameters.get("account");
        String tenantId = loginParameters.get("tenantId");
        UserInfo userInfo = UserProvider.getUser();
        userInfo.setUserAccount(account + "@" + tenantId);
        //切换租户
        switchTenant(userInfo);
        //获取系统配置
        BaseSystemInfo baseSystemInfo = getSysconfig(userInfo);
        //用户验证
        UserEntity userByAccount = userApi.getUserByAccount(account);
        if (userByAccount == null) {
            throw new LoginException(MsgCode.LOG119.get());
        }
        //登录账号
        super.loginAccount(userInfo, baseSystemInfo);
        //返回登录信息
        LoginVO loginResult = getLoginVo(userInfo);
        return ActionResult.success(loginResult);
    }

    protected LoginVO getLoginVo(UserInfo userInfo) {
        LoginVO loginVO = new LoginVO();
        loginVO.setTheme(userInfo.getTheme());
        loginVO.setToken(userInfo.getToken());
        if (configValueUtil.isMultiTenancy()) {
            //删除卫翎信息
            TenantVO tenantVO = TenantDataSourceUtil.getCacheTenantInfo(userInfo.getTenantId());
            if (tenantVO != null && tenantVO.getWl_qrcode() != null) {
                loginVO.setWlQrcode(tenantVO.getWl_qrcode());
                tenantVO.setWl_qrcode(null);
                TenantDataSourceUtil.setTenantInfo(tenantVO);
            }
        }
        return loginVO;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }



    @Override
    public ActionResult logout() {
        UserInfo userInfo = UserProvider.getUser();
        if (userInfo.getUserId() != null) {
            if ("1".equals(String.valueOf(loginService.getBaseSystemConfig(userInfo.getTenantId()).getSingleLogin()))) {
                UserProvider.logoutByUserId(userInfo.getUserId(), DeviceType.valueOf(userInfo.getLoginDevice()));
            } else {
                UserProvider.logoutByToken(userInfo.getToken());
            }
        }
        return ActionResult.success(MsgCode.OA014.get());
    }

    @Override
    protected String getGrantType() {
        return GRANT_TYPE;
    }

    @Override
    protected String getUserDetailKey() {
        return AuthConsts.USERDETAIL_ACCOUNT;
    }
}

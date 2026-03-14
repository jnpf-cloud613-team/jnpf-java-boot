package jnpf.granter;

import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.constant.MsgCode;
import jnpf.consts.AuthConsts;
import jnpf.consts.DeviceType;
import jnpf.exception.LoginException;
import jnpf.model.BaseSystemInfo;
import jnpf.model.LoginVO;
import jnpf.util.UserProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static jnpf.granter.ScanCodeTokenGranter.GRANT_TYPE;

@Slf4j
@Component(GRANT_TYPE)
public class ScanCodeTokenGranter extends AbstractTokenGranter {

    public static final String GRANT_TYPE = "scancode";
    public static final Integer ORDER = 5;
    private static final String URL_LOGIN = "";


    public ScanCodeTokenGranter() {
        super(URL_LOGIN);
    }

    /**
     * @param loginParameters {userId, tenantId}
     * @return
     * @throws LoginException
     */
    @Override
    public ActionResult granter(Map<String, String> loginParameters) throws LoginException {
        String token = loginParameters.get("token");
        // 验证token是否有效
        UserInfo userInfo = UserProvider.getUser(token);
        if (configValueUtil.isMultiTenancy()){
            userInfo.setUserAccount(userInfo.getTenantId()+"@"+userInfo.getUserAccount());
        }
        //切换租户
        switchTenant(userInfo);
        //获取系统配置
        BaseSystemInfo baseSystemInfo = getSysconfig(userInfo);
        //登录账号
        super.loginAccount(userInfo, baseSystemInfo);
        //返回登录信息
        LoginVO loginResult = getLoginVo(userInfo);
        return ActionResult.success(loginResult);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }


    protected LoginVO getLoginVo(UserInfo userInfo) {
        LoginVO loginVO = new LoginVO();
        loginVO.setTheme(userInfo.getTheme());
        loginVO.setToken(userInfo.getToken());
        return loginVO;
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
        return AuthConsts.USERDETAIL_USER_ID;
    }

}

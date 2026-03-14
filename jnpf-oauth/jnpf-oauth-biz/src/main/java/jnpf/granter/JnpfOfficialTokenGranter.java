package jnpf.granter;

import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.consts.AuthConsts;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.LoginException;
import jnpf.model.BaseSystemInfo;
import jnpf.model.LoginForm;
import jnpf.model.LoginVO;
import jnpf.permission.entity.UserEntity;
import jnpf.util.JsonUtil;
import jnpf.util.LoginHolder;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

import static jnpf.granter.JnpfOfficialTokenGranter.GRANT_TYPE;


/**
 * JNPF官网专用短信认证
 *
 * @author JNPF开发平台组
 * @user N
 * @copyright 引迈信息技术有限公司
 * @date 2022/9/17 22:13
 */
@Slf4j
@Component(GRANT_TYPE)
@RequiredArgsConstructor
public class JnpfOfficialTokenGranter extends PasswordTokenGranter {

    public static final String GRANT_TYPE = "official";


    private final UserDetailsServiceBuilder userDetailsServiceBuilder;


    @Override
    public ActionResult granter(Map<String, String> loginParameters) throws LoginException {
        LoginForm loginForm = JsonUtil.getJsonToBean(loginParameters, LoginForm.class);
        //校验短信验证码
        TenantDataSourceUtil.checkOfficialSmsCode(loginForm.getAccount(), loginForm.getCode(), 1);

        UserInfo userInfo = UserProvider.getUser();
        //切换租户
        switchTenant(userInfo);
        //获取系统配置
        BaseSystemInfo baseSystemInfo = getSysconfig(userInfo);
        //预检信息
        preAuthenticate(loginForm, userInfo, baseSystemInfo);
        //登录账号
        super.loginAccount(userInfo, baseSystemInfo);
        //返回登录信息
        LoginVO loginResult = getLoginVo(userInfo);
        return ActionResult.success(loginResult);
    }

    /**
     * 可重写实现邮箱、短信、TOTP验证
     *
     * @param loginForm
     * @param sysConfigInfo
     * @throws LoginException
     */
    @Override
    protected void preAuthenticate(LoginForm loginForm, UserInfo userInfo, BaseSystemInfo sysConfigInfo) throws LoginException {
        //验证密码
        UserEntity userEntity = userDetailsServiceBuilder.getUserDetailService(AuthConsts.USERDETAIL_ACCOUNT).loadUserEntity(userInfo);
        try {
            authenticateLock(userEntity, sysConfigInfo);
        } catch (Exception e) {
            authenticateFailure(userEntity, sysConfigInfo);
            throw e;
        }
        LoginHolder.setUserEntity(userEntity);
    }

    @Override
    protected String getGrantType() {
        return GRANT_TYPE;
    }
}

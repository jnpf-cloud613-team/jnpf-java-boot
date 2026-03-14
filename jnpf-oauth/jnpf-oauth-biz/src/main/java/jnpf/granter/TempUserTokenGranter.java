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

import java.util.Collections;
import java.util.Map;

import static jnpf.granter.TempUserTokenGranter.GRANT_TYPE;


/**
 * 临时用户认证
 * @user N
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 * @date 2022/9/16 21:25
 */
@Slf4j
@Component(GRANT_TYPE)
public class TempUserTokenGranter extends AbstractTokenGranter{

    public static final String GRANT_TYPE = "tempuser";
    public static final Integer ORDER = 4;
    private static final String URL_LOGIN = "";


    public TempUserTokenGranter(){
        super(URL_LOGIN);
    }


    /**
     *
     * @param loginParameters {userId, tenantId}
     * @return
     * @throws LoginException
     */
    @Override
    public ActionResult granter(Map<String, String> loginParameters) throws LoginException {
        String token = loginParameters.get("token");
        //验证是否由内部发起
        if(!UserProvider.isValidInnerToken(token)){
            throw new LoginException(MsgCode.OA018.get());
        }
        String userId = loginParameters.get("userId");
        String tenantId = loginParameters.get("tenantId");
        boolean limited = Boolean.parseBoolean(loginParameters.get("limited"));
        String device = limited?DeviceType.TEMPUSERLIMITED.getDevice():DeviceType.TEMPUSER.getDevice();
        //尝试获取已经登录的用户信息
        UserInfo userInfo;
        if(limited){
            //只获取限制类型的TOKEN
            userInfo = UserProvider.getUser(userId, tenantId, Collections.singletonList(DeviceType.TEMPUSERLIMITED.getDevice()), null);
        }else{
            //排除限制类型的TOKEN
            userInfo = UserProvider.getUser(userId, tenantId, null, Collections.singletonList(DeviceType.TEMPUSERLIMITED.getDevice()));
        }
        if(userInfo.getUserId() != null){
            return ActionResult.success(getLoginVo(userInfo));
        }
        userInfo = UserProvider.getUser();
        userInfo.setUserAccount(tenantId);
        userInfo.setUserId(userId);
        //切换租户
        switchTenant(userInfo);
        //获取系统配置
        BaseSystemInfo baseSystemInfo = getSysconfig(userInfo);
        //先设置用户类型
        userInfo.setLoginDevice(device);
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




    @Override
    protected void login(UserInfo userInfo, BaseSystemInfo baseSystemInfo) throws LoginException {
        UserProvider.loginNoRequest(userInfo, this.getLoginModel(userInfo, baseSystemInfo));
    }



    @Override
    protected DeviceType getDeviceType() {
        return DeviceType.TEMPUSER;
    }

    protected LoginVO getLoginVo(UserInfo userInfo){
        LoginVO loginVO = new LoginVO();
        loginVO.setTheme(userInfo.getTheme());
        loginVO.setToken(userInfo.getToken());
        return loginVO;
    }

    @Override
    public ActionResult logout() {
        //非临时用户不注销
        UserInfo userInfo = UserProvider.getUser();
        if(UserProvider.isTempUser(userInfo)){
            UserProvider.logoutByToken(userInfo.getToken());
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

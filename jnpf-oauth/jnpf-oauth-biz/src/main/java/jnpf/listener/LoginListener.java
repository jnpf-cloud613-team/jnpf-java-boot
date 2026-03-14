package jnpf.listener;

import cn.dev33.satoken.listener.SaTokenListenerForSimple;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import jnpf.base.UserInfo;
import jnpf.constant.MsgCode;
import jnpf.message.service.MessageService;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.DateUtil;
import jnpf.util.IpUtil;
import jnpf.util.LoginHolder;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginListener extends SaTokenListenerForSimple {

    public static final String BRACKET = "：{}, ";

    private final MessageService messageApi;

    private final UserService userApi;

    @Override
    public void doLogin(String loginType, Object loginId, String tokenValue, SaLoginParameter loginModel) {
        println(  MsgCode.OA001.get()+ BRACKET +MsgCode.OA002.get()+ BRACKET +MsgCode.OA003.get()+"：{}", loginId, loginType, tokenValue);
        UserInfo userInfo = UserProvider.getUser();
        //临时用户登录不记录
        if(!UserProvider.isTempUser(userInfo)) {

            UserEntity entity = LoginHolder.getUserEntity();
            entity.setLogErrorCount(0);
            entity.setUnlockTime(null);
            entity.setEnabledMark(1);
            entity.setPrevLogIp(IpUtil.getIpAddr());
            entity.setPrevLogTime(DateUtil.getNowDate());
            entity.setLastLogIp(IpUtil.getIpAddr());
            entity.setLastLogTime(DateUtil.getNowDate());
            entity.setLogSuccessCount(entity.getLogSuccessCount() != null ? entity.getLogSuccessCount() + 1 : 1);

            userApi.updateById(entity);
        }
    }

    @Override
    public void doLogout(String loginType, Object loginId, String tokenValue) {
        println(MsgCode.OA004.get()+ BRACKET +MsgCode.OA002.get()+ BRACKET +MsgCode.OA003.get()+"：{}", loginId, loginType, tokenValue);
    }

    @Override
    public void doKickout(String loginType, Object loginId, String tokenValue) {
        println(MsgCode.OA005.get()+ BRACKET +MsgCode.OA002.get()+ BRACKET +MsgCode.OA003.get()+"：{}", loginId, loginType, tokenValue);
        messageApi.logoutWebsocketByToken(tokenValue, null);
        //删除用户信息缓存, 保留Token状态记录等待自动过期, 如果用户不在线下次打开浏览器会提示被踢下线
        StpUtil.getTokenSessionByToken(tokenValue).logout();
    }

    @Override
    public void doReplaced(String loginType, Object loginId, String tokenValue) {
        println(MsgCode.OA006.get()+ BRACKET +MsgCode.OA002.get()+ BRACKET +MsgCode.OA003.get()+"：{}", loginId, loginType, tokenValue);
        messageApi.logoutWebsocketByToken(tokenValue, null);
        StpUtil.getTokenSessionByToken(tokenValue).logout();
    }

    /**
     * 打印指定字符串
     * @param str 字符串
     */
    public void println(String str, Object... params) {
        if(log.isDebugEnabled()) {
            log.debug(str, params);
        }
    }
}

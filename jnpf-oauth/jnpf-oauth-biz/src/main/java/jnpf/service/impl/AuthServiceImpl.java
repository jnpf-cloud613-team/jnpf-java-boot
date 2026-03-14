package jnpf.service.impl;

import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.MsgCode;
import jnpf.exception.LoginException;
import jnpf.exception.TenantDatabaseException;
import jnpf.exception.TenantInvalidException;
import jnpf.granter.TokenGranter;
import jnpf.granter.TokenGranterBuilder;
import jnpf.model.LoginVO;
import jnpf.model.logout.LogoutResultModel;
import jnpf.service.AuthService;
import jnpf.service.LogService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 登录与退出服务 其他服务调用
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    
    @Lazy
    private final TokenGranterBuilder tokenGranterBuilder;
    
    private final LogService logApi;
    
    private final ConfigValueUtil configValueUtil;
    
    private final RedisUtil redisUtil;

    /**
     * 登录
     * @param parameters {grant_type}
     * @return
     * @throws LoginException
     */
    @Override
    public ActionResult<LoginVO> login(Map<String, String> parameters) throws LoginException{
        long millis = System.currentTimeMillis();
        TokenGranter tokenGranter = tokenGranterBuilder.getGranter(parameters.getOrDefault("grant_type", ""));
        ActionResult<LoginVO> result;
        UserInfo userInfo = new UserInfo();
        try {
            String account = parameters.get("account");
            userInfo.setUserAccount(account);
            UserProvider.setLocalLoginUser(userInfo);
            result = tokenGranter.granter(parameters);
            //写入日志
            if (userInfo.getUserId() != null && !UserProvider.isTempUser(userInfo)) {
                logApi.writeLogAsync(userInfo.getUserId(), userInfo.getUserName() + "/" + userInfo.getUserAccount(), MsgCode.OA015.get(), (System.currentTimeMillis() - millis));
            }
        } catch (TenantDatabaseException tdex){
            throw tdex;
        } catch (Exception e){
            if(!(e instanceof LoginException || e instanceof TenantInvalidException)){
                String msg = e.getMessage();
                if(msg == null){
                    msg = MsgCode.OA007.get();
                }
                log.error(MsgCode.OA007.get()+", Account: {}, Error: {}", parameters.getOrDefault("account", ""), e.getMessage(), e);
                throw new LoginException(msg);
            }
            String userName = StringUtil.isNotEmpty(userInfo.getUserName()) ? userInfo.getUserName()+"/"+userInfo.getUserAccount() : userInfo.getUserAccount();
            logApi.writeLogAsync(userInfo.getUserId(), userName, e.getMessage(), userInfo, 0, null, (System.currentTimeMillis()-millis));
            throw e;
        }finally{
            LoginHolder.clearUserEntity();
            TenantProvider.clearBaseSystemIfo();
            // 请求之后就删除验证码 不论结果
            String imgCode = parameters.get("timestamp");
            if(StringUtil.isNotEmpty(imgCode)) {
                redisUtil.remove(imgCode);
            }
        }
        return result;
    }


    /**
     * 踢出用户, 用户将收到Websocket下线通知
     * 执行流程：认证服务退出用户->用户踢出监听->消息服务发送Websocket推送退出消息
     * @param tokens
     */
    @Override
    public ActionResult<Object> kickoutByToken(String... tokens){
        UserProvider.kickoutByToken(tokens);
        return ActionResult.success();
    }

    /**
     * 踢出用户, 用户将收到Websocket下线通知
     * 执行流程：认证服务退出用户->用户踢出监听->消息服务发送Websocket推送退出消息
     * @param userId
     * @param tenantId
     */
    @Override
    public ActionResult<Object> kickoutByUserId(String userId, String tenantId){
        UserProvider.kickoutByUserId(userId, tenantId);
        return ActionResult.success();
    }

    /**
     * 退出登录
     * @param grandtype
     * @return
     */
    @Override
    public ActionResult<LogoutResultModel> logout(String grandtype) {
        long millis = System.currentTimeMillis();
        TokenGranter tokenGranter = tokenGranterBuilder.getGranterByLogin(grandtype);
        if (tokenGranter != null) {
            UserInfo userInfo = UserProvider.getUser();
            logApi.writeLogAsync(userInfo.getUserId(), userInfo.getUserName() + "/" + userInfo.getUserAccount(), "退出登录", userInfo, 1, 1, (System.currentTimeMillis() - millis));
            return tokenGranter.logout();
        }
        return ActionResult.success();
    }
}

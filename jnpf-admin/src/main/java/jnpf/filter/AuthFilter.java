package jnpf.filter;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.filter.SaFilterAuthStrategy;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import jnpf.base.ActionResultCode;
import jnpf.constant.MsgCode;
import jnpf.consts.AuthConsts;
import jnpf.properties.GatewayWhite;
import jnpf.util.GatewayUtil;
import jnpf.util.IpUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 网关验证token
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-03-24
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuthFilter {


    private final GatewayWhite gatewayWhite;


    private final SaFilterAuthStrategy defaultBeforeAuthStrategy;



    // 注册 Sa-Token全局过滤器
    @Bean
    public SaServletFilter getSaReactorFilter() {
        return new SaServletFilter()
                // 拦截地址
                .addInclude("/**")
                .setExcludeList(gatewayWhite.getExcludeUrl())
                // 鉴权方法：每次访问进入
                .setAuth(obj -> {
                    if(log.isInfoEnabled()){
                        log.info("请求路径: {}", SaHolder.getRequest().getRequestPath());
                    }
                    //拦截路径
                    SaRouter.match(gatewayWhite.getBlockUrl()).match(o -> {
                        //禁止访问URL 排除白名单
                        String ip = getIpAddr();
                        for (String o1 : gatewayWhite.getWhiteIp()) {
                            if(ip.startsWith(o1)){
                                return false;
                            }
                        }
                        log.info("非白名单IP访问限制接口：{}, {}", SaHolder.getRequest().getRequestPath(), ip);
                        return true;
                    }).back(MsgCode.AD101.get());
                    //白名单不拦截
                    SaRouter.match(gatewayWhite.getWhiteUrl()).stop();
                    //内部请求不拦截
                    SaRouter.match(t->{
                        String innerToken = SaHolder.getRequest().getHeader(AuthConsts.INNER_TOKEN_KEY);
                        return UserProvider.isValidInnerToken(innerToken);
                    }).stop();
                    // 登录校验 -- 拦截所有路由
                    SaRouter.match("/**", r -> {
                        StpUtil.checkLogin();
                        GatewayUtil.renewToken(StpUtil.getTokenValue());
                    }).stop();
                }).setError(e -> {
                    SaHolder.getResponse().addHeader("Content-Type","application/json; charset=utf-8");
                    if(e instanceof NotLoginException){
                        return SaResult.error(ActionResultCode.SESSIONOVERDUE.getMessage()).setCode(ActionResultCode.SESSIONOVERDUE.getCode());
                    }
                    log.error(e.getMessage(), e);
                    return SaResult.error(MsgCode.AD102.get()).setCode(ActionResultCode.EXCEPTION.getCode());
                })
                // 前置函数：在每次认证函数之前执行
                .setBeforeAuth(defaultBeforeAuthStrategy);
    }


    public static String getIpAddr() {
        return IpUtil.getIpAddr();
    }


}

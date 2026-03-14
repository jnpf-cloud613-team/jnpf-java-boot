package jnpf.visualdata.controller.aop;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-03-26
 */

import jnpf.base.UserInfo;
import jnpf.config.ConfigValueUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.util.ServletUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/15 17:12
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class VisualBindAspect {


    

    private final ConfigValueUtil configValueUtil;

    @Pointcut("within(jnpf.visualdata.controller.VisualCategoryController || jnpf.visualdata.controller.VisualMapController) && (execution(* jnpf.*.controller.VisualCategoryController.list(..))  || execution(* jnpf.*.controller.VisualMapController.dataInfo(..)))")
    public void bindDataSource() {

    }

    /**
     * NoDataSourceBind 不需要绑定数据库的注解
     *
     * @param pjp
     * @return
     * @throws Throwable
     */
    @Around("bindDataSource()")
    public Object doAroundService(ProceedingJoinPoint pjp) throws Throwable {
        if (configValueUtil.isMultiTenancy()) {
            String jwtToken = ServletUtil.getRequest().getHeader("Authorization");
            if(StringUtil.isEmpty(jwtToken)){
                //兼容旧版大屏前端
                jwtToken = ServletUtil.getRequest().getParameter("token");
            }
            UserInfo userInfo = UserProvider.getUser(jwtToken);
            if(userInfo.getTenantId() == null){
                throw new IllegalArgumentException("租户信息为空: " + jwtToken);
            }
            //设置租户
            TenantDataSourceUtil.switchTenant(userInfo.getTenantId());
        }
        return pjp.proceed();
    }
}


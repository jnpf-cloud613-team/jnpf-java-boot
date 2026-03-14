package jnpf.aop;

import jnpf.constant.PermissionConstant;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 角色操作权限
 *
 * @author JNPF开发平台组 YanYu
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2022/2/10
 */
@Slf4j
@Aspect
@Component
public class PermissionRoleAspect implements PermissionAdminBase {



    /**
     * 分级管理切点
     */
    @Pointcut("within(jnpf.*.controller.*) && @annotation(jnpf.annotation.RolePermission)")
    public void pointcut() {
    }

    /**
     * 分级管理切点
     *
     * @param pjp
     * @return
     * @throws Throwable
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        return PermissionAdminBase.permissionCommon(pjp);
    }







}

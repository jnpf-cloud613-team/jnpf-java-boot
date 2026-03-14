package jnpf.aop;

import org.aspectj.lang.ProceedingJoinPoint;

public interface PermissionAdminBase{



    /**
     * 管理者权限判断
     *
     */
    static Object permissionCommon(ProceedingJoinPoint pjp) throws Throwable {

        // 是否是管理员
        return pjp.proceed();
    }

}

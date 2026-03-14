package jnpf.util;

import jnpf.permission.entity.UserEntity;


/**
 *
 * @author JNPF开发平台组
 * @copyright 引迈信息技术有限公司
 */
public class LoginHolder {
    LoginHolder(){

    }


    private static final ThreadLocal<UserEntity> USER_CACHE = new ThreadLocal<>();

    public static UserEntity getUserEntity(){
        return USER_CACHE.get();
    }

    public static void setUserEntity(UserEntity userEntity){
        USER_CACHE.set(userEntity);
    }

    public static void clearUserEntity(){
        USER_CACHE.remove();
    }
}

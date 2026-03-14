package jnpf.permission.rest;

import jnpf.permission.connector.HttpRequestUserInfoService;
import jnpf.permission.entity.UserEntity;
import jnpf.util.JsonUtil;
import jnpf.util.context.SpringContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 推送工具类
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/7/28 20:56
 */
@Slf4j
@Component
public class PullUserUtil {

    private static HttpRequestUserInfoService httpRequestUserInfoService = SpringContext.getBean(HttpRequestUserInfoService.class);

    PullUserUtil() {
    }

    /**
     * 推送到
     *
     * @param userEntity
     * @param method
     * @param tenantId
     */
    public static void syncUser(UserEntity userEntity, String method, String tenantId) {
        if (httpRequestUserInfoService != null) {
            Map<String, Object> map = JsonUtil.entityToMap(userEntity);
            httpRequestUserInfoService.syncUserInfo(map, method, tenantId);
        }
    }

}

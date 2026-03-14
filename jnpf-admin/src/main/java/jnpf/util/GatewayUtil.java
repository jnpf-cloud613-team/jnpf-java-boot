package jnpf.util;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;

/**
 * 网关工具类
 */
public class GatewayUtil {

    private GatewayUtil() {
    }

    private static TimedCache<String, Integer> renewLimit = null;
    private static final long TIMEOUT = 1000L * 60;

    static {
        renewLimit = CacheUtil.newTimedCache(TIMEOUT);
        // 一分钟清理一次无用数据
        renewLimit.schedulePrune(TIMEOUT);
    }

    /**
     * 同一个TOKEN一分钟内只会续期一次
     */
    public static void renewToken(String token) {
        if (renewLimit.containsKey(token)) {
            return;
        }
        UserProvider.renewTimeout(token);
        renewLimit.put(token, 0);
    }

}

package jnpf.flowable.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2025/1/9 14:19
 */
@Component
@RequiredArgsConstructor
public class RedisLock {
    private static final String LOCK_KEY_PREFIX = "workflow-lock:";


    private final RedisTemplate<String, Object> redisTemplate;

    private String getLockKey(String lockName) {
        return LOCK_KEY_PREFIX + lockName;
    }

    // false表示设置失败
    public boolean lock(String lockName, String lockValue, long expireTime, TimeUnit unit) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(getLockKey(lockName), lockValue, unit.toSeconds(expireTime), TimeUnit.SECONDS);

        return Boolean.TRUE.equals(result);
    }

    public boolean unlock(String lockName, String lockValue) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(getLockKey(lockName)), Collections.singletonList(lockValue));
        assert result != null;
        return result > 0;
    }
}

package jnpf.flowable.job;

import jnpf.flowable.model.trigger.TimeTriggerModel;
import jnpf.util.JsonUtil;
import jnpf.util.RedisUtil;
import jnpf.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/20 10:22
 */
@Slf4j
public class TriggerJobUtil {

    private TriggerJobUtil() {
    }

    public static final String TRIGGER_MODEL = "trigger_model";

    public static TimeTriggerModel getModel(TimeTriggerModel model, RedisUtil redisUtil) {
        String hashValues = redisUtil.getHashValues(TRIGGER_MODEL, model.getId());
        return StringUtil.isNotEmpty(hashValues) ? JsonUtil.getJsonToBean(hashValues, TimeTriggerModel.class) : null;
    }

    public static void insertModel(TimeTriggerModel model, RedisUtil redisUtil) {
        String id = model.getId();
        redisUtil.insertHash(TRIGGER_MODEL, id, JsonUtil.getObjectToString(model));
    }

    public static void removeModel(TimeTriggerModel model, RedisUtil redisUtil) {
        redisUtil.removeHash(TRIGGER_MODEL, model.getId());
    }

}

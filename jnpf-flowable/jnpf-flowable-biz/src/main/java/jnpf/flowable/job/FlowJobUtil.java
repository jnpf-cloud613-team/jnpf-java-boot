
package jnpf.flowable.job;

import jnpf.flowable.model.time.FlowTimeModel;
import jnpf.flowable.util.TimeUtil;
import jnpf.util.JsonUtil;
import jnpf.util.RedisUtil;
import jnpf.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;


/**
 * @author JNPF开发平台组
 * @version V3.3.0 flowable
 * @copyright 引迈信息技术有限公司
 * @date 2022/6/15 17:37
 */
@Slf4j
public class FlowJobUtil {

    private FlowJobUtil() {}
    /**
     * 自动审批
     */
    public static final String OPERATOR_REDIS_KEY = "flowable_operator";

    /**
     * 自动转审
     */
    public static final String OPERATOR_TRANSFER = "flowable_transfer";

    /**
     * 超时
     */
    public static final String TIME_REDIS_KEY = "flowable_timeModel";

    /**
     * 提醒
     */
    public static final String NOTICE_REDIS_KEY = "flowable_notice";

    public static FlowTimeModel getModel(FlowTimeModel model, RedisUtil redisUtil) {
        String id = model.getOperatorId();
        String hashValues = redisUtil.getHashValues(Boolean.TRUE.equals(model.getOverTime()) ? TIME_REDIS_KEY : NOTICE_REDIS_KEY, id);
        return StringUtil.isNotEmpty(hashValues) ? JsonUtil.getJsonToBean(hashValues, FlowTimeModel.class) : null;
    }

    public static void insertModel(FlowTimeModel model, RedisUtil redisUtil) {
        String integrateId = model.getOperatorId();
        redisUtil.insertHash(Boolean.TRUE.equals(model.getOverTime()) ? TIME_REDIS_KEY : NOTICE_REDIS_KEY, integrateId, JsonUtil.getObjectToString(model));
    }

    public static void removeModel(FlowTimeModel model, RedisUtil redisUtil) {
        redisUtil.removeHash(Boolean.TRUE.equals(model.getOverTime()) ? TIME_REDIS_KEY : NOTICE_REDIS_KEY, model.getOperatorId());
    }

    //-----------------------审批------------------------------

    public static void insertOperator(FlowTimeModel model, RedisUtil redisUtil) {
        redisUtil.insertHash(OPERATOR_REDIS_KEY, model.getOperatorId(), JsonUtil.getObjectToString(model));
    }

    public static List<FlowTimeModel> getOperator(RedisUtil redisUtil) {
        List<FlowTimeModel> list = new ArrayList<>();
        List<String> hashValues = redisUtil.getHashValues(OPERATOR_REDIS_KEY);
        if (!hashValues.isEmpty()) {
            for (String hashValue : hashValues) {
                FlowTimeModel integrateModel = StringUtil.isNotEmpty(hashValue) ? JsonUtil.getJsonToBean(hashValue, FlowTimeModel.class) : null;
                list.add(integrateModel);
            }
        }
        return list;
    }

    //-----------------------转审------------------------------

    public static void removeTransfer(FlowTimeModel model, RedisUtil redisUtil) {
        redisUtil.removeHash(OPERATOR_TRANSFER, model.getOperatorId());
    }

    public static void insertTransfer(FlowTimeModel model, RedisUtil redisUtil) {
        redisUtil.insertHash(OPERATOR_TRANSFER, model.getOperatorId(), JsonUtil.getObjectToString(model));
    }

    public static List<FlowTimeModel> getTransfer(RedisUtil redisUtil) {
        List<FlowTimeModel> list = new ArrayList<>();
        List<String> hashValues = redisUtil.getHashValues(OPERATOR_TRANSFER);
        if (!hashValues.isEmpty()) {
            for (String hashValue : hashValues) {
                FlowTimeModel integrateModel = StringUtil.isNotEmpty(hashValue) ? JsonUtil.getJsonToBean(hashValue, FlowTimeModel.class) : null;
                list.add(integrateModel);
            }
        }
        return list;
    }


    //-----------------------删除------------------------------

    public static void deleteByOperatorId(String operatorId, RedisUtil redisUtil) {
        FlowTimeModel timeModel = new FlowTimeModel();
        timeModel.setOperatorId(operatorId);
        FlowTimeModel flowTimeModel = getModel(timeModel, redisUtil);
        if (null != flowTimeModel) {
            TimeUtil.deleteJob(flowTimeModel.getId());
            remove(flowTimeModel, redisUtil);
        }
        timeModel.setOverTime(true);
        FlowTimeModel overTimeModel = getModel(timeModel, redisUtil);
        if (null != overTimeModel) {
            TimeUtil.deleteJob(overTimeModel.getId());
            remove(overTimeModel, redisUtil);
        }
    }

    public static void remove(FlowTimeModel model, RedisUtil redisUtil) {
        redisUtil.removeHash(OPERATOR_TRANSFER, model.getOperatorId());
        redisUtil.removeHash(OPERATOR_REDIS_KEY, model.getOperatorId());
        redisUtil.removeHash(TIME_REDIS_KEY, model.getOperatorId());
        redisUtil.removeHash(NOTICE_REDIS_KEY, model.getOperatorId());
    }
}

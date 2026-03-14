package jnpf.flowable.job;

import jnpf.flowable.model.trigger.TimeTriggerModel;
import jnpf.util.JsonUtil;
import jnpf.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.quartz.JobDataMap;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/20 14:55
 */
@Component
@RequiredArgsConstructor
public class TimeTriggerRunner implements ApplicationRunner {

    private final RedisUtil redisUtil;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> hashValues = redisUtil.getHashValues(TriggerJobUtil.TRIGGER_MODEL);
        for (String value : hashValues) {
            TimeTriggerModel model = JsonUtil.getJsonToBean(value, TimeTriggerModel.class);
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.putAll(JsonUtil.entityToMap(model));
            Date startTime = new Date(model.getStartTime());
            Date endTime = null != model.getEndTime() ? new Date(model.getEndTime()) : null;
            boolean isAdd = null == endTime || endTime.getTime() > System.currentTimeMillis();
            if (isAdd) {
                QuartzJobUtil.addJob(model.getId(), model.getCron(), TimeTriggerJob.class, jobDataMap, startTime, endTime);
            }
        }
    }
}

package jnpf.base.util.job;

import jnpf.base.model.schedule.ScheduleJobModel;
import jnpf.util.JsonUtil;
import jnpf.util.RedisUtil;
import jnpf.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 流程设计
 *
 * @author JNPF开发平台组
 * @version V3.3.0 flowable
 * @copyright 引迈信息技术有限公司
 * @date 2022/6/15 17:37
 */
@Component
@Slf4j
@DependsOn("threadPoolTaskExecutor")
public class ScheduleJobUtil {
    /**
     * 缓存key
     */
    public static final String WORKTIMEOUT_REDIS_KEY = "idgenerator_Schedule";




    /**
     * 将数据放入缓存
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/6/2
     */
    public void insertRedis(List<ScheduleJobModel> scheduleJobList, RedisUtil redisUtil) {
        for (ScheduleJobModel jobModel : scheduleJobList) {
            String id = jobModel.getId();
            String objectToString = JsonUtil.getObjectToString(jobModel);
            redisUtil.insertHash(WORKTIMEOUT_REDIS_KEY, id, objectToString);
        }
    }

    /**
     * 定时器取用数据调用创建方法
     *
     * @param
     * @return
     * @copyright 引迈信息技术有限公司
     * @date 2022/6/2
     */
    public List<ScheduleJobModel> getListRedis(RedisUtil redisUtil) {
        List<ScheduleJobModel> scheduleJobList = new ArrayList<>();
        if (redisUtil.exists(WORKTIMEOUT_REDIS_KEY)) {
            Map<String, Object> map = redisUtil.getMap(WORKTIMEOUT_REDIS_KEY);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String object = entry.getKey();
                if (map.get(object) instanceof String) {
                    ScheduleJobModel scheduleJobModel = JsonUtil.getJsonToBean(String.valueOf(map.get(object)), ScheduleJobModel.class);
                    if(StringUtil.isNotEmpty(scheduleJobModel.getId())) {
                        scheduleJobList.add(scheduleJobModel);
                    }else {
                        redisUtil.removeHash(WORKTIMEOUT_REDIS_KEY,object);
                    }
                }else {
                    redisUtil.removeHash(WORKTIMEOUT_REDIS_KEY,object);
                }
            }
        }
        return scheduleJobList;
    }


}

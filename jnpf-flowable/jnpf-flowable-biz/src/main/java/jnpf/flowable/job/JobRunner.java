package jnpf.flowable.job;

import jnpf.flowable.model.time.FlowTimeModel;
import jnpf.flowable.util.TimeUtil;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.quartz.JobDataMap;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Date;
import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/23 17:49
 */
//@Component
@RequiredArgsConstructor
public class JobRunner implements ApplicationRunner {

    private final RedisUtil redisUtil;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 获取缓存中定时相关信息
        List<FlowTimeModel> list = FlowJobUtil.getOperator(redisUtil);
        for (FlowTimeModel timeModel : list) {
            Date date = new Date();
            // 判断当前时间是否已经 超过了结束时间
            if (date.getTime() > timeModel.getEndDate().getTime()) {
                continue;
            }
            if (timeModel.getOn().equals(Boolean.TRUE)) {
                timeModel.setId(RandomUtil.uuId());
                JobDataMap jobDataMap = new JobDataMap();
                jobDataMap.putAll(JsonUtil.entityToMap(timeModel));
                FlowJobUtil.insertModel(timeModel, redisUtil);
                TimeUtil.addJob(timeModel.getId(), timeModel.getDuring(), FlowTime.class, jobDataMap, timeModel.getStartDate(), timeModel.getEndDate());
            }
        }

    }
}

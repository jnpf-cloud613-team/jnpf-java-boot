package jnpf.base.util.job;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScheduleConfig {

    @Bean
    public JobDetail scheduleJobDetail() {
        //必须调用该方法，添加任务
        return JobBuilder.newJob(Schedule.class)
                .storeDurably() //必须调用该方法，添加任务
                .build();
    }

    @Bean
    public Trigger scheduleTrigger() {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule("0 0/5 * * * ?");
        //对触发器配置任务
        return TriggerBuilder.newTrigger()
                .forJob(scheduleJobDetail())
                .withSchedule(cronScheduleBuilder) //对触发器配置任务
                .build();
    }

}

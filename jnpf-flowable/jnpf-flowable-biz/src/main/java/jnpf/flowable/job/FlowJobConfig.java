package jnpf.flowable.job;

import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/23 18:16
 */
@Configuration
public class FlowJobConfig {
    @Bean
    public JobDetail autoAuditDetail() {
        // 必须调用该方法，添加任务
        return JobBuilder.newJob(AutoAuditJob.class)
                .storeDurably() // 必须调用该方法，添加任务
                .build();
    }

    @Bean
    public Trigger autoAuditTrigger() {
        // 任务频率
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule("0/5 * * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(autoAuditDetail())
                .withSchedule(cronScheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail autoTransferDetail() {
        // 必须调用该方法，添加任务
        return JobBuilder.newJob(AutoTransferJob.class)
                .storeDurably() // 必须调用该方法，添加任务
                .build();
    }

    @Bean
    public Trigger autoTransferTrigger() {
        // 任务频率
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule("0/8 * * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(autoTransferDetail())
                .withSchedule(cronScheduleBuilder)
                .build();
    }

}

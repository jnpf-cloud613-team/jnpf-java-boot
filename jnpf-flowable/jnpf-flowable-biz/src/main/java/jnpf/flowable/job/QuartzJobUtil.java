package jnpf.flowable.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/20 16:48
 */
@Slf4j
public class QuartzJobUtil {

    private QuartzJobUtil() {
    }

    private static final SchedulerFactory schedulerFactory = new StdSchedulerFactory();

    public static void addJob(String jobName, String cron, Class<? extends Job> jobClass, JobDataMap jobDataMap, Date startDate, Date endDate) {
        if (jobDataMap == null) {
            jobDataMap = new JobDataMap();
        }
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName).setJobData(jobDataMap).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobName)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .startAt(startDate == null ? new Date() : startDate)
                .endAt(endDate != null ? endDate : null)
                .build();
        try {
            //获取实例化的 Scheduler。
            Scheduler scheduler = getScheduler();
            //将任务及其触发器放入调度器
            scheduler.scheduleJob(jobDetail, trigger);
            //调度器开始调度任务
            if (!scheduler.isShutdown()) {
                scheduler.start();
            }
        } catch (SchedulerException e) {
            log.error("新增调度失败:" + e.getMessage());
        }
    }

    private static Scheduler getScheduler() {
        try {
            return schedulerFactory.getScheduler();
        } catch (SchedulerException e) {
            e.getMessage();
        }
        return null;
    }

    public static void deleteJob(String jobName) {
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobName);
            Scheduler scheduler = getScheduler();
            if (null != scheduler) {
                scheduler.pauseTrigger(triggerKey);
                scheduler.unscheduleJob(triggerKey);
                scheduler.deleteJob(JobKey.jobKey(jobName));
            }

        } catch (SchedulerException e) {
            e.getMessage();
        }
    }
}

package jnpf.flowable.util;

import cn.hutool.core.date.DateUtil;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.enums.NodeEnum;
import jnpf.flowable.job.FlowJobUtil;
import jnpf.flowable.job.FlowTime;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.templatenode.nodejson.TimeConfig;
import jnpf.flowable.model.time.FlowTimeModel;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.RedisUtil;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/5/22 下午7:22
 */
public class TimeUtil {
    TimeUtil(){

    }

    private static final boolean TEST = false;

    private static final SchedulerFactory schedulerFactory = new StdSchedulerFactory();

    public static void addJob(String jobName, Integer time, Class<? extends Job> jobClass, JobDataMap jobDataMap, Date startDate, Date endDate) {
        if (jobDataMap == null) {
            jobDataMap = new JobDataMap();
        }
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName).setJobData(jobDataMap).build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobName)
                .withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInMinutes(TEST ? time : time * 60))
                .startAt(startDate == null ? new Date() : startDate)
                .endAt(endDate != null ? endDate : null)
                .build();
        try {
            //获取实例化的 Scheduler。
            Scheduler scheduler = getScheduler();
            if (scheduler != null) {
                //将任务及其触发器放入调度器
                scheduler.scheduleJob(jobDetail, trigger);
                //调度器开始调度任务
                if (!scheduler.isShutdown()) {
                    scheduler.start();
                }

            }



        } catch (SchedulerException e) {
            e.printStackTrace();
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
            if (scheduler != null) {
                scheduler.pauseTrigger(triggerKey);
                scheduler.unscheduleJob(triggerKey);
                scheduler.deleteJob(JobKey.jobKey(jobName));
            }

        } catch (SchedulerException e) {
            e.getMessage();
        }
    }

    public static void timeModel(List<OperatorEntity> operatorList, FlowModel flowModel, RedisUtil redisUtil) {
        FlowModel model = new FlowModel();
        model.setUserInfo(flowModel.getUserInfo());
        model.setNodeEntityList(flowModel.getNodeEntityList());
        model.setNodes(flowModel.getNodes());
        model.setDeploymentId(flowModel.getDeploymentId());
        model.setTaskEntity(flowModel.getTaskEntity());
        model.setFormData(flowModel.getFormData());
        List<FlowTimeModel> timeList = new ArrayList<>();
        TaskEntity task = flowModel.getTaskEntity();
        Map<String, NodeModel> nodes = flowModel.getNodes();
        Map<String, Object> formData = flowModel.getFormData();
        Map<String, List<OperatorEntity>> map = operatorList.stream().collect(Collectors.groupingBy(OperatorEntity::getNodeCode));
        for (Map.Entry<String, List<OperatorEntity>> stringListEntry : map.entrySet()) {
            String key = stringListEntry.getKey();
            model.setNodeCode(key);
            NodeModel nodeModel = new NodeModel();
            NodeModel childNode = nodes.get(key);
            NodeModel startNode = nodes.values().stream().filter(e -> e.getType().equals(NodeEnum.START.getType())).findFirst().orElse(new NodeModel());
            List<OperatorEntity> operatorEntities = map.get(key);
            for (OperatorEntity entity : operatorEntities) {
                TimeConfig limit = childNode.getTimeLimitConfig();
                if (limit.getOn() == 2) {
                    limit = startNode.getTimeLimitConfig();
                }
                nodeModel.setTimeLimitConfig(limit);
                boolean isOn = limit.getOn() != 0;
                if (isOn) {
                    TimeConfig notice = childNode.getNoticeConfig();
                    FlowTimeModel noticeModel = new FlowTimeModel();
                    noticeModel.setOperatorId(entity.getId());
                    noticeModel.setTaskId(entity.getTaskId());
                    noticeModel.setFlowModel(model);
                    if (notice.getOn() == 2) {
                        notice = startNode.getNoticeConfig();
                    }
                    noticeModel.setOn(notice.getOn() != 0);
                    nodeModel.setNoticeConfig(notice);
                    FlowTimeModel overModel = new FlowTimeModel();
                    overModel.setOperatorId(entity.getId());
                    overModel.setTaskId(entity.getTaskId());
                    overModel.setFlowModel(model);

                    TimeConfig overTime = childNode.getOverTimeConfig();
                    if (overTime.getOn() == 2) {
                        overTime = startNode.getOverTimeConfig();
                    }
                    overModel.setOn(overTime.getOn() != 0);
                    nodeModel.setOverTimeConfig(overTime);
                    Date date = null;
                    if (limit.getNodeLimit() == 0) {
                        date = entity.getCreatorTime();
                    } else if (limit.getNodeLimit() == 1) {
                        date = task.getStartTime();
                    } else if (limit.getNodeLimit() == 2) {
                        Object data = formData.get(limit.getFormField());
                        try {
                            date = new Date((Long) data);
                        } catch (Exception e) {
                            try {
                                date = DateUtil.parse(String.valueOf(data));
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                    if (date == null) {
                        continue;
                    }
                    noticeModel.setStartDate(DateUtil.offsetMinute(date, TEST ? notice.getFirstOver() : notice.getFirstOver() * 60));
                    noticeModel.setEndDate(DateUtil.offsetSecond(DateUtil.offsetMinute(date, TEST ? limit.getDuringDeal() : limit.getDuringDeal() * 60), 1));
                    noticeModel.setChildNode(nodeModel);
                    noticeModel.setDuring(notice.getOverTimeDuring());
                    timeList.add(noticeModel);

                    overModel.setStartDate(DateUtil.offsetMinute(noticeModel.getEndDate(), TEST ? overTime.getFirstOver() : overTime.getFirstOver() * 60));
                    overModel.setEndDate(null);
                    overModel.setChildNode(nodeModel);
                    overModel.setOverTime(true);
                    overModel.setDuring(overTime.getOverTimeDuring());
                    timeList.add(overModel);
                }
            }

        }

        for (FlowTimeModel timeModel : timeList) {
            Date date = new Date();
            if (timeModel.getEndDate() != null && timeModel.getEndDate().getTime() < date.getTime()) {
                continue;
            }
            if (Boolean.TRUE.equals(timeModel.getOn())) {
                String uuid = RandomUtil.uuId();
                timeModel.setId(uuid);
                JobDataMap jobDataMap = new JobDataMap();
                jobDataMap.putAll(JsonUtil.entityToMap(timeModel));
                FlowJobUtil.insertModel(timeModel, redisUtil);
                TimeUtil.addJob(uuid, timeModel.getDuring(), FlowTime.class, jobDataMap, timeModel.getStartDate(), timeModel.getEndDate());
            }
        }
    }


}

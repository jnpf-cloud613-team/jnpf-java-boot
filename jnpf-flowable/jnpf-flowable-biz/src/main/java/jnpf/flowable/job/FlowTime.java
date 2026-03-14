package jnpf.flowable.job;

import cn.hutool.core.util.ObjectUtil;
import jnpf.base.UserInfo;
import jnpf.config.ConfigValueUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.templatenode.nodejson.TimeConfig;
import jnpf.flowable.model.time.FlowTimeModel;
import jnpf.flowable.util.OverTimeUtil;
import jnpf.flowable.util.TimeUtil;
import jnpf.util.JsonUtil;
import jnpf.util.RedisUtil;
import jnpf.util.UserProvider;
import jnpf.util.context.SpringContext;
import org.quartz.*;
import org.springframework.scheduling.quartz.QuartzJobBean;


@DisallowConcurrentExecution
public class FlowTime extends QuartzJobBean {

    private static RedisUtil redisUtil;
    private static ConfigValueUtil configValueUtil;
    private static OverTimeUtil overTimeUtil;


    static {
        redisUtil = SpringContext.getBean(RedisUtil.class);
        configValueUtil = SpringContext.getBean(ConfigValueUtil.class);
        overTimeUtil = SpringContext.getBean(OverTimeUtil.class);
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();
        String jobName = jobDetail.getKey().getName();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        FlowTimeModel model = FlowJobUtil.getModel(JsonUtil.getJsonToBean(jobDataMap, FlowTimeModel.class), redisUtil);
        boolean isModel = model == null;
        FlowTimeModel timeModel = isModel ? JsonUtil.getJsonToBean(jobDataMap, FlowTimeModel.class) : model;
        if (timeModel != null) {
            FlowModel flowModel = timeModel.getFlowModel();
            UserInfo userInfo = flowModel.getUserInfo();
            if (configValueUtil.isMultiTenancy()) {
                TenantDataSourceUtil.switchTenant(userInfo.getTenantId());
                UserProvider.setLoginUser(userInfo);
                UserProvider.setLocalLoginUser(userInfo);
            }
            try {
                overTimeUtil.overMsg(timeModel);
                Boolean isPause = timeModel.getIsPause();
                timeModel.setIsPause(false);
                boolean overTime = timeModel.getOverTime();
                FlowJobUtil.insertModel(timeModel, redisUtil);
                if (overTime) {
                    if (Boolean.TRUE.equals(isPause)) {
                        return;
                    }
                    NodeModel nodeModel = timeModel.getChildNode();
                    TimeConfig timeConfig = nodeModel.getOverTimeConfig();
                    //超时自动审批
                    Boolean overAutoApprove = timeConfig.getOverAutoApprove();
                    if (Boolean.TRUE.equals(overAutoApprove)) {
                        Integer autoNum = timeConfig.getOverAutoApproveTime();
                        if (ObjectUtil.equals(timeModel.getNum(), autoNum)) {
                            // 放在另一个redis的key(用于自动审批)
                            FlowJobUtil.insertOperator(timeModel, redisUtil);
                        }
                    } else {
                        Integer transferNum = timeConfig.getOverAutoTransferTime();
                        if (ObjectUtil.equals(timeModel.getTransferNum(), transferNum)) {
                            FlowJobUtil.insertTransfer(timeModel, redisUtil);
                        }
                    }
                } else {
                    // 下一次时间大于结束时间 删除提醒缓存
                    if (timeModel.getEndDate().getTime() < context.getNextFireTime().getTime()) {
                        FlowJobUtil.removeModel(timeModel, redisUtil);
                        TimeUtil.deleteJob(jobName);
                    }
                }
            } catch (Exception e) {
                FlowJobUtil.removeModel(timeModel, redisUtil);
                TimeUtil.deleteJob(jobName);
            }
        } else {
            TimeUtil.deleteJob(jobName);
            FlowJobUtil.removeModel(model, redisUtil);
        }
    }


}

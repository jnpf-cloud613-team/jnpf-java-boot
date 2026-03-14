package jnpf.flowable.job;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.Method;
import jnpf.base.UserInfo;
import jnpf.config.ConfigValueUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.flowable.entity.TemplateJsonEntity;
import jnpf.flowable.enums.TemplateJsonStatueEnum;
import jnpf.flowable.model.trigger.TimeTriggerModel;
import jnpf.flowable.model.trigger.TriggerModel;
import jnpf.flowable.service.TemplateJsonService;
import jnpf.util.AuthUtil;
import jnpf.util.Constants;
import jnpf.util.JsonUtil;
import jnpf.util.RedisUtil;
import jnpf.util.context.SpringContext;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/20 9:28
 */
//@DisallowConcurrentExecution
public class TimeTriggerJob extends QuartzJobBean {
    private static RedisUtil redisUtil;
    private static ConfigValueUtil configValueUtil;
    private static TemplateJsonService templateJsonService;

    static {
        redisUtil = SpringContext.getBean(RedisUtil.class);
        configValueUtil = SpringContext.getBean(ConfigValueUtil.class);
        templateJsonService = SpringContext.getBean(TemplateJsonService.class);
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();
        String jobName = jobDetail.getKey().getName();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        TimeTriggerModel jsonToBean = JsonUtil.getJsonToBean(jobDataMap, TimeTriggerModel.class);
        TimeTriggerModel timeTriggerModel = TriggerJobUtil.getModel(jsonToBean, redisUtil);
        TimeTriggerModel model = timeTriggerModel == null ? jsonToBean : timeTriggerModel;

        UserInfo userInfo = model.getUserInfo();
        if (configValueUtil.isMultiTenancy()) {
            TenantDataSourceUtil.switchTenant(userInfo.getTenantId());
        }

        TemplateJsonEntity jsonEntity = templateJsonService.getById(model.getFlowId());

        if (null != jsonEntity && ObjectUtil.equals(jsonEntity.getState(), TemplateJsonStatueEnum.START.getCode())) {
            long currentTimeMillis = System.currentTimeMillis();
            model.setTime(currentTimeMillis);
            Integer num = model.getNum();
            Integer endTimeType = model.getEndTimeType();
            Integer endLimit = model.getEndLimit();
            int currentNum = num + 1;

            boolean isNext = true;
            // 触发次数、指定时间
            if (ObjectUtil.equals(endTimeType, 1)) {
                isNext = currentNum <= endLimit;
            } else if (ObjectUtil.equals(endTimeType, 2)) {
                isNext = currentTimeMillis <= model.getEndTime();
            }

            model.setNum(currentNum);
            if (isNext) {
                TriggerJobUtil.insertModel(model, redisUtil);

                String token = AuthUtil.loginTempUser(userInfo.getUserId(), userInfo.getTenantId(), true);
                String url = configValueUtil.getApiDomain() + "/api/workflow/trigger/TimeExecute";

                TriggerModel triggerModel = new TriggerModel();
                triggerModel.setUserInfo(userInfo);
                triggerModel.setId(model.getFlowId());
                HttpRequest request = HttpRequest.of(url).method(Method.POST).body(JsonUtil.getObjectToString(triggerModel));
                request.header(Constants.AUTHORIZATION, token);
                request.execute().body();
            } else {
                TriggerJobUtil.removeModel(model, redisUtil);
                QuartzJobUtil.deleteJob(jobName);
            }
        } else {
            TriggerJobUtil.removeModel(model, redisUtil);
            QuartzJobUtil.deleteJob(jobName);
        }
    }
}

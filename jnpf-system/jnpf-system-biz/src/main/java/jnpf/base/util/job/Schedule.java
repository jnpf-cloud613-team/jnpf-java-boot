package jnpf.base.util.job;

import cn.hutool.core.util.ObjectUtil;
import jnpf.base.UserInfo;
import jnpf.base.entity.ScheduleNewEntity;
import jnpf.base.model.schedule.ScheduleJobModel;
import jnpf.base.service.ScheduleNewService;
import jnpf.config.ConfigValueUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.TenantInvalidException;
import jnpf.util.RedisUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class Schedule extends QuartzJobBean {


    private final RedisUtil redisUtil;

    private final  ScheduleNewService scheduleNewService;

    private final  RedisTemplate redisTemplate;

    private final  ScheduleJobUtil scheduleJobUtil;

    private final  ConfigValueUtil configValueUtil;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        List<ScheduleJobModel> listRedis = scheduleJobUtil.getListRedis(redisUtil);
        for (ScheduleJobModel jobModel : listRedis) {
            String id = jobModel.getId();
            boolean useSuccess = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(ScheduleJobUtil.WORKTIMEOUT_REDIS_KEY + "_key:" + id, System.currentTimeMillis(), 100, TimeUnit.SECONDS));
            if (!useSuccess) continue;
            UserInfo userInfo = jobModel.getUserInfo();
            if (configValueUtil.isMultiTenancy()) {
                try {
                    TenantDataSourceUtil.switchTenant(userInfo.getTenantId());
                }catch (TenantInvalidException e){
                    // 租户无效 删除任务
                    log.error("Schedule, 租户无效, 删除任务：{}", userInfo.getTenantId());
                    redisUtil.removeHash(ScheduleJobUtil.WORKTIMEOUT_REDIS_KEY, id);
                }
            }
            ScheduleNewEntity info = scheduleNewService.getInfo(id);
            boolean msg = info != null && System.currentTimeMillis() >= jobModel.getScheduleTime().getTime();
            if (msg) {
                scheduleNewService.scheduleMessage(jobModel);
            }
            boolean delete = (ObjectUtil.isNull(info) || msg);
            if (delete) {
                redisUtil.removeHash(ScheduleJobUtil.WORKTIMEOUT_REDIS_KEY, id);
            }
        }
    }
}

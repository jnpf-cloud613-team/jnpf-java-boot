package jnpf.flowable.model.trigger;

import jnpf.base.UserInfo;
import lombok.Data;

/**
 * 定时触发模型
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/20 10:28
 */
@Data
public class TimeTriggerModel {
    private UserInfo userInfo;
    private String id;
    private String flowId;
    private String cron;
    private Long startTime = System.currentTimeMillis();
    private Long endTime;
    private Integer endTimeType = 1;
    private Integer endLimit = 1;
    private Integer num = 0;
    private Integer state = 0;
    private Long time = System.currentTimeMillis();
}

package jnpf.base.service;

import jnpf.base.ActionResult;
import jnpf.base.model.schedule.ScheduleNewCrForm;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/18 9:57
 */
public interface ScheduleNewApi {
    ActionResult<Object> create(ScheduleNewCrForm scheduleCrForm);
}

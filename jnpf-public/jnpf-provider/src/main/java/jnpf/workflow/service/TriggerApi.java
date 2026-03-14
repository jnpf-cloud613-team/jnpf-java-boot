package jnpf.workflow.service;

import jnpf.flowable.model.trigger.TriggerDataFo;
import jnpf.flowable.model.trigger.TriggerDataModel;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/19 14:40
 */
public interface TriggerApi {

    List<TriggerDataModel> getTriggerDataModel(TriggerDataFo fo);
}

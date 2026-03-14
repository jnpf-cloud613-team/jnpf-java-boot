package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.TriggerLaunchflowEntity;

import java.util.List;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/4/29 下午5:14
 */
public interface TriggerLaunchflowService extends SuperService<TriggerLaunchflowEntity> {

    /**
     * 保存
     *
     */
    void create(TriggerLaunchflowEntity entity);

    /**
     * 保存
     *
     */
    List<TriggerLaunchflowEntity> getTaskList(List<String> taskId);

    /**
     * 保存
     *
     */
    List<TriggerLaunchflowEntity> getTaskIds(String taskId);

    /**
     * 保存
     *
     */
    void delete(TriggerLaunchflowEntity entity);

}

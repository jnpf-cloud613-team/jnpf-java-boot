package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.LaunchUserEntity;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 9:45
 */
public interface LaunchUserService extends SuperService<LaunchUserEntity> {

    /**
     * 根据任务id获取发起用户
     *
     * @param taskId 任务id
     */
    LaunchUserEntity getInfoByTask(String taskId);

    /**
     * 根据任务id获取发起用户
     *
     * @param taskId 任务id
     */
    List<LaunchUserEntity> getTaskList(String taskId);

    /**
     * 创建发起用户
     *
     * @param taskId 任务id
     * @param userId 用户id
     */
    void createLaunchUser(String taskId, String userId);

    /**
     * 删除发起用户
     *
     * @param taskId 任务主键
     */
    void delete(String taskId);

    /**
     * 删除逐渐用户
     *
     * @param taskId 任务主键
     */
    void delete(String taskId, List<String> nodeCode);

    /**
     * 删除逐渐用户
     *
     * @param taskId 任务主键
     */
    void deleteStepUser(String taskId);
}

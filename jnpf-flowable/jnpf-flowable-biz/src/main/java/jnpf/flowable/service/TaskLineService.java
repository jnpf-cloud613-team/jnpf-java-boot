package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.TaskLineEntity;

import java.util.List;
import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/23 17:36
 */
public interface TaskLineService extends SuperService<TaskLineEntity> {
    /**
     * 列表
     *
     * @param taskId 任务主键
     */
    List<TaskLineEntity> getList(String taskId);

    /**
     * 保存任务条件
     *
     * @param taskId          任务主键
     * @param conditionResMap 条件
     */
    void create(String taskId, Map<String, Boolean> conditionResMap);

    /**
     * 获取最新的（值为true的）线的集合
     *
     * @param taskId 任务主键
     */
    List<String> getLineKeyList(String taskId);
}

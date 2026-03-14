package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.EventLogEntity;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/13 16:55
 */
public interface EventLogService extends SuperService<EventLogEntity> {
    /**
     * 列表
     */
    List<EventLogEntity> getList(String taskId);

    /**
     * 列表
     */
    List<EventLogEntity> getList(String taskId, List<String> nodeCode);

    /**
     * 删除外部节点
     */
    void delete(String taskId, List<String> nodeCode);

    /**
     * 新增外部节点
     */
    void create(EventLogEntity entity);

}

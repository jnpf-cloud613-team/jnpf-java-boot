package jnpf.flowable.service;

import jnpf.base.service.SuperService;
import jnpf.flowable.entity.CirculateEntity;
import jnpf.flowable.model.operator.OperatorVo;
import jnpf.flowable.model.task.TaskPagination;

import java.util.List;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/4/29 下午5:14
 */
public interface CirculateService extends SuperService<CirculateEntity> {

    /**
     * 列表
     *
     * @param taskId 任务主键
     */
    List<CirculateEntity> getList(String taskId);

    /**
     * 列表
     *
     * @param taskId 任务主键
     */
    List<CirculateEntity> getList(String taskId, String nodeCode);


    /**
     * 分页列表
     *
     * @param pagination 分页
     */
    List<OperatorVo> getList(TaskPagination pagination);

    /**
     * 记录列表
     *
     * @param taskId 任务主键
     * @param nodeId 节点id
     */
    List<CirculateEntity> getNodeList(String taskId, String nodeId);
}

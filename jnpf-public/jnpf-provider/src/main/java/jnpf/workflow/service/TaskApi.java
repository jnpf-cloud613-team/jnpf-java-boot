package jnpf.workflow.service;

import jnpf.base.ActionResult;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.task.TaskTo;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/28 14:35
 */
public interface TaskApi {


    /**
     * 发起流程
     *
     * @param flowModel 参数（templateId、userIds、formDataList）
     */
    ActionResult<Object> launchFlow(FlowModel flowModel);

    /**
     * 获取流程数量
     */
    TaskTo getFlowTodoCount(TaskTo taskTo);

}

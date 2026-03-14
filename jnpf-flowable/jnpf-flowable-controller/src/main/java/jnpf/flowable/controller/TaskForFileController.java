package jnpf.flowable.controller;

import jnpf.base.ActionResult;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.task.TaskTo;
import jnpf.flowable.service.TaskService;
import jnpf.flowable.util.OperatorUtil;
import jnpf.workflow.service.TaskApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/28 14:32
 */
@Component
@RequiredArgsConstructor
public class TaskForFileController implements TaskApi {

    private final TaskService taskService;

    private final  OperatorUtil operatorUtil;

    @Override
    public ActionResult<Object> launchFlow(FlowModel flowModel) {
        try {
            return operatorUtil.launchFlow(flowModel);
        } catch (Exception e) {
            e.printStackTrace();
            return ActionResult.fail(e.getMessage());
        }
    }

    @Override
    public TaskTo getFlowTodoCount(TaskTo taskTo) {
        return taskService.getFlowTodoCount(taskTo);
    }

}

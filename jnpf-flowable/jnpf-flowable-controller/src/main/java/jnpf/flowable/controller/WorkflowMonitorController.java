package jnpf.flowable.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.entity.SystemEntity;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.model.monitor.MonitorModel;
import jnpf.flowable.model.monitor.MonitorVo;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.task.FlowTodoVO;
import jnpf.flowable.model.task.TaskPagination;
import jnpf.flowable.model.task.TaskTo;
import jnpf.flowable.service.TaskService;
import jnpf.flowable.util.OperatorUtil;
import jnpf.flowable.util.ServiceUtil;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.model.authorize.AuthorizeVO;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程监控
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/15 10:15
 */
@Tag(name = "流程监控", description = "Monitor")
@RestController
@RequestMapping("/api/workflow/monitor")
@RequiredArgsConstructor
public class WorkflowMonitorController {


    private final  ServiceUtil serviceUtil;

    private final  OperatorUtil operatorUtil;


    private final  TaskService taskService;

    /**
     * 监控列表
     *
     * @param pagination 分页参数
     */
    @Operation(summary = "流程监控列表")
    @GetMapping
    public ActionResult<PageListVO<MonitorVo>> list(TaskPagination pagination) {
        List<MonitorVo> list = taskService.getMonitorList(pagination);
        List<UserEntity> userList = serviceUtil.getUserName(list.stream().map(MonitorVo::getCreatorUser).collect(Collectors.toList()));
        List<SystemEntity> systemList = serviceUtil.getSystemList(list.stream().map(MonitorVo::getSystemName).collect(Collectors.toList()));
        List<MonitorVo> voList = new LinkedList<>();
        for (MonitorVo vo : list) {
            UserEntity user = userList.stream().filter(t -> t.getId().equals(vo.getCreatorUser())).findFirst().orElse(null);
            vo.setCreatorUser(user != null ? user.getRealName() + "/" + user.getAccount() : "");
            SystemEntity system = systemList.stream().filter(t -> t.getId().equals(vo.getSystemName())).findFirst().orElse(null);
            vo.setSystemName(system != null ? system.getFullName() : "");
            if (ObjectUtil.equals(vo.getIsFile(), "0")) {
                vo.setIsFile("否");
            } else if (ObjectUtil.equals(vo.getIsFile(), "1")) {
                vo.setIsFile("是");
            } else {
                vo.setIsFile("");
            }
            voList.add(vo);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(voList, paginationVO);
    }

    /**
     * 批量删除流程监控
     *
     * @param model 参数
     */
    @Operation(summary = "批量删除流程监控")
    @DeleteMapping
    @SaCheckPermission(value = {"workFlow.flowMonitor"})
    public ActionResult<Object> delete(@RequestBody MonitorModel model) throws WorkFlowException {
        taskService.deleteBatch(model.getIds());
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 判断是否存在异步子流程
     *
     * @param id 任务主键
     */
    @Operation(summary = "判断是否存在异步子流程")
    @GetMapping("/AnySubFlow/{id}")
    public ActionResult<Object> checkAsync(@PathVariable("id") String id) {
        return ActionResult.success(taskService.checkAsync(id));
    }

    /**
     * 暂停
     *
     * @param id        主键
     * @param flowModel 参数
     */
    @Operation(summary = "暂停")
    @PostMapping("/Pause/{id}")
    @SaCheckPermission(value = {"workFlow.flowMonitor"})
    public ActionResult<String> pause(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        taskService.pause(id, flowModel, true);
        operatorUtil.handleTaskStatus();
        return ActionResult.success(MsgCode.WF074.get());
    }

    /**
     * 恢复
     *
     * @param id        主键
     * @param flowModel 参数
     */
    @Operation(summary = "恢复")
    @PostMapping("/Reboot/{id}")
    @SaCheckPermission(value = {"workFlow.flowMonitor"})
    public ActionResult<String> reboot(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        taskService.pause(id, flowModel, false);
        operatorUtil.handleTaskStatus();
        return ActionResult.success(MsgCode.WF016.get());
    }

    /**
     * 终止
     *
     * @param id        任务主键
     * @param flowModel 参数
     */
    @Operation(summary = "终止")
    @PostMapping("/Cancel/{id}")
    @SaCheckPermission(value = {"workFlow.flowMonitor"})
    public ActionResult<String> cancel(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        taskService.cancel(id, flowModel, true);
        operatorUtil.handleTaskStatus();
        return ActionResult.success(MsgCode.SU009.get());
    }

    /**
     * 复活
     *
     * @param id        任务主键
     * @param flowModel 参数
     */
    @Operation(summary = "复活")
    @PostMapping("/Activate/{id}")
    @SaCheckPermission(value = {"workFlow.flowMonitor"})
    public ActionResult<String> activate(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        taskService.cancel(id, flowModel, false);
        operatorUtil.handleTaskStatus();
        return ActionResult.success(MsgCode.WF013.get());
    }

    /**
     * 指派
     *
     * @param id        主键
     * @param flowModel 参数
     */
    @Operation(summary = "指派")
    @PostMapping("/Assign/{id}")
    @SaCheckPermission(value = {"workFlow.flowMonitor"})
    public ActionResult<String> assign(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        taskService.assign(id, flowModel);
        operatorUtil.handleOperator();
        return ActionResult.success(MsgCode.WF010.get());
    }

    /**
     * 获取我的待办
     *
     * @return
     */
    @Operation(summary = "获取我的待办")
    @PostMapping("/FlowTodoCount")
    public ActionResult<Object> getFlowTodoCount(@RequestBody TaskTo taskTo) {
        AuthorizeVO authorize = serviceUtil.getAuthorizeByUser();
        taskTo.setModuleList(authorize.getModuleList());
        TaskTo vo = taskService.getFlowTodoCount(taskTo);
        return ActionResult.success(vo);
    }

    /**
     * 获取待办事项
     *
     * @return
     */
    @Operation(summary = "获取待办事项")
    @GetMapping("/FlowTodo")
    public ActionResult<Object> getFlowTodo(@RequestParam("type") String type) {
        TaskPagination pagination = new TaskPagination();
        pagination.setDelegateType(true);
        pagination.setPageSize(10L);
        pagination.setCurrentPage(1L);
        pagination.setCategory(type);
        FlowTodoVO flowTodo = taskService.getFlowTodo(pagination);
        return ActionResult.success(flowTodo);
    }

}

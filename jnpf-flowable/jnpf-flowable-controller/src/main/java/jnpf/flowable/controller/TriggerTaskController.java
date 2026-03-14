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
import jnpf.flowable.model.trigger.TriggerInfoListModel;
import jnpf.flowable.model.trigger.TriggerPagination;
import jnpf.flowable.model.trigger.TriggerTaskModel;
import jnpf.flowable.model.util.FlowNature;
import jnpf.flowable.service.TriggerTaskService;
import jnpf.flowable.util.ServiceUtil;
import jnpf.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 任务流程
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/21 9:57
 */
@Tag(name = "任务流程实例", description = "triggerTask")
@RestController
@RequestMapping("/api/workflow/trigger/task")
@RequiredArgsConstructor
public class TriggerTaskController {

    private final  ServiceUtil serviceUtil;

    private final  TriggerTaskService triggerTaskService;


    /**
     * 获取任务下的触发记录
     *
     * @param taskId 任务主键
     */
    @Operation(summary = "获取任务下的触发记录")
    @GetMapping("/List")
    public ActionResult<Object> list(@RequestParam("taskId") String taskId, @RequestParam("nodeCode") String nodeCode) {
        List<TriggerInfoListModel> list = triggerTaskService.getListByTaskId(taskId, nodeCode);
        return ActionResult.success(list);
    }

    /**
     * 任务流程列表
     *
     * @param pagination 分页参数
     */
    @Operation(summary = "任务流程列表")
    @GetMapping
    public ActionResult<PageListVO<TriggerTaskModel>> list(TriggerPagination pagination) {
        List<TriggerTaskModel> list = triggerTaskService.getList(pagination);
        List<SystemEntity> systemList = serviceUtil.getSystemList(list.stream().map(TriggerTaskModel::getSystemName).collect(Collectors.toList()));
        List<TriggerTaskModel> voList = new ArrayList<>();
        for (TriggerTaskModel model : list) {
            model.setIsRetry(ObjectUtil.equals(model.getParentId(), FlowNature.PARENT_ID) ? 0 : 1);
            SystemEntity system = systemList.stream().filter(t -> Objects.equals(model.getSystemName(), t.getId())).findFirst().orElse(null);
            model.setSystemName(system != null ? system.getFullName() : "");
            voList.add(model);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(voList, paginationVO);
    }

    /**
     * 重试
     *
     * @param id 主键
     */
    @Operation(summary = "重试")
    @PostMapping("/Retry/{id}")
    @SaCheckPermission(value = {"workFlow.flowMonitor"})
    public ActionResult<Object> retry(@PathVariable("id") String id) throws WorkFlowException {
        triggerTaskService.retry(id);
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 批量删除
     *
     * @param model 参数
     */
    @Operation(summary = "批量删除")
    @DeleteMapping
    @SaCheckPermission(value = {"workFlow.flowMonitor"})
    public ActionResult<Object> delete(@RequestBody MonitorModel model) {
        triggerTaskService.batchDelete(model.getIds());
        return ActionResult.success(MsgCode.SU003.get());
    }
}

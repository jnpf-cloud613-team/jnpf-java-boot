package jnpf.flowable.controller;

import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.controller.SuperController;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.config.ConfigValueUtil;
import jnpf.constant.MsgCode;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.EventLogEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.enums.EventEnum;
import jnpf.flowable.enums.TaskStatusEnum;
import jnpf.flowable.model.candidates.CandidateUserVo;
import jnpf.flowable.model.outside.OutsideModel;
import jnpf.flowable.model.task.*;
import jnpf.flowable.model.template.BeforeInfoVo;
import jnpf.flowable.model.trigger.TriggerInfoModel;
import jnpf.flowable.model.util.FlowNature;
import jnpf.flowable.service.EventLogService;
import jnpf.flowable.service.TaskService;
import jnpf.flowable.service.TriggerTaskService;
import jnpf.flowable.util.*;
import jnpf.permission.entity.UserEntity;
import jnpf.util.*;
import jnpf.util.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 流程任务
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/17 15:13
 */
@Tag(name = "流程任务", description = "TaskController")
@RestController
@RequestMapping("/api/workflow/task")
@RequiredArgsConstructor
public class TaskController extends SuperController<TaskService, TaskEntity> {


    private final FlowUtil flowUtil;

    private final ServiceUtil serviceUtil;

    private final OutsideUtil outsideUtil;

    private final OperatorUtil operatorUtil;

    private final ConfigValueUtil configValueUtil;


    private final TaskService taskService;

    private final EventLogService eventLogService;

    private final TriggerTaskService triggerTaskService;

    /**
     * 详情
     *
     * @param id 任务id
     * @param fo 参数类
     */
    @Operation(summary = "详情")
    @GetMapping("/{id}")
    public ActionResult<Object> getInfo(@PathVariable("id") String id, FlowModel fo) throws WorkFlowException {
        String fileNameAll = DesUtil.aesOrDecode(id, false, true);
        int i1 = fileNameAll.indexOf(",");
        int i2 = fileNameAll.lastIndexOf(",");
        id = fileNameAll.substring(i1 + 1, i2);
        if (!Objects.equals(id, UserProvider.getLoginUserId())) {
            throw new WorkFlowException(MsgCode.FA021.getMsg());
        }
        String taskId = fileNameAll.substring(0, i1);
        if (ObjectUtil.equals(fo.getType(), 2)) {
            TriggerInfoModel triggerInfo = triggerTaskService.getInfo(taskId);
            return ActionResult.success(triggerInfo);
        }
        return ActionResult.success(taskService.getInfo(taskId, fo));
    }

    /**
     * 暂存或提交
     *
     * @param fo 参数类
     */
    @Operation(summary = "暂存或提交")
    @PostMapping
    public ActionResult<AuditModel> saveOrSubmit(@RequestBody FlowModel fo) throws WorkFlowException {
        taskService.batchSaveOrSubmit(fo);
        operatorUtil.handleOperator();
        if (ObjectUtil.equals(TaskStatusEnum.RUNNING.getCode(), fo.getStatus())) {
            flowUtil.event(fo, EventEnum.INIT.getStatus());
        }
        TaskEntity taskEntity = fo.getTaskEntity();
        if (taskEntity.getRejectDataId() == null) {
            operatorUtil.autoAudit(fo);
            operatorUtil.launchTrigger(fo);
            operatorUtil.handleOperator();
            operatorUtil.handleEvent();
        }
        operatorUtil.handleTaskStatus();
        String msg = TaskStatusEnum.TO_BE_SUBMIT.getCode().equals(fo.getStatus()) ? MsgCode.SU002.get() : MsgCode.SU006.get();
        TaskEntity task = taskService.getById(taskEntity.getId());
        taskEntity = task == null ? taskEntity : task;
        AuditModel model = operatorUtil.getAuditModel(taskEntity.getId(), fo, null);
        return ActionResult.success(msg, model);
    }

    /**
     * 暂存或提交，已暂存的再次暂存或提交
     *
     * @param id 任务主键
     * @param fo 参数
     */
    @Operation(summary = "暂存或提交(我发起的)")
    @PutMapping("/{id}")
    public ActionResult<Object> saveOrSubmit(@PathVariable("id") String id, @RequestBody FlowModel fo) throws WorkFlowException {
        Map<String, Object> data = fo.getFormData();
        String flowTaskID = Objects.nonNull(data.get(FlowFormConstant.FLOWTASKID)) ? data.get(FlowFormConstant.FLOWTASKID).toString() : id;
        fo.setId(flowTaskID);
        TaskEntity taskEntity = taskService.getById(flowTaskID);
        if (taskEntity != null) {
            flowUtil.isSuspend(taskEntity);
            flowUtil.isCancel(taskEntity);
        }
        taskService.batchSaveOrSubmit(fo);
        operatorUtil.handleOperator();
        if (ObjectUtil.equals(TaskStatusEnum.RUNNING.getCode(), fo.getStatus())) {
            flowUtil.event(fo, EventEnum.INIT.getStatus());
        }
        taskEntity = fo.getTaskEntity();
        if (taskEntity.getRejectDataId() == null) {
            operatorUtil.autoAudit(fo);
            operatorUtil.launchTrigger(fo);
            operatorUtil.handleOperator();
            operatorUtil.handleEvent();
        }
        operatorUtil.handleTaskStatus();
        TaskEntity task = taskService.getById(taskEntity.getId());
        taskEntity = task == null ? taskEntity : task;
        String msg = TaskStatusEnum.TO_BE_SUBMIT.getCode().equals(fo.getStatus()) ? MsgCode.SU002.get() : MsgCode.SU006.get();
        AuditModel model = operatorUtil.getAuditModel(taskEntity.getId(), fo, null);
        return ActionResult.success(msg, model);
    }

    /**
     * 删除
     *
     * @param id 主键
     */
    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public ActionResult<String> delete(@PathVariable("id") String id) throws WorkFlowException {
        taskService.delete(id);
        return ActionResult.success(MsgCode.SU003.get());
    }

    /**
     * 我发起的 列表
     *
     * @param pagination 分页参数
     */
    @Operation(summary = "我发起的")
    @GetMapping
    public ActionResult<PageListVO<TaskVo>> list(TaskPagination pagination) {
        pagination.setSystemId(serviceUtil.getSystemCodeById(RequestContext.getAppCode()));
        List<TaskVo> list = taskService.getList(pagination);
        List<TaskVo> voList = new ArrayList<>();
        List<UserEntity> userList = serviceUtil.getUserName(list.stream().map(TaskVo::getCreatorUser).collect(Collectors.toList()));
        for (TaskVo vo : list) {
            UserEntity userEntity = userList.stream().filter(t -> t.getId().equals(vo.getCreatorUser())).findFirst().orElse(null);
            vo.setCreatorUser(userEntity != null ? userEntity.getRealName() + "/" + userEntity.getAccount() : "");
            voList.add(vo);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(voList, paginationVO);
    }

    /**
     * 发起撤回
     *
     * @param id 任务主键
     */
    @Operation(summary = "发起撤回")
    @PutMapping("/Recall/{id}")
    public ActionResult<String> recall(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        taskService.recall(id, flowModel);
        flowUtil.event(flowModel, EventEnum.FLOW_RECALL.getStatus());
        operatorUtil.handleTaskStatus();
        return ActionResult.success(MsgCode.WF008.get());
    }

    /**
     * 催办
     *
     * @param id 任务主键
     */
    @Operation(summary = "催办")
    @PostMapping("/Press/{id}")
    public ActionResult<String> press(@PathVariable("id") String id) throws WorkFlowException {
        if (taskService.press(id)) {
            return ActionResult.success(MsgCode.WF022.get());
        }
        return ActionResult.fail(MsgCode.WF023.get());
    }

    /**
     * 撤销
     *
     * @param id 任务主键
     */
    @Operation(summary = "撤销")
    @PutMapping("/Revoke/{id}")
    public ActionResult<String> revoke(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        taskService.revoke(id, flowModel);
        return ActionResult.success(MsgCode.SU006.get());
    }

    /**
     * 获取流程所关联的用户信息
     *
     * @param id         任务主键
     * @param pagination 分页参数
     */
    @Operation(summary = "获取流程所关联的用户信息")
    @GetMapping("/TaskUserList/{id}")
    public ActionResult<PageListVO<CandidateUserVo>> getTaskUserList(@PathVariable("id") String id, Pagination pagination) {
        TaskUserListModel model = taskService.getTaskUserList(id);
        String admin = serviceUtil.getAdmin();
        List<String> allUserIdList = model.getAllUserIdList();
        allUserIdList.remove(admin);
        List<CandidateUserVo> userList = operatorUtil.getUserModel(allUserIdList, pagination);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(userList, paginationVO);
    }

    /**
     * 子流程详情
     *
     * @param flowModel 参数
     */
    @Operation(summary = "子流程详情")
    @GetMapping("/SubFlowInfo")
    public ActionResult<Object> subFlowInfo(FlowModel flowModel) throws WorkFlowException {
        List<BeforeInfoVo> list = taskService.subFlowInfo(flowModel);
        return ActionResult.success(list);
    }

    /**
     * 查看发起表单
     *
     * @param taskId 任务主键
     */
    @Operation(summary = "查看发起表单")
    @GetMapping("/ViewStartForm/{taskId}")
    public ActionResult<Object> getStartForm(@PathVariable("taskId") String taskId) throws WorkFlowException {
        ViewFormModel model = taskService.getStartForm(taskId);
        return ActionResult.success(model);
    }

    /**
     * 外部重试
     */
    @Operation(summary = "外部重试")
    @GetMapping("/Hooks/Retry/{id}")
    public ActionResult<Object> outsideRetry(@PathVariable("id") String id) throws WorkFlowException {
        boolean retryResult = outsideUtil.retry(id);
        return ActionResult.success(MsgCode.SU005.get(), retryResult);
    }

    /**
     * 外部审批
     */
    @Operation(summary = "外部审批")
    @PostMapping("/Hooks")
    @NoDataSourceBind
    public ActionResult<Object> outside(@RequestParam(value = "tenantId", required = false) String tenantId, @RequestBody Map<String, Object> body) throws WorkFlowException {
        if (configValueUtil.isMultiTenancy() && StringUtil.isNotEmpty(tenantId)) {
            // 判断是不是从外面直接请求

            //切换成租户库
            try {
                TenantDataSourceUtil.switchTenant(tenantId);
            } catch (Exception e) {
                return ActionResult.fail(MsgCode.LOG105.get());
            }

        }
        OutsideModel outsideModel = JsonUtil.getJsonToBean(body, OutsideModel.class);
        String eventId = outsideModel.getEventId();
        EventLogEntity eventLog = StringUtil.isNotEmpty(eventId) ? eventLogService.getById(eventId) : null;
        if (eventLog == null) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        TaskEntity taskEntity = taskService.getInfo(eventLog.getTaskId());
        FlowModel flowModel = new FlowModel();
        flowModel.setFormData(outsideModel.getFormData());
        flowModel.setId(eventId);
        flowModel.setTaskId(eventLog.getTaskId());
        try {
            if (FlowNature.SYSTEM_CODE.equals(eventLog.getCreatorUserId())){
                eventLog.setCreatorUserId(serviceUtil.getAdmin());
            }
            String token = AuthUtil.loginTempUser(eventLog.getCreatorUserId(), tenantId);
            UserInfo userInfo = UserProvider.getUser(token);
            UserProvider.setLoginUser(userInfo);
            UserProvider.setLocalLoginUser(userInfo);
            flowModel.setUserInfo(userInfo);
            outsideUtil.outsideAudit(flowModel, eventLog);
        } catch (Exception e) {
            operatorUtil.compensate(taskEntity);
            throw e;
        }
        operatorUtil.handleOperator();
        operatorUtil.handleEvent();
        if (taskEntity.getRejectDataId() == null) {
            operatorUtil.autoAudit(flowModel);
            operatorUtil.launchTrigger(flowModel);
            operatorUtil.handleOperator();
            operatorUtil.handleEvent();
        }
        operatorUtil.handleTaskStatus();
        return ActionResult.success(MsgCode.WF066.get());
    }


    @GetMapping("/test")
    public Map<String,String> test() {
        return ImmutableMap.of("handleId", "03d159a3-0f88-424c-a24f-02f63855fe4f,9624fa22-ac3a-4184-af0b-b2c720df9d60,3977de33-668c-4585-b09b-239aacfb4ebe");
    }
}

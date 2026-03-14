package jnpf.flowable.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jnpf.base.ActionResult;
import jnpf.base.Pagination;
import jnpf.base.controller.SuperController;
import jnpf.base.vo.ListVO;
import jnpf.base.vo.PageListVO;
import jnpf.base.vo.PaginationVO;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.CirculateEntity;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.RecordEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.enums.*;
import jnpf.flowable.model.candidates.CandidateCheckFo;
import jnpf.flowable.model.candidates.CandidateCheckVo;
import jnpf.flowable.model.candidates.CandidateUserVo;
import jnpf.flowable.model.operator.FlowBatchModel;
import jnpf.flowable.model.operator.OperatorVo;
import jnpf.flowable.model.task.AuditModel;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.task.TaskPagination;
import jnpf.flowable.model.templatenode.BackNodeModel;
import jnpf.flowable.model.util.FlowNature;
import jnpf.flowable.service.CirculateService;
import jnpf.flowable.service.OperatorService;
import jnpf.flowable.service.RecordService;
import jnpf.flowable.service.TaskService;
import jnpf.flowable.util.*;
import jnpf.permission.entity.UserEntity;
import jnpf.util.JsonUtil;
import jnpf.util.context.RequestContext;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/25 14:48
 */
@Tag(name = "经办", description = "OperatorController")
@RestController
@RequestMapping("/api/workflow/operator")
@RequiredArgsConstructor
public class OperatorController extends SuperController<OperatorService, OperatorEntity> {


    private final RedisLock redisLock;

    private final FlowUtil flowUtil;

    private final ServiceUtil serviceUtil;

    private final OperatorUtil operatorUtil;

    private final TaskService taskService;

    private final RecordService recordService;

    private final OperatorService operatorService;

    private final CirculateService circulateService;

    /**
     * 判断候选人
     *
     * @param id 经办主键
     * @param fo 参数类
     */
    @Operation(summary = "判断候选人")
    @PostMapping("/CandidateNode/{id}")
    public ActionResult<Object> candidates(@PathVariable("id") String id, @RequestBody CandidateCheckFo fo) throws WorkFlowException {
        OperatorEntity info = !ObjectUtil.equals(id, FlowNature.PARENT_ID) ? operatorService.getInfo(id) : null;
        if (info != null) {
            operatorUtil.checkOperatorPermission(id);
        }
        return ActionResult.success(taskService.checkCandidates(id, fo));
    }

    /**
     * 获取候选人
     *
     * @param fo 参数类
     */
    @Operation(summary = "获取候选人")
    @PostMapping("/CandidateUser/{id}")
    public ActionResult<PageListVO<CandidateUserVo>> candidateUser(@PathVariable("id") String id, @RequestBody CandidateCheckFo fo) throws WorkFlowException {
        OperatorEntity info = !ObjectUtil.equals(id, FlowNature.PARENT_ID) ? operatorService.getInfo(id) : null;
        if (info != null) {
            operatorUtil.checkOperatorPermission(id);
        }
        List<CandidateUserVo> candidates = taskService.getCandidateUser(id, fo);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(fo, PaginationVO.class);
        return ActionResult.page(candidates, paginationVO);
    }

    /**
     * 列表
     *
     * @param category   列表标识
     * @param pagination 参数
     */
    @Operation(summary = "列表")
    @GetMapping("/List/{category}")
    public ActionResult<PageListVO<OperatorVo>> list(@PathVariable("category") String category, TaskPagination pagination) {
        List<OperatorVo> list = new ArrayList<>();
        CategoryEnum categoryEnum = CategoryEnum.getType(category);
        pagination.setCategory(categoryEnum.getType());
        pagination.setDelegateType(true);
        pagination.setSystemId(serviceUtil.getSystemCodeById(RequestContext.getAppCode()));
        switch (categoryEnum) {
            case SIGN: // 待签
            case TODO: // 待办
            case DOING: // 在办
            case BATCH_DOING: // 批量在办
                list = operatorService.getList(pagination);
                break;
            case DONE: // 已办
                list = recordService.getList(pagination);
                break;
            case CIRCULATE: // 抄送
                list = circulateService.getList(pagination);
                break;
            default:
                break;
        }
        List<OperatorVo> vos = new ArrayList<>();
        List<UserEntity> userList = serviceUtil.getUserName(list.stream().map(OperatorVo::getCreatorUserId).collect(Collectors.toList()));
        for (OperatorVo operatorVo : list) {
            OperatorVo vo = JsonUtil.getJsonToBean(operatorVo, OperatorVo.class);
            UserEntity userEntity = userList.stream().filter(t -> t.getId().equals(vo.getCreatorUserId())).findFirst().orElse(null);
            vo.setCreatorUser(userEntity != null ? userEntity.getRealName() + "/" + userEntity.getAccount() : "");
            if (ObjectUtil.equals(operatorVo.getIsProcessing(), FlowNature.PROCESSING) && ObjectUtil.equals(operatorVo.getStatus(), OperatorStateEnum.TRANSFER.getCode())) {
                vo.setStatus(OperatorStateEnum.TRANSFER_PROCESSING.getCode());
            }
            vos.add(vo);
        }
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(vos, paginationVO);
    }

    /**
     * 签收或退签
     *
     * @param flowModel 参数类，ids 主键集合、type 0 签收 1 退签
     */
    @Operation(summary = "签收或退签")
    @PostMapping("/Sign")
    public ActionResult<String> sign(@RequestBody FlowModel flowModel) throws WorkFlowException {
        operatorService.sign(flowModel);
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 开始办理
     *
     * @param flowModel 参数类，ids 主键集合
     */
    @Operation(summary = "开始办理")
    @PostMapping("/Transact")
    public ActionResult<String> startHandle(@RequestBody FlowModel flowModel) throws WorkFlowException {
        operatorService.startHandle(flowModel);
        return ActionResult.success(MsgCode.SU005.get());
    }

    /**
     * 保存草稿
     *
     * @param id        经办主键
     * @param flowModel 参数
     */
    @Operation(summary = "保存草稿")
    @PostMapping("/SaveAudit/{id}")
    public ActionResult<String> saveAudit(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        operatorUtil.checkOperatorPermission(id);
        operatorService.saveAudit(id, flowModel);
        return ActionResult.success(MsgCode.SU002.get());
    }

    /**
     * 同意拒绝
     *
     * @param id        经办主键
     * @param flowModel 参数
     */
    @Operation(summary = "同意拒绝")
    @PostMapping("/Audit/{id}")
    public ActionResult<Object> audit(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        OperatorEntity operator = operatorUtil.checkOperator(id);
        TaskEntity taskEntity = taskService.getInfo(operator.getTaskId());
        if (null == taskEntity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        operatorUtil.checkOperatorPermission(id);
        boolean lock = redisLock.lock(taskEntity.getId() + operator.getNodeId(), operator.getId(), 5, TimeUnit.SECONDS);
        if (!lock) {
            throw new WorkFlowException("当前节点正在审批中，请稍后再试");
        }
        Integer handleStatus = flowModel.getHandleStatus();
        try {
            operatorService.auditWithCheck(id, flowModel);
        } catch (Exception e) {
            operatorUtil.compensate(taskEntity);
            throw e;
        } finally {
            redisLock.unlock(taskEntity.getId() + operator.getNodeId(), operator.getId());
        }
        operatorUtil.handleOperator();
        operatorUtil.handleEvent();
        taskEntity = flowModel.getTaskEntity();
        if (taskEntity.getRejectDataId() == null) {
            operatorUtil.autoAudit(flowModel);
            operatorUtil.handleOperator();
            operatorUtil.handleEvent();
        }
        operatorUtil.launchTrigger(flowModel);
        operatorUtil.handleTaskStatus();
        TaskEntity task = taskService.getById(taskEntity.getId());
        taskEntity = task == null ? taskEntity : task;
        AuditModel model = operatorUtil.getAuditModel(taskEntity.getId(), flowModel, operator);
        String msg = Objects.equals(handleStatus, FlowNature.REJECT_COMPLETION) ? MsgCode.WF065.get() : MsgCode.WF066.get();
        if (ObjectUtil.equals(operator.getIsProcessing(), FlowNature.PROCESSING)) {
            msg = MsgCode.WF148.get();
        }
        return ActionResult.success(msg, model);
    }

    /**
     * 加签
     *
     * @param id        经办主键
     * @param flowModel 参数
     */
    @Operation(summary = "加签")
    @PostMapping("/AddSign/{id}")
    public ActionResult<String> freeApprover(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        operatorUtil.checkOperatorPermission(id);
        operatorService.addSign(id, flowModel);
        operatorUtil.autoAudit(flowModel);
        operatorUtil.launchTrigger(flowModel);
        operatorUtil.handleOperator();
        operatorUtil.handleEvent();
        return ActionResult.success(MsgCode.WF004.get());
    }


    /**
     * 获取加签的人
     *
     * @param id         经办主键
     * @param pagination 参数
     */
    @Operation(summary = "获取加签的人")
    @PostMapping("/AddSignUserIdList/{id}")
    public ActionResult<PageListVO<CandidateUserVo>> getAddSignUserIdList(@PathVariable("id") String id, @RequestBody Pagination pagination) throws WorkFlowException {
        List<CandidateUserVo> reduceList = operatorService.getReduceList(id, pagination);
        PaginationVO paginationVO = JsonUtil.getJsonToBean(pagination, PaginationVO.class);
        return ActionResult.page(reduceList, paginationVO);
    }

    /**
     * 减签
     *
     * @param flowModel 参数
     * @param id        记录主键
     */
    @Operation(summary = "减签")
    @PostMapping("/ReduceApprover/{id}")
    public ActionResult<String> reduceApprover(@RequestBody FlowModel flowModel, @PathVariable("id") String id) throws WorkFlowException {
        RecordEntity recordEntity = recordService.getInfo(id);
        if (null == recordEntity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        if (!Objects.equals(recordEntity.getHandleId(), UserProvider.getLoginUserId())) {
            throw new WorkFlowException(MsgCode.AD104.get());
        }
        operatorService.reduce(id, flowModel);
        return ActionResult.success(MsgCode.WF069.get());
    }

    /**
     * 获取退回的节点
     *
     * @param id 经办主键
     */
    @Operation(summary = "获取退回的节点")
    @GetMapping("/SendBackNodeList/{id}")
    public ActionResult<Object> getFallbacks(@PathVariable("id") String id) throws WorkFlowException {
        ListVO<BackNodeModel> vo = new ListVO<>();
        vo.setList(operatorService.getFallbacks(id));
        return ActionResult.success(vo);
    }

    /**
     * 退回
     *
     * @param id        经办主键
     * @param flowModel 参数
     */
    @Operation(summary = "退回")
    @PostMapping("/SendBack/{id}")
    public ActionResult<String> reject(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        OperatorEntity operator = operatorUtil.checkOperator(id);
        TaskEntity taskEntity = taskService.getInfo(operator.getTaskId());
        if (null == taskEntity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        operatorUtil.checkOperatorPermission(id);
        try {
            operatorService.back(id, flowModel);
        } catch (Exception e) {
            operatorUtil.compensate(taskEntity);
            throw e;
        }
        operatorUtil.handleOperator();
        operatorUtil.handleTaskStatus();
        operatorUtil.handleEvent();
        return ActionResult.success(MsgCode.WF002.get());
    }

    /**
     * 撤回
     *
     * @param id        记录主键
     * @param flowModel 参数
     */
    @Operation(summary = "撤回")
    @PostMapping("/Recall/{taskRecordId}")
    public ActionResult<String> recall(@PathVariable("taskRecordId") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        RecordEntity recordEntity = recordService.getInfo(id);
        if (null == recordEntity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        if (recordEntity.getStatus().equals(FlowNature.INVALID)) {
            throw new WorkFlowException(MsgCode.WF005.get());
        }
        OperatorEntity operator = operatorService.getInfo(recordEntity.getOperatorId());
        if (null == operator) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        TaskEntity taskEntity = taskService.getInfo(operator.getTaskId());
        if (null == taskEntity) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        flowModel.setRecordEntity(recordEntity);
        flowModel.setOperatorEntity(operator);
        try {
            operatorService.recall(id, flowModel);
        } catch (Exception e) {
            operatorUtil.compensate(taskEntity);
            throw e;
        }
        operatorUtil.handleOperator();
        operatorUtil.handleTaskStatus();
        flowUtil.event(flowModel, EventEnum.RECALL.getStatus());
        return ActionResult.success(MsgCode.WF008.get());
    }

    /**
     * 转审
     *
     * @param id        经办主键
     * @param flowModel 参数
     */
    @Operation(summary = "转审")
    @PostMapping("/Transfer/{id}")
    public ActionResult<String> transfer(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        operatorUtil.checkOperatorPermission(id);
        operatorService.transfer(id, flowModel);
        operatorUtil.autoAudit(flowModel);
        operatorUtil.launchTrigger(flowModel);
        operatorUtil.handleOperator();
        operatorUtil.handleEvent();
        OperatorEntity operator = operatorService.getById(id);
        return ActionResult.success(ObjectUtil.equals(operator.getIsProcessing(), FlowNature.PROCESSING) ? MsgCode.WF003.get() : MsgCode.WF152.get());
    }

    /**
     * 协办
     *
     * @param id        主键
     * @param flowModel 参数
     */
    @Operation(summary = "协办")
    @PostMapping("/Assist/{id}")
    public ActionResult<String> assist(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        operatorUtil.checkOperatorPermission(id);
        operatorService.assist(id, flowModel);
        return ActionResult.success(MsgCode.WF067.get());
    }

    /**
     * 协办保存
     *
     * @param id        主键
     * @param flowModel 参数
     */
    @Operation(summary = "协办保存")
    @PostMapping("/AssistSave/{id}")
    public ActionResult<String> assistSave(@PathVariable("id") String id, @RequestBody FlowModel flowModel) throws WorkFlowException {
        operatorUtil.checkOperatorPermission(id);
        operatorService.assistSave(id, flowModel);
        return ActionResult.success(MsgCode.WF068.get());
    }

    /**
     * 批量审批流程分类列表
     */
    @Operation(summary = "批量审批流程分类列表")
    @GetMapping("/BatchFlowSelector")
    public ActionResult<List<FlowBatchModel>> batchFlowSelector() {
        List<FlowBatchModel> list = operatorService.batchFlowSelector();
        return ActionResult.success(list);
    }

    /**
     * 批量审批流程版本列表
     *
     * @param templateId 流程定义主键
     */
    @Operation(summary = "批量审批流程版本列表")
    @GetMapping("/BatchVersionSelector/{templateId}")
    public ActionResult<List<FlowBatchModel>> batchVersionSelector(@PathVariable("templateId") String templateId) {
        List<FlowBatchModel> list = operatorService.batchVersionSelector(templateId);
        return ActionResult.success(list);
    }

    /**
     * 批量审批节点列表
     *
     * @param flowId 定义版本主键
     */
    @Operation(summary = "批量审批节点列表")
    @GetMapping("/BatchNodeSelector/{flowId}")
    public ActionResult<List<FlowBatchModel>> batchNodeSelector(@PathVariable("flowId") String flowId) {
        List<FlowBatchModel> list = operatorService.batchNodeSelector(flowId);
        return ActionResult.success(list);
    }

    /**
     * 批量审批节点属性
     *
     * @param flowModel 参数
     */
    @Operation(summary = "批量审批节点属性")
    @GetMapping("/BatchNode")
    public ActionResult<Object> batchNode(FlowModel flowModel) throws WorkFlowException {
        Map<String, Object> nodeMap = operatorService.batchNode(flowModel);
        return ActionResult.success(nodeMap);
    }

    /**
     * 批量获取候选人
     *
     * @param flowId     版本主键
     * @param operatorId 经办主键
     * @param batchType  类型，0.同意  1.拒绝
     */
    @Operation(summary = "批量获取候选人")
    @GetMapping("/BatchCandidate")
    public ActionResult<Object> batchCandidate(String flowId, String operatorId, Integer batchType) throws WorkFlowException {
        CandidateCheckVo vo = operatorService.batchCandidates(flowId, operatorId, batchType);
        return ActionResult.success(vo);
    }

    /**
     * 批量审批
     *
     * @param flowModel 参数
     */
    @Operation(summary = "批量审批")
    @PostMapping("/BatchOperation")
    public ActionResult<Object> batchOperation(@RequestBody FlowModel flowModel) throws WorkFlowException {
        try {
            operatorService.batch(flowModel);
        } catch (Exception e) {
            List<TaskEntity> taskList = flowModel.getTaskList();
            if (CollUtil.isNotEmpty(taskList)) {
                for (TaskEntity taskEntity : taskList) {
                    operatorUtil.compensate(taskEntity);
                }
            }
            throw e;
        }
        List<TaskEntity> taskList = flowModel.getTaskList();
        for (TaskEntity taskEntity : taskList) {
            FlowModel model = JsonUtil.getJsonToBean(flowModel, FlowModel.class);
            model.setTaskEntity(taskEntity);
            operatorUtil.launchTrigger(model);
        }
        operatorUtil.handleOperator();
        operatorUtil.handleTaskStatus();
        operatorUtil.handleEvent();
        return ActionResult.success(MsgCode.WF011.get());
    }

    /**
     * 消息跳转工作流
     *
     * @param id 经办或抄送主键
     */
    @Operation(summary = "消息跳转工作流")
    @GetMapping("/{id}/Info")
    public ActionResult<Object> checkInfo(@PathVariable("id") String id, @RequestParam(value = "opType", required = false) String opType) throws WorkFlowException {
        Map<String, String> map = new HashMap<>();
        List<String> list = ImmutableList.of(OpTypeEnum.LAUNCH_DETAIL.getType(), OpTypeEnum.LAUNCH_CREATE.getType());
        if (list.contains(opType)) {
            String type = OpTypeEnum.LAUNCH_DETAIL.getType();
            TaskEntity task = taskService.getById(id);
            if (null != task) {
                operatorUtil.checkTemplateHide(task.getTemplateId());
                if (ObjectUtil.equals(task.getStatus(), TaskStatusEnum.TO_BE_SUBMIT.getCode())) {
                    type = OpTypeEnum.LAUNCH_CREATE.getType();
                }
            }
            map.put("opType", type);
            return ActionResult.success(map);
        }
        String type = taskService.checkInfo(id);
        map.put("opType", type);
        return ActionResult.success(map);
    }

    /**
     * 节点记录列表
     *
     * @param taskId 任务主键
     * @param nodeId 节点id
     */
    @Operation(summary = "节点记录列表")
    @GetMapping("/RecordList")
    public ActionResult<Object> getRecordList(@RequestParam("taskId") String taskId, @RequestParam("nodeId") String nodeId) {
        return ActionResult.success(recordService.getList(taskId, nodeId));
    }

    /**
     * 节点抄送列表
     *
     * @param taskId 任务主键
     * @param nodeId 节点id
     */
    @Operation(summary = "节点抄送列表")
    @GetMapping("/CirculateList")
    public ActionResult<Object> getCirculateList(@RequestParam("taskId") String taskId, @RequestParam("nodeId") String nodeId) {
        List<CirculateEntity> list = circulateService.getNodeList(taskId, nodeId);
        return ActionResult.success(operatorUtil.getCirculateList(list));
    }
}

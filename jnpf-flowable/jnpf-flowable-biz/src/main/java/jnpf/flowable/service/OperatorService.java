package jnpf.flowable.service;

import jnpf.base.Pagination;
import jnpf.base.service.SuperService;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.model.candidates.CandidateCheckVo;
import jnpf.flowable.model.candidates.CandidateUserVo;
import jnpf.flowable.model.operator.FlowBatchModel;
import jnpf.flowable.model.operator.OperatorVo;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.task.TaskPagination;
import jnpf.flowable.model.templatenode.BackNodeModel;
import jnpf.model.FlowWorkListVO;
import jnpf.permission.model.user.WorkHandoverModel;

import java.util.List;
import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 15:29
 */
public interface OperatorService extends SuperService<OperatorEntity> {

    /**
     * 经办实体
     *
     * @param id 经办主键
     */
    OperatorEntity getInfo(String id) throws WorkFlowException;

    /**
     * 列表
     *
     * @param taskId 任务主键
     */
    List<OperatorEntity> getList(String taskId);

    /**
     * 列表
     *
     * @param pagination 参数
     */
    List<OperatorVo> getList(TaskPagination pagination);

    /**
     * 处理经办
     *
     * @param flowModel 参数
     */
    List<OperatorEntity> handleOperator(FlowModel flowModel) throws WorkFlowException;

    /**
     * 同意
     *
     * @param id        经办主键
     * @param flowModel 参数
     */
    void auditWithCheck(String id, FlowModel flowModel) throws WorkFlowException;

    /**
     * 同意
     *
     * @param id        经办主键
     * @param flowModel 参数
     */
    void audit(String id, FlowModel flowModel) throws WorkFlowException;

    /**
     * 同意（发起时自动通过）
     *
     * @param operator  经办实体
     * @param flowModel 参数
     */
    void audit(OperatorEntity operator, FlowModel flowModel) throws WorkFlowException;

    /**
     * 签收
     *
     * @param flowModel 参数，ids 、type 0 签收 1 退签
     */
    void sign(FlowModel flowModel) throws WorkFlowException;

    /**
     * 开始办理
     *
     * @param flowModel 参数，ids
     */
    void startHandle(FlowModel flowModel) throws WorkFlowException;

    /**
     * 暂存
     *
     * @param id        经办主键
     * @param flowModel 参数
     */
    void saveAudit(String id, FlowModel flowModel) throws WorkFlowException;

    /**
     * 加签
     *
     * @param id        经办主键
     * @param flowModel 参数
     */
    void addSign(String id, FlowModel flowModel) throws WorkFlowException;

    /**
     * 获取加签的人
     *
     * @param id         经办主键
     * @param pagination 参数
     */
    List<CandidateUserVo> getReduceList(String id, Pagination pagination) throws WorkFlowException;

    /**
     * 减签
     *
     * @param id        记录主键
     * @param flowModel 参数
     */
    void reduce(String id, FlowModel flowModel) throws WorkFlowException;

    /**
     * 获取退回的节点
     *
     * @param id 经办主键
     */
    List<BackNodeModel> getFallbacks(String id) throws WorkFlowException;

    /**
     * 退回
     *
     * @param id        经办主键
     * @param flowModel 参数
     */
    void back(String id, FlowModel flowModel) throws WorkFlowException;

    /**
     * 撤回
     *
     * @param id        记录主键
     * @param flowModel 参数
     */
    void recall(String id, FlowModel flowModel) throws WorkFlowException;

    /**
     * 转审
     *
     * @param id        经办主键
     * @param flowModel 参数
     */
    void transfer(String id, FlowModel flowModel) throws WorkFlowException;

    /**
     * 协办
     *
     * @param id        经办主键
     * @param flowModel 参数
     */
    void assist(String id, FlowModel flowModel) throws WorkFlowException;

    /**
     * 协办保存
     *
     * @param id        经办主键
     * @param flowModel 参数
     */
    void assistSave(String id, FlowModel flowModel) throws WorkFlowException;

    /**
     * 批量审批流程分类列表
     */
    List<FlowBatchModel> batchFlowSelector();

    /**
     * 批量审批流程版本列表
     *
     * @param templateId 流程定义主键
     */
    List<FlowBatchModel> batchVersionSelector(String templateId);

    /**
     * 批量审批节点列表
     *
     * @param flowId 流程定义版本主键
     */
    List<FlowBatchModel> batchNodeSelector(String flowId);

    /**
     * 批量审批节点属性
     *
     * @param flowModel 参数
     */
    Map<String, Object> batchNode(FlowModel flowModel) throws WorkFlowException;

    /**
     * 批量审批获取候选人
     *
     * @param flowId     版本主键
     * @param operatorId 经办主键
     * @param batchType  类型，0.同意  1.拒绝
     */
    CandidateCheckVo batchCandidates(String flowId, String operatorId, Integer batchType) throws WorkFlowException;

    /**
     * 批量审批
     *
     * @param flowModel 参数
     */
    void batch(FlowModel flowModel) throws WorkFlowException;

    /**
     * 流程交接
     *
     * @param fromId 移交人
     */
    FlowWorkListVO flowWork(String fromId);

    /**
     * 流程交接
     *
     * @param workHandoverModel 参数
     */
    boolean flowWork(WorkHandoverModel workHandoverModel);

}

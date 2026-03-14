package jnpf.flowable.service;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import jnpf.base.service.SuperService;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.model.candidates.CandidateCheckFo;
import jnpf.flowable.model.candidates.CandidateCheckVo;
import jnpf.flowable.model.candidates.CandidateUserVo;
import jnpf.flowable.model.monitor.MonitorVo;
import jnpf.flowable.model.task.*;
import jnpf.flowable.model.template.BeforeInfoVo;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/17 15:08
 */
public interface TaskService extends SuperService<TaskEntity> {

    TaskEntity getInfoSubmit(String id, SFunction<TaskEntity, ?>... columns);

    List<TaskEntity> getInfosSubmit(String[] ids, SFunction<TaskEntity, ?>... columns);

    /**
     * 我发起的 列表
     *
     * @param pagination 分页
     */
    List<TaskVo> getList(TaskPagination pagination);

    /**
     * 监控列表
     *
     * @param pagination 分页参数
     */
    List<MonitorVo> getMonitorList(TaskPagination pagination);

    /**
     * 任务实体
     *
     * @param id 任务主键
     */
    TaskEntity getInfo(String id) throws WorkFlowException;

    /**
     * 发起、审批详情
     *
     * @param id 任务id
     * @param fo 参数类
     */
    BeforeInfoVo getInfo(String id, FlowModel fo) throws WorkFlowException;

    /**
     * 获取能走的节点，判断候选人
     *
     * @param id 经办主键
     * @param fo 参数类
     */
    CandidateCheckVo checkCandidates(String id, CandidateCheckFo fo) throws WorkFlowException;

    /**
     * 获取候选人
     *
     * @param fo 参数类
     */
    List<CandidateUserVo> getCandidateUser(String id, CandidateCheckFo fo) throws WorkFlowException;

    /**
     * 暂存、提交
     *
     * @param flowModel 参数
     */
    void batchSaveOrSubmit(FlowModel flowModel) throws WorkFlowException;

    /**
     * 暂存、提交
     *
     * @param fo 参数类
     */
    void saveOrSubmit(FlowModel fo) throws WorkFlowException;

    /**
     * 发起撤回
     *
     * @param id        任务主键
     * @param flowModel 参数
     */
    void recall(String id, FlowModel flowModel) throws WorkFlowException;

    /**
     * 催办
     *
     * @param id 任务主键
     */
    boolean press(String id) throws WorkFlowException;

    /**
     * 撤销
     *
     * @param id 任务主键
     */
    void revoke(String id, FlowModel flowModel) throws WorkFlowException;

    /**
     * 删除
     *
     * @param id 主键
     */
    List<TaskEntity> delete(String id) throws WorkFlowException;

    /**
     * 删除
     *
     * @param ids 主键数组
     */
    void deleteBatch(List<String> ids) throws WorkFlowException;

    /**
     * 递归获取子流程
     *
     * @param idList 需要递归的任务主键集合
     * @param idAll  结果任务主键集合
     */
    void deleTaskAll(List<String> idList, List<String> idAll);

    /**
     * 终止、复活
     *
     * @param id        主键
     * @param flowModel 参数
     * @param isCancel  标识，true 终止
     */
    void cancel(String id, FlowModel flowModel, boolean isCancel) throws WorkFlowException;

    /**
     * 判断是否存在异步子流程
     *
     * @param id 任务主键
     */
    boolean checkAsync(String id);

    /**
     * 挂起、恢复
     *
     * @param id        主键
     * @param flowModel 参数
     * @param isSuspend 标识，true 挂起、false 恢复
     */
    void pause(String id, FlowModel flowModel, Boolean isSuspend) throws WorkFlowException;

    /**
     * 指派
     *
     * @param id        主键
     * @param flowModel 参数
     */
    void assign(String id, FlowModel flowModel) throws WorkFlowException;

    /**
     * 获取流程所关联的用户信息
     *
     * @param taskId 任务主键
     */
    TaskUserListModel getTaskUserList(String taskId);

    /**
     * 子流程详情
     *
     * @param flowModel 参数
     */
    List<BeforeInfoVo> subFlowInfo(FlowModel flowModel) throws WorkFlowException;

    /**
     * 消息跳转流程校验
     *
     * @param id 经办或抄送主键
     */
    String checkInfo(String id) throws WorkFlowException;

    /**
     * 更新归档状态
     *
     * @param taskId 任务主键
     */
    void updateIsFile(String taskId) throws WorkFlowException;

    /**
     * 获取发起表单
     *
     * @param taskId 任务主键
     */
    ViewFormModel getStartForm(String taskId) throws WorkFlowException;

    /**
     * 获取流程数量
     */
    TaskTo getFlowTodoCount(TaskTo taskTo);

    /**
     * 获取待办事项
     */
    FlowTodoVO getFlowTodo(TaskPagination pagination);
}

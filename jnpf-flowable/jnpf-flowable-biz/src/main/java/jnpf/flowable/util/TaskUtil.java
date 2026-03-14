package jnpf.flowable.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.ImmutableList;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.OperatorStateEnum;
import jnpf.flowable.enums.TaskStatusEnum;
import jnpf.flowable.mapper.*;
import jnpf.flowable.model.util.FlowNature;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskUtil {

    
    private final TaskMapper taskMapper;
    
    private final LaunchUserMapper launchUserMapper;
    
    private final CandidatesMapper candidatesMapper;
    
    private final OperatorMapper operatorMapper;
    
    private final RecordMapper recordMapper;
    
    private final CirculateMapper circulateMapper;
    
    private final CommentMapper commentMapper;
    
    private final RejectDataMapper rejectDataMapper;
    
    private final NodeRecordMapper nodeRecordMapper;
    
    private final TaskLineMapper taskLineMapper;
    
    private final RevokeMapper revokeMapper;
    
    private final TriggerTaskMapper triggerTaskMapper;
    
    private final TriggerRecordMapper triggerRecordMapper;
    
    private final EventLogMapper eventLogMapper;
    
    private final TriggerLaunchflowMapper triggerLaunchflowMapper;


    public List<TaskEntity> delete(List<String> ids, boolean isBatch) throws WorkFlowException {
        List<TaskEntity> taskList = taskMapper.getOrderStaList(ids);
        List<TaskEntity> taskStatus = taskList.stream()
                .filter(t -> TaskStatusEnum.PAUSED.getCode().equals(t.getStatus())).collect(Collectors.toList());
        if (!taskStatus.isEmpty()) {
            throw new WorkFlowException(isBatch ? taskStatus.get(0).getFullName() + MsgCode.WF113.get() : MsgCode.WF114.get());
        }
        if (!isBatch) {
            List<Integer> status = ImmutableList.of(TaskStatusEnum.TO_BE_SUBMIT.getCode(), TaskStatusEnum.RECALL.getCode());
            List<TaskEntity> taskStateList = taskList.stream().filter(e -> !status.contains(e.getStatus())).collect(Collectors.toList());
            if (!taskStateList.isEmpty()) {
                throw new WorkFlowException(MsgCode.WF063.get());
            }
        }

        List<TaskEntity> child = taskList.stream()
                .filter(t -> StringUtil.isNotEmpty(t.getParentId()) && !FlowNature.PARENT_ID.equals(t.getParentId())).collect(Collectors.toList());
        if (!child.isEmpty()) {
            throw new WorkFlowException(child.get(0).getFullName() + MsgCode.WF021.get());
        }

        for (TaskEntity entity : taskList) {
            List<TriggerLaunchflowEntity> triggerList = triggerLaunchflowMapper.getTaskIds(entity.getId());
            if (!triggerList.isEmpty()) {
                throw new WorkFlowException(entity.getFullName() + MsgCode.WF157.get());
            }
        }
        delete(ids);
        return taskList;
    }

    public void delete(List<String> idList) {
        List<String> idAll = new ArrayList<>();
        taskMapper.deleTaskAll(idList, idAll);
        List<String> revokeTaskIds = revokeMapper.getByTaskId(idAll);
        idAll.addAll(revokeTaskIds);
        if (!idAll.isEmpty()) {
            QueryWrapper<TaskEntity> task = new QueryWrapper<>();
            task.lambda().in(TaskEntity::getId, idAll);
            taskMapper.deleteByIds(taskMapper.selectList(task));
            // 候选人
            QueryWrapper<CandidatesEntity> candidates = new QueryWrapper<>();
            candidates.lambda().select(CandidatesEntity::getId);
            candidates.lambda().in(CandidatesEntity::getTaskId, idAll);
            candidatesMapper.deleteByIds(candidatesMapper.selectList(candidates));
            // 发起人
            QueryWrapper<LaunchUserEntity> launchUser = new QueryWrapper<>();
            launchUser.lambda().select(LaunchUserEntity::getId);
            launchUser.lambda().in(LaunchUserEntity::getTaskId, idAll);
            launchUserMapper.deleteByIds(launchUserMapper.selectList(launchUser));
            // 评论
            QueryWrapper<CommentEntity> comment = new QueryWrapper<>();
            comment.lambda().select(CommentEntity::getId);
            comment.lambda().in(CommentEntity::getTaskId, idAll);
            commentMapper.deleteByIds(commentMapper.selectList(comment));
            // 经办
            QueryWrapper<OperatorEntity> operator = new QueryWrapper<>();
            operator.lambda().select(OperatorEntity::getId);
            operator.lambda().in(OperatorEntity::getTaskId, idAll);
            operatorMapper.deleteByIds(operatorMapper.selectList(operator));
            // 记录
            QueryWrapper<RecordEntity> recordEntityQueryWrapper = new QueryWrapper<>();
            recordEntityQueryWrapper.lambda().select(RecordEntity::getId);
            recordEntityQueryWrapper.lambda().in(RecordEntity::getTaskId, idAll);
            recordMapper.deleteByIds(recordMapper.selectList(recordEntityQueryWrapper));
            // 退回信息
            List<TaskEntity> taskList = taskMapper.selectList(task);
            List<String> rejectDataIds = taskList.stream().map(TaskEntity::getRejectDataId).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(rejectDataIds)) {
                QueryWrapper<RejectDataEntity> rejectData = new QueryWrapper<>();
                rejectData.lambda().select(RejectDataEntity::getId);
                rejectData.lambda().in(RejectDataEntity::getId, rejectDataIds);
                rejectDataMapper.deleteByIds(rejectDataMapper.selectList(rejectData));
            }
            // 抄送
            QueryWrapper<CirculateEntity> circulate = new QueryWrapper<>();
            circulate.lambda().select(CirculateEntity::getId);
            circulate.lambda().in(CirculateEntity::getTaskId, idAll);
            circulateMapper.deleteByIds(circulateMapper.selectList(circulate));
            // 任务条件
            QueryWrapper<TaskLineEntity> taskLine = new QueryWrapper<>();
            taskLine.lambda().select(TaskLineEntity::getId);
            taskLine.lambda().in(TaskLineEntity::getTaskId, idAll);
            taskLineMapper.deleteByIds(taskLineMapper.selectList(taskLine));
            // 撤销
            QueryWrapper<RevokeEntity> revoke = new QueryWrapper<>();
            revoke.lambda().select(RevokeEntity::getId);
            revoke.lambda().in(RevokeEntity::getRevokeTaskId, idAll);
            revokeMapper.deleteByIds(revokeMapper.selectList(revoke));
            //节点流转
            QueryWrapper<NodeRecordEntity> nodeRecord = new QueryWrapper<>();
            nodeRecord.lambda().select(NodeRecordEntity::getId);
            nodeRecord.lambda().in(NodeRecordEntity::getTaskId, idAll);
            nodeRecordMapper.deleteByIds(nodeRecordMapper.selectList(nodeRecord));
            //外部事件
            QueryWrapper<EventLogEntity> eventLog = new QueryWrapper<>();
            eventLog.lambda().select(EventLogEntity::getId);
            eventLog.lambda().in(EventLogEntity::getTaskId, idAll);
            eventLogMapper.deleteByIds(eventLogMapper.selectList(eventLog));
            // 任务流程
            QueryWrapper<TriggerTaskEntity> triggerTask = new QueryWrapper<>();
            triggerTask.lambda().select(TriggerTaskEntity::getId);
            triggerTask.lambda().and(t -> t.in(TriggerTaskEntity::getTaskId, idAll).or().in(TriggerTaskEntity::getId, idAll));
            List<TriggerTaskEntity> list = triggerTaskMapper.selectList(triggerTask);
            List<String> ids = list.stream().map(TriggerTaskEntity::getId).collect(Collectors.toList());
            if (!ids.isEmpty()) {
                triggerTaskMapper.deleteByIds(list);
                QueryWrapper<TriggerRecordEntity> triggerRecord = new QueryWrapper<>();
                triggerRecord.lambda().select(TriggerRecordEntity::getId);
                triggerRecord.lambda().in(TriggerRecordEntity::getTriggerId, ids);
                triggerRecordMapper.deleteByIds(triggerRecordMapper.selectList(triggerRecord));
                QueryWrapper<TriggerLaunchflowEntity> triggerLaunch = new QueryWrapper<>();
                triggerLaunch.lambda().select(TriggerLaunchflowEntity::getId);
                triggerLaunch.lambda().in(TriggerLaunchflowEntity::getTriggerId, ids);
                triggerLaunchflowMapper.deleteByIds(triggerLaunchflowMapper.selectList(triggerLaunch));
            }
        }
    }


    // 归档 当前流程所有人：包含流程的发起人、所有节点的审批人及抄送人员，不包含加签和转审人员；（审批人读取参与审批的人）
    public List<String> getListOfFile(String taskId) {
        List<String> resList = new ArrayList<>();
        List<OperatorEntity> operatorList = operatorMapper.getList(taskId);
        if (CollUtil.isNotEmpty(operatorList)) {
            List<Integer> status = ImmutableList.of(OperatorStateEnum.ADD_SIGN.getCode(), OperatorStateEnum.TRANSFER.getCode(),
                    OperatorStateEnum.ASSIST.getCode(), OperatorStateEnum.FUTILITY.getCode());
            List<OperatorEntity> list = operatorList.stream()
                    .filter(e -> !status.contains(e.getStatus()) && e.getHandleStatus() != null)
                    .collect(Collectors.toList());

            resList.addAll(list.stream().map(OperatorEntity::getHandleId).collect(Collectors.toList()));
        }
        List<CirculateEntity> circulateList = circulateMapper.getList(taskId);
        if (CollUtil.isNotEmpty(circulateList)) {
            resList.addAll(circulateList.stream().map(CirculateEntity::getUserId).collect(Collectors.toList()));
        }
        return resList.stream().distinct().collect(Collectors.toList());
    }

    // 归档 最后节点审批人：表示最后节点的审批人才有权限查看该文档（取最后节点实际参与审批的人，不包含加签和转审人员）；
    public List<String> getListOfLast(String taskId) {
        List<String> resList = new ArrayList<>();

        List<Integer> status = ImmutableList.of(OperatorStateEnum.ADD_SIGN.getCode(), OperatorStateEnum.TRANSFER.getCode(),
                OperatorStateEnum.ASSIST.getCode(), OperatorStateEnum.FUTILITY.getCode());

        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(OperatorEntity::getTaskId, taskId)
                .ne(OperatorEntity::getHandleId, FlowNature.SYSTEM_CODE)
                .notIn(OperatorEntity::getStatus, status)
                .isNotNull(OperatorEntity::getHandleStatus).orderByDesc(OperatorEntity::getHandleTime);

        List<OperatorEntity> list = operatorMapper.selectList(queryWrapper);
        if (CollUtil.isNotEmpty(list)) {
            // 获取符合条件的最后一个审批人，通过该审批人获取节点
            OperatorEntity operator = list.get(0);
            List<OperatorEntity> operatorList = list.stream()
                    .filter(e -> ObjectUtil.equals(operator.getNodeId(), e.getNodeId()))
                    .sorted(Comparator.comparing(OperatorEntity::getHandleTime).reversed()).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(operatorList)) {
                List<String> handleIds = operatorList.stream().map(OperatorEntity::getHandleId).distinct().collect(Collectors.toList());
                resList.addAll(handleIds);
            }
            List<CirculateEntity> circulateList = circulateMapper.getList(taskId);
            if (CollUtil.isNotEmpty(circulateList)) {
                List<String> userIds = circulateList.stream().filter(e -> ObjectUtil.equals(operator.getNodeId(), e.getNodeId()))
                        .map(CirculateEntity::getUserId).collect(Collectors.toList());
                resList.addAll(userIds);
            }
        }
        return resList.stream().distinct().collect(Collectors.toList());
    }

}

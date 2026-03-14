package jnpf.flowable.util;

import cn.hutool.core.util.ObjectUtil;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.enums.OperatorStateEnum;
import jnpf.flowable.enums.TemplateStatueEnum;
import jnpf.flowable.mapper.OperatorMapper;
import jnpf.flowable.mapper.TaskMapper;
import jnpf.flowable.mapper.TemplateMapper;
import jnpf.flowable.model.message.FlowMsgModel;
import jnpf.flowable.model.time.FlowTimeModel;
import jnpf.flowable.model.util.FlowNature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/5/23 上午10:33
 */
@Component
@RequiredArgsConstructor
public class OverTimeUtil {
    
    private final MsgUtil msgUtil;
    
    private final FlowUtil flowUtil;
    
    private final TaskMapper taskMapper;
    
    private final OperatorMapper operatorMapper;
    
    private final TemplateMapper templateMapper;

    public void overMsg(FlowTimeModel flowTimeModel) throws WorkFlowException {
        TaskEntity task = taskMapper.getInfo(flowTimeModel.getTaskId());
        //暂停，是跳过
        if (task == null) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        boolean isSuccess = false;
        try {
            flowUtil.isSuspend(task);
        } catch (Exception e) {
            isSuccess = true;
        }
        if (!isSuccess) {
            // 判断是否下架
            TemplateEntity template = templateMapper.selectById(task.getTemplateId());
            if (null != template && !ObjectUtil.equals(template.getStatus(), TemplateStatueEnum.UP.getCode())) {
                isSuccess = true;
            }
        }
        if (isSuccess) {
            if (Boolean.TRUE.equals(flowTimeModel.getOverTime())) {
                flowTimeModel.setNum(flowTimeModel.getNum() + 1);
                flowTimeModel.setTransferNum(flowTimeModel.getTransferNum() + 1);
            }
            flowTimeModel.setIsPause(true);
            return;
        }
        flowUtil.isCancel(task);
        OperatorEntity operator = operatorMapper.selectById(flowTimeModel.getOperatorId());
        if (operator == null || ObjectUtil.equals(operator.getStatus(), OperatorStateEnum.FUTILITY.getCode())) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        if (operator.getHandleStatus() != null) {
            throw new WorkFlowException(MsgCode.WF031.get());
        }
        // complete为1的经办，不执行（加签、指派、转办、驳回、撤回、变更、复活流程，该节点的限时提醒规则重新生效）
        if (operator.getCompletion().equals(FlowNature.ACTION)) {
            throw new WorkFlowException(MsgCode.FA001.get());
        }
        if (Boolean.TRUE.equals(flowTimeModel.getOverTime()) && operator.getDuedate() == null) {
                operator.setDuedate(new Date());
                operatorMapper.updateById(operator);
            }

        List<OperatorEntity> list = new ArrayList<>();
        list.add(operator);
        FlowMsgModel msgModel = new FlowMsgModel();
        msgModel.setUserInfo(flowTimeModel.getFlowModel().getUserInfo());
        msgModel.setTaskEntity(task);
        msgModel.setNodeList(flowTimeModel.getFlowModel().getNodeEntityList());
        msgModel.setNodeCode(operator.getNodeCode());
        msgModel.setOperatorList(list);
        msgModel.setNotice(!flowTimeModel.getOverTime());
        msgModel.setOvertime(flowTimeModel.getOverTime());
        msgModel.setWait(false);
        msgUtil.message(msgModel);
        if (Boolean.TRUE.equals(flowTimeModel.getOverTime())) {
            flowTimeModel.setNum(flowTimeModel.getNum() + 1);
            flowTimeModel.setTransferNum(flowTimeModel.getTransferNum() + 1);
        }
    }

}

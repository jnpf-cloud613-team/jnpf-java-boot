package jnpf.flowable.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import jnpf.base.UserInfo;
import jnpf.config.ConfigValueUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.enums.OperatorStateEnum;
import jnpf.flowable.model.task.FlowMethod;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.time.FlowTimeModel;
import jnpf.flowable.model.util.FlowNature;
import jnpf.flowable.service.OperatorService;
import jnpf.flowable.service.TaskService;
import jnpf.flowable.util.*;
import jnpf.permission.entity.UserEntity;
import jnpf.util.AuthUtil;
import jnpf.util.RedisUtil;
import jnpf.util.UserProvider;
import jnpf.util.context.SpringContext;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/23 17:42
 */
@Slf4j
public class AutoAuditJob extends QuartzJobBean {

    private static RedisUtil redisUtil;
    private static ConditionUtil conditionUtil;
    private static OperatorUtil operatorUtil;
    private static FlowUtil flowUtil;
    private static ServiceUtil serviceUtil;
    private static ConfigValueUtil configValueUtil;
    private static OperatorService operatorService;
    private static TaskService taskService;
    private static RedisLock redisLock;

    static {
        redisUtil = SpringContext.getBean(RedisUtil.class);
        configValueUtil = SpringContext.getBean(ConfigValueUtil.class);
        operatorService = SpringContext.getBean(OperatorService.class);
        conditionUtil = SpringContext.getBean(ConditionUtil.class);
        operatorUtil = SpringContext.getBean(OperatorUtil.class);
        flowUtil = SpringContext.getBean(FlowUtil.class);
        taskService = SpringContext.getBean(TaskService.class);
        serviceUtil = SpringContext.getBean(ServiceUtil.class);
        redisLock = SpringContext.getBean(RedisLock.class);
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        List<FlowTimeModel> list = FlowJobUtil.getOperator(redisUtil);
        if (CollUtil.isNotEmpty(list)) {
            for (FlowTimeModel timeModel : list) {
                FlowModel flowModel = timeModel.getFlowModel();
                Map<String, Object> formData = flowModel.getFormData();
                String deploymentId = flowModel.getDeploymentId();
                Map<String, NodeModel> nodes = flowModel.getNodes();

                UserInfo userInfo = flowModel.getUserInfo();
                if (configValueUtil.isMultiTenancy()) {
                    TenantDataSourceUtil.switchTenant(userInfo.getTenantId());
                }

                String operatorId = timeModel.getOperatorId();

                boolean lock = redisLock.lock("autoAudit-" + operatorId, operatorId, 5, TimeUnit.SECONDS);
                if (lock) {
                    TaskEntity taskEntity = taskService.getById(timeModel.getTaskId());
                    if (null == taskEntity) {
                        taskEntity = flowModel.getTaskEntity();
                    }

                    try {
                        OperatorEntity operator = operatorService.getInfo(operatorId);
                        if (null != operator) {
                            if (ObjectUtil.equals(operator.getCompletion(), FlowNature.ACTION) || ObjectUtil.equals(operator.getStatus(), OperatorStateEnum.WAITING.getCode())) {
                                continue;
                            }
                            flowModel.setTaskEntity(taskEntity);
                            if (operator.getSignTime() == null) {
                                operator.setSignTime(new Date());
                            }
                            if (operator.getStartHandleTime() == null) {
                                operator.setStartHandleTime(new Date());
                            }
                            FlowMethod flowMethod = new FlowMethod();
                            flowMethod.setTaskEntity(taskEntity);
                            flowMethod.setFormData(formData == null ? new HashMap<>() : formData);
                            flowMethod.setDeploymentId(deploymentId);
                            flowMethod.setNodeCode(operator.getNodeCode());
                            flowMethod.setNodes(nodes);
                            // 判断节点的线的条件
                            Map<String, Boolean> resMap = conditionUtil.handleCondition(flowMethod);
                            List<NodeModel> nextApprover = new ArrayList<>();
                            boolean next = true;
                            try {
                                nextApprover.addAll(flowUtil.getNextApprover(flowMethod));
                                conditionUtil.checkCondition(resMap, nodes);
                            } catch (WorkFlowException e) {
                                next = false;
                            }
                            if (next) {
                                NodeModel nodeModel = nodes.get(operator.getNodeCode());
                                if (!flowUtil.checkBranch(nodeModel) && flowUtil.checkNextCandidates(nextApprover) && flowUtil.checkNextError(flowModel, nextApprover, false, false) == 0) {
                                    String handleId = operator.getHandleId();
                                    String username = "";
                                    UserEntity user = serviceUtil.getUserInfo(handleId);
                                    if (user != null) {
                                        username = user.getAccount() + "(" + user.getRealName() + ")";
                                        userInfo.setUserId(user.getId());
                                        userInfo.setUserName(user.getRealName());
                                        userInfo.setUserAccount(user.getAccount());
                                        String token = AuthUtil.loginTempUser(handleId, userInfo.getTenantId());
                                        userInfo.setToken(token);
                                        UserProvider.setLoginUser(userInfo);
                                        UserProvider.setLocalLoginUser(userInfo);
                                    }
                                    String str = ObjectUtil.equals(operator.getIsProcessing(), FlowNature.NOT_PROCESSING) ? "自动审批通过" : "自动办理通过";
                                    flowModel.setHandleOpinion(username + str);
                                    flowModel.setFreeFlowConfig(null);
                                    //自动审批，处理签收、办理时间
                                    operatorService.updateById(operator);
                                    operatorService.auditWithCheck(operator.getId(), flowModel);
                                    operatorUtil.handleEvent();
                                    operatorUtil.handleTaskStatus();
                                    operatorUtil.handleOperator();
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("超时自动审批异常", e);
                        try {
                            operatorUtil.compensate(taskEntity);
                        } catch (Exception ex) {
                            log.error("超时自动审批补偿异常", ex);
                        }
                    } finally {
                        FlowJobUtil.remove(timeModel, redisUtil);
                    }
                }
            }
        }

    }
}

package jnpf.flowable.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.ActionResult;
import jnpf.base.UserInfo;
import jnpf.config.ConfigValueUtil;
import jnpf.database.util.TenantDataSourceUtil;
import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.entity.RecordEntity;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.enums.OperatorEnum;
import jnpf.flowable.enums.OperatorStateEnum;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.templatenode.nodejson.TemplateJsonModel;
import jnpf.flowable.model.templatenode.nodejson.TimeConfig;
import jnpf.flowable.model.time.FlowTimeModel;
import jnpf.flowable.service.OperatorService;
import jnpf.flowable.service.TaskService;
import jnpf.flowable.util.*;
import jnpf.permission.entity.UserEntity;
import jnpf.util.*;
import jnpf.util.context.SpringContext;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/12/10 9:17
 */
@Slf4j
public class AutoTransferJob extends QuartzJobBean {
    private static RedisUtil redisUtil;
    private static ConfigValueUtil configValueUtil;
    private static OperatorService operatorService;
    private static TaskService taskService;
    private static ServiceUtil serviceUtil;
    private static FlowUtil flowUtil;
    private static RedisLock redisLock;
    private static OperatorUtil operatorUtil;

    static {
        redisUtil = SpringContext.getBean(RedisUtil.class);
        configValueUtil = SpringContext.getBean(ConfigValueUtil.class);
        operatorService = SpringContext.getBean(OperatorService.class);
        taskService = SpringContext.getBean(TaskService.class);
        serviceUtil = SpringContext.getBean(ServiceUtil.class);
        flowUtil = SpringContext.getBean(FlowUtil.class);
        redisLock = SpringContext.getBean(RedisLock.class);
        operatorUtil = SpringContext.getBean(OperatorUtil.class);
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        List<FlowTimeModel> list = FlowJobUtil.getTransfer(redisUtil);
        if (CollUtil.isNotEmpty(list)) {
            Set<String> nodeCodes = new TreeSet<>();
            for (FlowTimeModel timeModel : list) {
                FlowModel flowModel = timeModel.getFlowModel();

                UserInfo userInfo = flowModel.getUserInfo();
                if (configValueUtil.isMultiTenancy()) {
                    TenantDataSourceUtil.switchTenant(userInfo.getTenantId());
                }

                String operatorId = timeModel.getOperatorId();

                boolean lock = redisLock.lock("transfer-" + operatorId, operatorId, 5, TimeUnit.SECONDS);
                if (lock) {
                    String nodeCode = flowModel.getNodeCode();

                    Map<String, Object> formData = flowModel.getFormData();
                    Map<String, NodeModel> nodes = flowModel.getNodes();

                    NodeModel nodeModel = nodes.get(nodeCode);
                    TimeConfig config = nodeModel.getOverTimeConfig();
                    OperatorEntity operator = operatorService.getById(operatorId);
                    if (null == operator || operator.getHandleStatus() != null) {
                        TimeUtil.deleteJob(timeModel.getId());
                        FlowJobUtil.remove(timeModel, redisUtil);
                        continue;
                    }
                    boolean next = true;
                    if (ObjectUtil.equals(config.getOverTimeType(), OperatorEnum.NOMINATOR.getCode()) || ObjectUtil.equals(config.getOverTimeType(), OperatorEnum.SERVE.getCode())) {
                        if (nodeCodes.contains(flowModel.getNodeCode())) {
                            log.info("自动转审失败，该经办已处理（id：" + operatorId + "，code：" + nodeCode + "）");
                            FlowJobUtil.removeTransfer(timeModel, redisUtil);
                            next = false;
                        }
                        nodeCodes.add(nodeCode);
                    }

                    if (ObjectUtil.equals(operator.getStatus(), OperatorStateEnum.TRANSFER.getCode())) {
                        log.info("转审状态的经办，不执行: " + operator.getId());
                        FlowJobUtil.removeTransfer(timeModel, redisUtil);
                        next = false;
                    }
                    // 指派类型的转审，判断整个节点 是否存在转审类型的经办
                    if (ObjectUtil.equals(config.getOverTimeType(), OperatorEnum.NOMINATOR.getCode()) || ObjectUtil.equals(config.getOverTimeType(), OperatorEnum.SERVE.getCode())) {
                        QueryWrapper<OperatorEntity> queryWrapper = new QueryWrapper<>();
                        queryWrapper.lambda().eq(OperatorEntity::getTaskId, operator.getTaskId()).eq(OperatorEntity::getNodeId, operator.getNodeId())
                                .eq(OperatorEntity::getStatus, OperatorStateEnum.TRANSFER.getCode());
                        long count = operatorService.count(queryWrapper);
                        if (count > 0) {
                            log.info("节点存在转审状态的经办，不执行: " + operator.getId() + " " + operator.getNodeId());
                            FlowJobUtil.removeTransfer(timeModel, redisUtil);
                            next = false;
                        }
                    }


                    String userId = operator.getHandleId();
                    UserEntity user = serviceUtil.getUserInfo(userId);
                    if (null != user) {
                        String token = AuthUtil.loginTempUser(userId, userInfo.getTenantId());
                        userInfo.setUserId(user.getId());
                        userInfo.setUserName(user.getRealName());
                        userInfo.setUserAccount(user.getAccount());
                        userInfo.setToken(token);
                        UserProvider.setLoginUser(userInfo);
                        UserProvider.setLocalLoginUser(userInfo);
                    }

                    try {
                        if (next) {
                            TaskEntity taskEntity = taskService.getById(operator.getTaskId());
                            if (null == taskEntity) {
                                taskEntity = flowModel.getTaskEntity();
                            }
                            String handleIds = null;
                            if (ObjectUtil.equals(config.getOverTimeType(), OperatorEnum.NOMINATOR.getCode())) {
                                // 指定人员
                                // 349057407209541--user
                                if (CollUtil.isNotEmpty(config.getReApprovers())) {
                                    String handleId = config.getReApprovers().get(0).split("--")[0];
                                    UserEntity userEntity = serviceUtil.getUserInfo(handleId);
                                    if (!ObjectUtil.equals(userId, handleId) && null != userEntity && ObjectUtil.equals(userEntity.getEnabledMark(), 1)) {
                                        handleIds = handleId;
                                    }
                                }
                                if (StringUtil.isNotEmpty(handleIds)) {
                                    flowModel.setHandleIds(handleIds);
                                    flowModel.setAutoTransferFlag(true);
                                    taskService.assign(operator.getTaskId(), flowModel);
                                    this.delete(timeModel);
                                }
                            } else if (ObjectUtil.equals(config.getOverTimeType(), OperatorEnum.SERVE.getCode())) {
                                // 接口
                                String interfaceId = config.getInterfaceId();
                                List<TemplateJsonModel> templateJson = config.getTemplateJson();
                                if (StringUtil.isNotEmpty(interfaceId)) {
                                    RecordEntity recordEntity = new RecordEntity();
                                    recordEntity.setTaskId(taskEntity.getId());
                                    recordEntity.setNodeCode(operator.getNodeCode());
                                    recordEntity.setHandleId(operator.getHandleId());
                                    FlowModel parameterModel = new FlowModel();
                                    parameterModel.setFormData(formData);
                                    parameterModel.setRecordEntity(recordEntity);
                                    parameterModel.setTaskEntity(taskEntity);
                                    Map<String, String> parameterMap = flowUtil.parameterMap(parameterModel, templateJson);
                                    ActionResult<Object> result = serviceUtil.infoToId(interfaceId, parameterMap);
                                    if (Objects.equals(200, result.getCode())) {
                                        Object data = result.getData();
                                        if (data instanceof Map) {
                                            JSONObject map = new JSONObject((Map) data);
                                            List<String> handleId = StringUtil.isNotEmpty(map.getString("handleId")) ? Arrays.asList(map.getString("handleId").split(",")) : new ArrayList<>();
                                            handleId = serviceUtil.getUserName(handleId, true)
                                                    .stream().map(UserEntity::getId).filter(e -> !ObjectUtil.equals(userId, e)).sorted().collect(Collectors.toList());
                                            handleIds = CollUtil.isNotEmpty(handleId) ? handleId.get(0) : null;
                                        }
                                    }
                                    if (StringUtil.isNotEmpty(handleIds)) {
                                        flowModel.setHandleIds(handleIds);
                                        flowModel.setAutoTransferFlag(true);
                                        taskService.assign(operator.getTaskId(), flowModel);
                                        this.delete(timeModel);
                                    }
                                }
                            } else {
                                // 超时审批人，2.同一部门 7.同一角色 3.同一岗位 8.同一分组
                                UserEntity userEntity = serviceUtil.getUserInfo(userId);
                                if (null != userEntity) {
                                    Integer overTimeExtraRule = config.getOverTimeExtraRule();
                                    List<String> userIds = new ArrayList<>();
                                    operatorUtil.getByRule(userIds, userEntity, overTimeExtraRule);
                                    userIds = serviceUtil.getUserName(userIds, true)
                                            .stream().map(UserEntity::getId).filter(e -> !ObjectUtil.equals(userId, e)).sorted().collect(Collectors.toList());
                                    if (CollUtil.isNotEmpty(userIds)) {
                                        handleIds = userIds.get(0);
                                    }
                                }
                                if (StringUtil.isNotEmpty(handleIds)) {
                                    flowModel.setHandleIds(handleIds);
                                    if (operator.getSignTime() == null) {
                                        operator.setSignTime(new Date());
                                    }
                                    if (operator.getStartHandleTime() == null) {
                                        operator.setStartHandleTime(new Date());
                                    }
                                    operatorService.updateById(operator);
                                    flowModel.setAutoTransferFlag(true);
                                    operatorService.transfer(operatorId, flowModel);
                                    this.delete(timeModel);
                                    operatorUtil.handleOperator();
                                    operatorUtil.launchTrigger(flowModel);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("超时自动转审异常", e);
                        this.delete(timeModel);
                    } finally {
                        FlowJobUtil.removeTransfer(timeModel, redisUtil);
                    }
                }
            }
        }
    }

    private void delete(FlowTimeModel timeModel) {
        TimeUtil.deleteJob(timeModel.getId());
        FlowJobUtil.remove(timeModel, redisUtil);
    }
}

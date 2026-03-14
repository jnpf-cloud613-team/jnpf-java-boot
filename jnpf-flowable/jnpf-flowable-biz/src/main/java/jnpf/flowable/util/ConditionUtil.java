package jnpf.flowable.util;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.ImmutableList;
import jnpf.constant.MsgCode;
import jnpf.exception.WorkFlowException;
import jnpf.flowable.entity.TaskEntity;
import jnpf.flowable.enums.DivideRuleEnum;
import jnpf.flowable.enums.NodeEnum;
import jnpf.flowable.model.flowable.FlowAbleUrl;
import jnpf.flowable.model.flowable.OutgoingFlowsFo;
import jnpf.flowable.model.task.FlowMethod;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.templatenode.nodejson.ProperCond;
import jnpf.permission.entity.UserEntity;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/19 15:14
 */
@Component
@RequiredArgsConstructor
public class ConditionUtil {

    private final ServiceUtil serviceUtil;

    private final FlowAbleUrl flowAbleUrl;

    // 处理选择分支的条件
    public Map<String, Boolean> getForBranch(FlowMethod flowMethod, List<String> branchList) throws WorkFlowException {
        Map<String, Boolean> resMap = new HashMap<>();

        String deploymentId = flowMethod.getDeploymentId();
        String nodeCode = flowMethod.getNodeCode();
        Map<String, NodeModel> nodes = flowMethod.getNodes();
        NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
        List<String> connectList = global.getConnectList();
        List<String> typeList = ImmutableList.of(NodeEnum.TRIGGER.getType());

        OutgoingFlowsFo flowsFo = new OutgoingFlowsFo();
        flowsFo.setDeploymentId(deploymentId);
        flowsFo.setTaskKey(nodeCode);
        List<String> outgoingFlows = flowAbleUrl.getOutgoingFlows(flowsFo);

        for (String flow : outgoingFlows) {
            resMap.put(flow, false);
            if (!connectList.contains(flow)) {
                // 不存在connectList中，说明是隐藏的线，默认给true
                resMap.put(flow, true);
                continue;
            }
            List<String> nodeKey = flowAbleUrl.getTaskKeyAfterFlow(deploymentId, flow);
            if (CollUtil.isNotEmpty(nodeKey)) {
                NodeModel nodeModel = nodes.get(nodeKey.get(0));
                if ((null != nodeModel && typeList.contains(nodeModel.getType()))||branchList.contains(nodeKey.get(0))) {
                    resMap.put(flow, true);
                }
            }
        }

        return resMap;
    }

    /**
     * 处理条件
     */
    public Map<String, Boolean> handleCondition(FlowMethod flowMethod) throws WorkFlowException {
        String deploymentId = flowMethod.getDeploymentId();
        String nodeCode = flowMethod.getNodeCode();
        // 获取节点的出线
        OutgoingFlowsFo flowsFo = new OutgoingFlowsFo();
        flowsFo.setDeploymentId(deploymentId);
        flowsFo.setTaskKey(nodeCode);
        List<String> outgoingFlows = flowAbleUrl.getOutgoingFlows(flowsFo);

        Map<String, Boolean> resMap = new HashMap<>();
        flowMethod.setResMap(resMap);
        flowMethod.setOutgoingFlows(outgoingFlows);
        // 判断条件
        getConditionResult(flowMethod);
        return resMap;
    }

    /**
     * 获取条件结果
     */
    public void getConditionResult(FlowMethod flowMethod) {
        List<String> outgoingFlows = flowMethod.getOutgoingFlows();
        Map<String, Boolean> resMap = flowMethod.getResMap();
        Map<String, NodeModel> nodes = flowMethod.getNodes();
        String nodeCode = flowMethod.getNodeCode();
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        if (CollUtil.isNotEmpty(outgoingFlows)) {

            Set<String> userList = new HashSet<>();
            userList.add(UserProvider.getLoginUserId());

            if (taskEntity != null) {
                userList.add(taskEntity.getCreatorUserId());
                if (StringUtil.isNotEmpty(taskEntity.getDelegateUserId())) {
                    userList.add(taskEntity.getDelegateUserId());
                }
            }

            List<UserEntity> userName = serviceUtil.getUserName(new ArrayList<>(userList));
            UserEntity createUser = null;
            UserEntity delegate = null;
            if (taskEntity != null) {
                createUser = userName.stream().filter(e -> Objects.equals(e.getId(), taskEntity.getCreatorUserId())).findFirst().orElse(null);
                if (StringUtil.isNotEmpty(taskEntity.getDelegateUserId())) {
                    delegate = userName.stream().filter(e -> Objects.equals(e.getId(), taskEntity.getDelegateUserId())).findFirst().orElse(null);
                }
            }


            // 设置条件判断 所需参数
            UserEntity userEntity = userName.stream().filter(e -> Objects.equals(e.getId(), UserProvider.getLoginUserId())).findFirst().orElse(null);
            flowMethod.setUserEntity(userEntity);
            if (flowMethod.getUserInfo() == null) {
                flowMethod.setUserInfo(UserProvider.getUser());
            }
            flowMethod.setCreateUser(createUser);
            flowMethod.setDelegate(delegate);

            NodeModel currentNodeModel = nodes.get(nodeCode);
            NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
            flowMethod.setNodeModel(currentNodeModel);
            if (StringUtils.equals(currentNodeModel.getDivideRule(), DivideRuleEnum.PARALLEL.getType())) {
                // 并行，全都为true
                for (String key : outgoingFlows) {
                    resMap.put(key, true);
                }
            } else {
                List<Boolean> exclusiveList = new ArrayList<>();
                boolean isExclusive = StringUtils.equals(currentNodeModel.getDivideRule(), DivideRuleEnum.EXCLUSIVE.getType());
                List<String> connectList = global.getConnectList();
                for (String key : outgoingFlows) {
                    boolean res = true;
                    if (!connectList.contains(key)) {
                        resMap.put(key, res);
                        continue;
                    }
                    // 获取出线节点 判断条件，没有设置条件的默认true
                    NodeModel nodeModel = nodes.get(key) != null ? nodes.get(key) : new NodeModel();
                    List<ProperCond> conditions = nodeModel.getConditions();
                    if (CollUtil.isNotEmpty(conditions)) {
                        flowMethod.setConditions(conditions);
                        flowMethod.setMatchLogic(nodeModel.getMatchLogic());
                        res = FlowJsonUtil.nodeConditionDecide(flowMethod);
                    }
                    if (isExclusive){
                        if (exclusiveList.contains(true)){
                            res = false;
                        }
                        exclusiveList.add(res);
                    }
                    resMap.put(key, res);
                }
            }
        }
    }

    // 处理条件
    public boolean hasCondition(FlowMethod flowMethod) {
        TaskEntity taskEntity = flowMethod.getTaskEntity();
        UserEntity createUser = null;
        UserEntity delegate = null;
        if (taskEntity != null) {
            createUser = serviceUtil.getUserInfo(taskEntity.getCreatorUserId());
            delegate = StringUtil.isNotEmpty(taskEntity.getDelegateUserId()) ? serviceUtil.getUserInfo(taskEntity.getDelegateUserId()) : null;
        }
        flowMethod.setCreateUser(createUser);
        flowMethod.setDelegate(delegate);
        UserEntity userEntity = serviceUtil.getUserInfo(UserProvider.getLoginUserId());
        flowMethod.setUserEntity(userEntity);
        flowMethod.setUserInfo(UserProvider.getUser());
        return FlowJsonUtil.nodeConditionDecide(flowMethod);
    }

    public void checkCondition(Map<String, Boolean> resMap, Map<String, NodeModel> nodes) throws WorkFlowException {
        if (CollUtil.isEmpty(nodes)) {
            return;
        }
        NodeModel global = nodes.get(NodeEnum.GLOBAL.getType());
        if (null == global) {
            throw new WorkFlowException(MsgCode.WF076.get());
        }
        List<String> connectList = global.getConnectList();
        long count = resMap.values().stream().filter(t -> Objects.equals(t, true)).count();
        Set<String> set = resMap.keySet();

        if (count == resMap.size()) {
            return;
        }
        Map<String, Boolean> defaultResMap = new HashMap<>();
        if (CollUtil.isNotEmpty(connectList)) {
            int c = 0;
            for (String key : set) {
                if (connectList.contains(key)) {
                    if (Boolean.TRUE.equals(resMap.get(key))) {
                        c++;
                    }
                    NodeModel nodeModel = nodes.get(key);
                    if (nodeModel != null) {
                        Boolean isDefault = nodeModel.getIsDefault();
                        defaultResMap.put(key, isDefault);
                    }
                }
            }
            if (c == 0) {
                long defaultCount = defaultResMap.values().stream().filter(t -> Objects.equals(t, true)).count();
                if (defaultCount > 0) {
                    resMap.putAll(defaultResMap);
                    c++;
                }
            }
            if (c < 1) {
                throw new WorkFlowException(MsgCode.WF075.get());
            }
        } else {
            if (count < 1) {
                throw new WorkFlowException(MsgCode.WF075.get());
            }
        }
    }
}

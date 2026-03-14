package jnpf.flowable.model.trigger;

import jnpf.flowable.entity.*;
import jnpf.flowable.model.task.FlowModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.util.FlowNature;
import jnpf.flowable.model.util.SystemAuditModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/10 20:44
 */
@Data
public class ExecuteModel {
    private String id;
    private String flowId;
    private String taskId;
    private String nodeCode;
    private String nodeId;
    private String operatorId;
    private String groupId;
    private FlowModel flowModel = new FlowModel();
    private List<Map<String, Object>> dataList = new ArrayList<>();
    private String parentId = FlowNature.PARENT_ID;
    private Map<String, NodeModel> nodes = new HashMap<>();
    private String instanceId;
    private String recordId;
    private String deploymentId;

    private TaskEntity taskEntity = new TaskEntity();

    private TriggerTaskEntity triggerTask = new TriggerTaskEntity();

    private NodeModel nodeModel = new NodeModel();

    /**
     * 当前执行节点，用于获取最后的执行节点引擎id
     */
    private String currentNodeId;
    //当前节点
    private String triggerKey;
    //是否异步
    private Integer isAsync;
    //判断是否节点重试
    private Boolean nodeRetry = false;
    //判断触发节点是否存在同步
    private Boolean sync = false;
    //节点数据
    private Map<String, List<Map<String, Object>>> nodeDataMap = new HashMap<>();
    //审批用户
    private List<OperatorEntity> operatorList = new ArrayList<>();
    //自动审批
    private List<SystemAuditModel> systemList = new ArrayList<>();
    //子流程
    private List<TaskEntity> subTaskList = new ArrayList<>();
    //触发节点分组
    private Map<String, List<NodeModel>> groupMap = new HashMap<>();
}

package jnpf.flowable.model.trigger;

import jnpf.flowable.model.task.FlowModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ExtraData {
    //审批参数
    //主建id
    private String id;
    //小版本
    private String flowId;
    //实例id
    private String taskId;
    //触发节点
    private String nodeCode;
    //节点id
    private String nodeId;
    //分组id
    private String groupId;
    //代办id
    private String operatorId;
    //flowable的实例id
    private String instanceId;
    //记录id
    private String recordId;
    private FlowModel flowModel = new FlowModel();
    /**
     * flowable的部署id
     */
    private String deploymentId;
    /**
     * 当前执行节点
     */
    private String currentNodeId;
    //当前节点
    private String triggerKey;
    //是否异步
    private Integer isAsync;
    //判断触发节点是否存在同步
    private Boolean sync = false;
    //节点数据
    private Map<String, List<Map<String, Object>>> nodeDataMap = new HashMap<>();
    //暂停的流程
    private List<String> launchFlow = new ArrayList<>();
    private String triggerId;
    private Map<String, FlowModel> systemMap = new HashMap<>();
    private List<String> operatorIdList = new ArrayList<>();
    private List<String> subTaskIdList = new ArrayList<>();
}

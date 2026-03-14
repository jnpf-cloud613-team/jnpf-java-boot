package jnpf.flowable.model.task;

import jnpf.base.UserInfo;
import jnpf.flowable.entity.*;
import jnpf.flowable.model.operator.AddSignModel;
import jnpf.flowable.model.templatenode.FlowErrorModel;
import jnpf.flowable.model.templatenode.TaskNodeModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.templatenode.nodejson.ProperCond;
import jnpf.permission.entity.UserEntity;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/6/24 下午4:57
 */
@Data
@Accessors(chain = true)
public class FlowMethod {
    /**
     * 任务主键
     */
    @Getter
    @Setter
    public String taskId;
    /**
     * 审批对象
     */
    private FlowModel flowModel;

    //createOperator
    /**
     * 经办实体
     */
    private OperatorEntity operatorEntity;
    /**
     * 状态
     */
    private Integer state;
    /**
     * 节点
     */
    private NodeModel nodeModel;

    //prevNodeList
    /**
     * flowable部署id
     */
    private String deploymentId;
    /**
     * 节点编码
     */
    private String nodeCode;
    /**
     * 节点对象
     */
    private List<TemplateNodeEntity> nodeEntityList;
    private List<String> nodeCodeList;

    //handleErrorRule
    private List<FlowErrorModel> errorList;

    //handleCondition
    /**
     * 表单数据
     */
    private Map<String, Object> formData = new HashMap<>();
    /**
     * 节点集合
     */
    private Map<String, NodeModel> nodes;
    /**
     * 任务
     */
    private TaskEntity taskEntity;

    //completeNode
    /**
     * flowable主键
     */
    private String flowableTaskId;
    /**
     * 结果Map
     */
    private Map<String, Boolean> resMap = new HashMap<>();


    //FlowDataModel
    /**
     * 节点对象
     */
    private TemplateNodeEntity nodeEntity;
    // false 仅查询表单数据
    /**
     * 是否保存表单数据
     */
    private Boolean isAssign = true;

    //getNextApprover
    /**
     * 下一节点
     */
    private List<NodeModel> nextNode = new ArrayList<>();


    //RecordModel
    /**
     * 记录操作类型，RecordEnum
     */
    private Integer type;
    /**
     * 流转操作人，如加签给谁
     */
    private String userId;


    //improperSort
    /**
     * 审批用户
     */
    private List<String> userIds;


    //TaskOperator
    /**
     * 异常规则
     */
    private Boolean errorRule = false;
    /**
     * 附加条件
     */
    private Boolean extraRule = false;
    /**
     * 默认审批通过
     */
    private Integer pass = 0;
    /**
     * 无法提交
     */
    private Integer notSubmit = 0;
    /**
     * 上一节点审批人指定处理人
     */
    private Integer node = 0;
    /**
     * 数据传递的最后一节点
     */
    private String resultNodeCode;
    //getConditionResult
    /**
     * 出线集合
     */
    private List<String> outgoingFlows;

    /**
     * 条件
     */
    private List<ProperCond> conditions;

    /**
     * 条件匹配逻辑
     */
    private String matchLogic;

    //checkPrint
    /**
     * 打印对象
     */
    private List<TaskNodeModel> printNodeList;

    private String handId;

    /**
     * 未经过的节点
     */
    private List<String> tobePass;
    /**
     * 当前节点
     */
    private List<String> currentNodes;
    /**
     * 是否签收
     */
    private Boolean signFor;

    /**
     * 审批方式
     */
    private Integer handleStatus;

    /**
     * 加签审批方式
     */
    private AddSignModel addSignModel;

    /**
     * 当前用户
     */
    private UserEntity userEntity;

    /**
     * 创建人
     */
    private UserEntity createUser;

    /**
     * 委托人
     */
    private UserEntity delegate;

    /**
     * 用户信息
     */
    private UserInfo userInfo;

    /**
     * 抄送集合
     */
    List<CirculateEntity> circulateList = new ArrayList<>();

    /**
     * 候选人接口标识
     */
    private Boolean auditFlag = false;

    /**
     * 获取下一级节点，是否获取子流程节点
     */
    private Boolean nextSubFlow = false;

    /**
     * 子流程表单数据
     */
    private Map<String, Object> subFormData = new HashMap<>();

    /**
     * 是否撤销流程
     */
    private Boolean isRevoke = false;

    /**
     * 逐级用户
     */
    private LaunchUserEntity launchUser;

}

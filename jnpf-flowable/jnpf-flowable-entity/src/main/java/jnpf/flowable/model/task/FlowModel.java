package jnpf.flowable.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.UserInfo;
import jnpf.flowable.entity.*;
import jnpf.flowable.enums.EventEnum;
import jnpf.flowable.enums.OpTypeEnum;
import jnpf.flowable.model.templatenode.FlowErrorModel;
import jnpf.flowable.model.templatenode.TaskNodeModel;
import jnpf.flowable.model.templatenode.nodejson.NodeModel;
import jnpf.flowable.model.util.FlowNature;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/15 9:17
 */
@Data
@JsonInclude
public class FlowModel extends FlowHandleModel {
    /**
     * 判断新增
     **/
    @Schema(description = "判断新增")
    private String id;
    /**
     * 版本ID
     **/
    @Schema(description = "版本ID")
    private String flowId;
    /**
     * 部署id
     **/
    @Schema(description = "部署id")
    private String deploymentId;

    private TemplateJsonEntity jsonEntity;
    private TaskEntity taskEntity = new TaskEntity();
    private List<TemplateNodeEntity> nodeEntityList = new ArrayList<>();
    private Map<String, NodeModel> nodes = new HashMap<>();
    private String flowableTaskId;
    private TemplateNodeEntity nodeEntity = new TemplateNodeEntity();
    /**
     * 审批处理标识，0.拒绝  1.同意
     */
    private Integer handleStatus = FlowNature.AUDIT_COMPLETION;
    /**
     * 任务详情类型
     * -1-我发起的新建/编辑
     * 0-我发起的详情
     * 1-待签事宜
     * 2-待办事宜
     * 3-在办事宜
     * 4-已办事宜
     * 5-抄送事宜
     * 6-流程监控
     */
    private String opType = OpTypeEnum.LAUNCH_CREATE.getType();

    private OperatorEntity operatorEntity = new OperatorEntity();
    private RecordEntity recordEntity = new RecordEntity();
    private List<TaskEntity> taskList = new ArrayList<>();
    private TemplateEntity templateEntity = new TemplateEntity();

    /**
     * 判断撤回的标识  1.发起撤回  2.审批撤回
     */
    private Integer flag = FlowNature.INITIATE_FLAG;
    /**
     * 事件状态
     */
    private Integer eventStatus = EventEnum.NONE.getStatus();
    /**
     * 判断撤回时，为true会抛出异常
     */
    private Boolean isException = false;
    /**
     * 判断是否校验发起人权限
     */
    private Boolean hasPermission = false;
    /**
     * 下一级节点编码
     */
    private List<String> nextCodes = new ArrayList<>();

    /**
     * 挂起，0.全部  1.仅主流程
     */
    private Integer pause = 0;
    /**
     * 流程结束是否更新为结束节点
     */
    private Boolean finishFlag = true;

    /**
     * 子流程节点编码
     */
    private String subCode;
    /**
     * 流程详情节点
     */
    private List<TaskNodeModel> nodeList = new ArrayList<>();
    /**
     * 是否流程，0-菜单 1-发起
     */
    private Integer isFlow = 0;
    /**
     * 签名主键
     */
    private String signId;
    /**
     * 下次继续使用此签名
     */
    private Boolean useSignNext = false;
    /**
     * 触发动作，1同意 2拒绝 3退回 4办理
     */
    private Integer action;
    /**
     * 拒绝直接结束的触发标识
     */
    private Boolean rejectTrigger = false;
    /**
     * 退回id，任务流程为退回触发时，获取最后一个执行节点的id用于退回
     */
    private String backId;
    /**
     * 子流程
     **/
    @Schema(description = "子流程")
    private String parentId = FlowNature.PARENT_ID;
    /**
     * 创建人
     **/
    @Schema(description = "创建人")
    private String userId;
    /**
     * 被委托人
     */
    @Schema(description = "被委托人")
    private String delegateUser;
    /**
     * 当前经办id
     **/
    @Schema(description = "当前经办id")
    private String operatorId;
    /**
     * 任务主键
     */
    @Schema(description = "任务主键")
    private String taskId;
    /**
     * 子流程，是否异步
     **/
    private Integer isAsync = FlowNature.CHILD_SYNC;
    /**
     * 用户信息
     */
    private UserInfo userInfo;

    /**
     * 抄送消息标识
     */
    private Boolean copyMsgFlag = true;
    /**
     * 自动转审，走指派方法的标识
     */
    private Boolean autoTransferFlag = false;
    /**
     * 是否自动审批
     */
    private Boolean autoAudit = false;
    /**
     * 子流程是否暂存标识
     */
    private Boolean subFlow = false;
    /**
     * 触发流程退回
     */
    private Boolean triggerBack = false;

    /**
     * 异常节点
     */
    private List<FlowErrorModel> errorList = new ArrayList<>();

    /**
     * 流程模板id
     */
    private String templateId;

    /**
     * 自由节点编码
     */
    private String freeCode;

    /**
     * 发起人
     */
    private List<String> userIds = new ArrayList<>();
    /**
     * 表单数据集合
     */
    private List<Map<String, Object>> formDataList = new ArrayList<>();
}

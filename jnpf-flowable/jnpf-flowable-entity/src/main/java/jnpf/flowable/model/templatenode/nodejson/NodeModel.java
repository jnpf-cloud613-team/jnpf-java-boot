package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.database.model.query.SuperQueryJsonModel;
import jnpf.emnus.SearchMethodEnum;
import jnpf.flowable.enums.*;
import jnpf.flowable.model.templatejson.FlowParamModel;
import jnpf.flowable.model.util.FlowConstant;
import jnpf.flowable.model.util.FlowNature;
import jnpf.model.visualjson.FieLdsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class NodeModel {

    /**
     * 节点类型
     */
    @Schema(description = "节点类型")
    private String type;
    /**
     * 节点id
     */
    @Schema(description = "节点id")
    private String nodeId;
    /**
     * 节点名称
     */
    @Schema(description = "节点名称")
    private String nodeName;

    /*------------- 全局 -------------*/
    /**
     * 可见的连线集合，用于条件判断
     */
    private List<String> connectList = new ArrayList<>();
    /**
     * 全局参数
     */
    @Schema(description = "全局参数")
    private List<FlowParamModel> globalParameterList = new ArrayList<>();
    /**
     * 标题类型 0：默认  1：自定义
     */
    @Schema(description = "标题类型")
    private Integer titleType = FlowNature.TITLE_TYPE;
    /**
     * 默认名称
     */
    @Schema(description = "默认名称")
    private String defaultContent = "{" + FlowConstant.USER_NAME + "}的{" + FlowConstant.FLOW_NAME + "}";
    /**
     * 自定义名称
     */
    @Schema(description = "自定义名称")
    private String titleContent;
    /**
     * 启用签名
     */
    @Schema(description = "启用签名")
    private Boolean hasSign = false;
    /**
     * 允许撤销
     */
    @Schema(description = "允许撤销")
    private Boolean hasRevoke = false;
    /**
     * 允许评论
     */
    @Schema(description = "允许评论")
    private Boolean hasComment = true;
    /**
     * 显示评论已被删除提示
     */
    @Schema(description = "显示评论已被删除提示")
    private Boolean hasCommentDeletedTips = true;
    /**
     * 审批任务是否签收
     */
    @Schema(description = "审批任务是否签收")
    private Boolean hasSignFor = false;
    /**
     * 允许审批节点独立配置表单
     */
    @Schema(description = "允许审批节点独立配置表单")
    private Boolean hasAloneConfigureForms = false;
    /**
     * 拒绝后允许流程继续流转审批
     */
    @Schema(description = "拒绝后允许流程继续流转审批")
    private Boolean hasContinueAfterReject = false;
    /**
     * 允许发起人对当前逾期节点进行催办
     */
    @Schema(description = "逾期节点催办")
    private Boolean hasInitiatorPressOverdueNode = true;
    /**
     * 校验发起杈限
     */
    @Schema(description = "校验发起杈限")
    private Boolean hasPermission = false;
    /**
     * 自动提交规则
     */
    @Schema(description = "自动提交规则")
    private AutoSubmitConfig autoSubmitConfig = new AutoSubmitConfig();
    /**
     * 流程撤回规则  1: 不允许撤回  2: 发起节点允许撤回  3:所有节点允许撤回
     */
    @Schema(description = "流程撤回规则")
    private Integer recallRule = FlowNature.NOT_ALLOWED;
    /**
     * 异常处理规则  1：超级管理员  2：指定人员   3：上一节点审批人指定  4：默认审批通过  5：无法提交
     */
    @Schema(description = "异常处理规则")
    private Integer errorRule = ErrorRuleEnum.ADMINISTRATOR.getCode();
    /**
     * 异常处理指定人员
     */
    @Schema(description = "异常处理指定人员")
    private List<String> errorRuleUser = new ArrayList<>();
    /**
     * 流程归档配置
     */
    @Schema(description = "流程归档配置")
    private FileConfig fileConfig = new FileConfig();

    /*------------- 开始 -------------*/
    /**
     * 流程表单id
     */
    @Schema(description = "流程表单id")
    private String formId;
    /**
     * 流程表单名称
     */
    @Schema(description = "流程表单名称")
    private String formName;
    /**
     * 流程表单权限
     */
    @Schema(description = "流程表单权限")
    private List<Map<String, Object>> formOperates = new ArrayList<>();
    /**
     * 发起权限  1.公开 2.权限设置
     */
    @Schema(description = "发起权限 1.公开 2.权限设置")
    private Integer launchPermission = FlowNature.LAUNCH_PERMISSION;
    /**
     * 打印配置
     */
    @Schema(description = "打印配置")
    private PrintConfig printConfig = new PrintConfig();
    /**
     * 限时设置配置
     */
    @Schema(description = "限时设置配置")
    private TimeConfig timeLimitConfig = new TimeConfig();
    /**
     * 提醒配置
     */
    @Schema(description = "提醒配置")
    private TimeConfig noticeConfig = new TimeConfig();
    /**
     * 超时设置
     */
    @Schema(description = "超时设置")
    private TimeConfig overTimeConfig = new TimeConfig();
    /**
     * 流程待办通知
     */
    @Schema(description = "流程待办通知")
    public MsgConfig waitMsgConfig = new MsgConfig();
    /**
     * 流程结束通知
     */
    @Schema(description = "流程结束通知")
    public MsgConfig endMsgConfig = new MsgConfig();
    /**
     * 节点同意通知
     */
    @Schema(description = "节点同意通知")
    public MsgConfig approveMsgConfig = new MsgConfig();
    /**
     * 节点拒绝通知
     */
    @Schema(description = "节点拒绝通知")
    public MsgConfig rejectMsgConfig = new MsgConfig();
    /**
     * 节点退回通知
     */
    @Schema(description = "节点退回通知")
    public MsgConfig backMsgConfig = new MsgConfig();
    /**
     * 节点抄送通知
     */
    @Schema(description = "节点抄送通知")
    public MsgConfig copyMsgConfig = new MsgConfig();
    /**
     * 节点超时通知
     */
    @Schema(description = "节点超时通知")
    public MsgConfig overTimeMsgConfig = new MsgConfig();
    /**
     * 节点提醒通知
     */
    @Schema(description = "节点提醒通知")
    public MsgConfig noticeMsgConfig = new MsgConfig();
    /**
     * 节点提醒通知
     */
    @Schema(description = "评论提醒通知")
    public MsgConfig commentMsgConfig = new MsgConfig();

    /*------------- 自由 -------------*/
    /**
     * 自行结束
     */
    @Schema(description = "自行结束")
    private Boolean oneSelfEndApproval = true;

    /**
     * 流转次数
     */
    @Schema(description = "流转次数")
    private Integer approvalNumber = 5;

    /*------------- 审批 -------------*/
    /**
     * 数据传递
     */
    @Schema(description = "数据传递")
    private List<Assign> assignList = new ArrayList<>();
    /**
     * 指定审批人
     */
    @Schema(description = "指定审批人")
    private Integer assigneeType = OperatorEnum.NOMINATOR.getCode();
    /**
     * 是否候选人
     */
    @Schema(description = "是否候选人")
    private Boolean isCandidates = false;
    /**
     * 直属主管审批人类型，1.发起人  2.上节点审批人
     */
    private Integer approverType = FlowNature.INITIATOR;
    /**
     * 发起者主管  1：直属  2：第二级主管  ....  10:第10级主管
     */
    @Schema(description = "发起者主管")
    private Integer managerLevel = 1;
    /**
     * 表单字段审核方式的类型  1：用户 2：部门  3：岗位  4：角色  5：分组
     */
    @Schema(description = "表单字段审核方式的类型")
    private Integer formFieldType = 1;
    /**
     * 表单字段
     */
    @Schema(description = "表单字段")
    private String formField;
    /**
     * 审批节点id
     */
    @Schema(description = "审批节点id")
    private String approverNodeId;
    /**
     * 审批人集合
     */
    @Schema(description = "审批人集合")
    private List<String> approvers = new ArrayList<>();
    /**
     * 审批人依次审批顺序
     */
    @Schema(description = "审批人依次审批顺序")
    private List<String> approversSortList = new ArrayList<>();
    /**
     * 逐级审批
     */
    @Schema(description = "逐级审批")
    private ApproversConfig approversConfig = new ApproversConfig();
    /**
     * 审批人范围  1:无审批人范围  2:同一部门  3:同一岗位  4:发起人上级  5:发起人下属  6:同一公司
     */
    @Schema(description = "审批人范围")
    private Integer extraRule = ExtraRuleEnum.NONE.getCode();
    /**
     * 会签规则  0：或签  1：会签  2：依次审批
     */
    @Schema(description = "会签规则")
    private Integer counterSign = FlowNature.FIXED_APPROVER;
    /**
     * 会签流转配置
     */
    @Schema(description = "会签流转配置")
    private CounterSignConfig counterSignConfig = new CounterSignConfig();
    /**
     * 抄送人集合
     */
    @Schema(description = "抄送人集合")
    private List<String> circulateUser = new ArrayList<>();
    /**
     * 抄送人范围
     */
    @Schema(description = "抄送人范围")
    private Integer extraCopyRule = ExtraRuleEnum.NONE.getCode();
    /**
     * 允许自选抄送人
     */
    @Schema(description = "允许自选抄送人")
    private Boolean isCustomCopy = false;
    /**
     * 抄送给流程发起人
     */
    @Schema(description = "抄送给流程发起人")
    private Boolean isInitiatorCopy = false;
    /**
     * 抄送给表单变量
     */
    @Schema(description = "抄送给表单变量")
    private Boolean isFormFieldCopy = false;
    /**
     * 表单字段类型  1：用户 2：部门  3：岗位  4：角色  5：分组
     */
    @Schema(description = "表单字段类型")
    private Integer copyFormFieldType = 1;
    /**
     * 表单字段
     */
    @Schema(description = "表单字段")
    private String copyFormField;
    /**
     *
     */
    @Schema(description = "")
    private Boolean hasFile = false;
    /**
     * 节点参数
     */
    @Schema(description = "节点参数")
    private List<GroupsModel> parameterList = new ArrayList<>();
    /**
     * 辅助信息
     */
    @Schema(description = "辅助信息")
    private List<AuxiliaryInfo> auxiliaryInfo = new ArrayList<>();
    /**
     * 自动同意规则,默认不启用
     */
    @Schema(description = "自动同意规则,默认不启用")
    private Boolean hasAutoApprover = false;
    /**
     * 自动同意规则
     */
    @Schema(description = "自动同意规则")
    private AutoAuditRule autoAuditRule;
    /**
     * 自动拒绝规则
     */
    @Schema(description = "自动拒绝规则")
    private AutoAuditRule autoRejectRule;

    /**
     * 通过按钮
     */
    @Schema(description = "通过按钮")
    private Boolean hasAuditBtn = true;
    /**
     * 通过按钮名称
     */
    @Schema(description = "通过按钮名称")
    private String auditBtnText = "同意";
    /**
     * 拒绝按钮
     */
    @Schema(description = "拒绝按钮")
    private Boolean hasRejectBtn = true;
    /**
     * 拒绝按钮名称
     */
    @Schema(description = "拒绝按钮名称")
    private String rejectBtnText = "拒绝";
    /**
     * 退回按钮
     */
    @Schema(description = "退回按钮")
    private Boolean hasBackBtn = false;
    /**
     * 退回按钮名称
     */
    @Schema(description = "退回按钮名称")
    private String backBtnText = "退回";
    /**
     * 加签按钮
     */
    @Schema(description = "加签按钮")
    private Boolean hasFreeApproverBtn = false;
    /**
     * 加签按钮名称
     */
    @Schema(description = "加签按钮名称")
    private String freeApproverBtnText = "加签";
    /**
     * 减签按钮
     */
    @Schema(description = "减签按钮")
    private Boolean hasReduceApproverBtn = false;
    /**
     * 减签按钮名称
     */
    @Schema(description = "减签按钮名称")
    private String reduceApproverBtnText = "减签";
    /**
     * 转审按钮
     */
    @Schema(description = "转审按钮")
    private Boolean hasTransferBtn = false;
    /**
     * 转审按钮名称
     */
    @Schema(description = "转审按钮名称")
    private String transferBtnText = "转审";
    /**
     * 协办按钮
     */
    @Schema(description = "协办按钮")
    private Boolean hasAssistBtn = false;
    /**
     * 协办按钮名称
     */
    @Schema(description = "协办按钮名称")
    private String assistBtnText = "协办";
    /**
     * 暂存按钮
     */
    @Schema(description = "暂存按钮")
    private Boolean hasSaveAuditBtn = false;
    /**
     * 暂存按钮名称
     */
    @Schema(description = "暂存按钮名称")
    private String saveAuditBtnText = "暂存";
    /**
     * 分流规则    inclusion: 根据条件多分支流转(包容网关)  exclusive:根据条件单分支流转（排它网关） parallel:所有分支都流转（并行网关）
     */
    @Schema(description = "分流规则")
    private String divideRule = DivideRuleEnum.INCLUSION.getType();
    /**
     * 接口服务
     */
    @Schema(description = "接口服务")
    private InterfaceConfig interfaceConfig = new InterfaceConfig();
    /**
     * 内容
     */
    @Schema(description = "内容")
    private String content;
    /**
     * 子流程发起权限
     */
    @Schema(description = "子流程发起权限")
    private Integer subFlowLaunchPermission = 1;
    /**
     * 退回设置，被退回的节点重新提交时
     */
    private Integer backType = FlowNature.RESTART_TYPE;
    /**
     * 设置退回到的节点
     */
    private String backNodeCode = FlowNature.START;


    /*------------- 同步类型 -------------*/
    /**
     * 同步类型  0:同步  1:异步
     */
    @Schema(description = "同步类型")
    private Integer isAsync = FlowNature.CHILD_SYNC;

    /*------------- 子流程 -------------*/
    /**
     * 自动提交 0:否  1:是
     */
    @Schema(description = "自动提交")
    private Integer autoSubmit = 0;
    /**
     * 节点提醒通知
     */
    @Schema(description = "节点提醒通知")
    public MsgConfig launchMsgConfig = new MsgConfig();
    /**
     * 流程版本主键
     */
    @Schema(description = "流程版本主键")
    private String flowId;
    /**
     * 创建规则  0:同时创建  1:依次创建
     */
    @Schema(description = "创建规则  0:同时创建  1:依次创建")
    private Integer createRule = FlowNature.CHILD_SYNC;

    /*------------- 线 -------------*/
    /**
     * 默认分支
     */
    @Schema(description = "默认分支")
    private Boolean isDefault = false;
    /**
     * 连接线条件
     */
    @Schema(description = "连接线条件")
    private List<ProperCond> conditions = new ArrayList<>();
    /**
     * 逻辑
     */
    @Schema(description = "逻辑")
    private String matchLogic = SearchMethodEnum.AND.getSymbol();

    /*--------------------------*/
    /**
     * 退回事件
     **/
    @Schema(description = "退回事件")
    private FuncConfig backFuncConfig = new FuncConfig();
    /**
     * 拒绝事件
     **/
    @Schema(description = "拒绝事件")
    private FuncConfig rejectFuncConfig = new FuncConfig();
    /**
     * 同意事件
     **/
    @Schema(description = "同意事件")
    private FuncConfig approveFuncConfig = new FuncConfig();
    /**
     * 开始事件
     **/
    @Schema(description = "开始事件")
    private FuncConfig initFuncConfig = new FuncConfig();
    /**
     * 结束事件
     **/
    @Schema(description = "结束事件")
    private FuncConfig endFuncConfig = new FuncConfig();
    /**
     * 超时事件
     **/
    @Schema(description = "超时事件")
    private FuncConfig overtimeFuncConfig = new FuncConfig();
    /**
     * 提醒事件
     */
    @Schema(description = "提醒事件")
    private FuncConfig noticeFuncConfig = new FuncConfig();
    /**
     * 节点撤回事件
     **/
    @Schema(description = "节点撤回事件")
    private FuncConfig recallFuncConfig = new FuncConfig();
    /**
     * 发起撤回事件
     **/
    @Schema(description = "发起撤回事件")
    private FuncConfig flowRecallFuncConfig = new FuncConfig();

    /*------------- 触发 -------------*/
    /**
     * 触发事件 1-表单事件 2-审批事件 3-空白事件
     */
    @Schema(description = "触发事件")
    private Integer triggerEvent = 1;
    /**
     * 触发表单事件 1-新增 2-修改 3-删除
     */
    @Schema(description = "触发表单事件")
    private Integer triggerFormEvent = 1;
    /**
     * 1-同意  2-拒绝  3-退回  4-确认办理
     */
    @Schema(description = "")
    private List<Integer> actionList = new ArrayList<>();
    /**
     * 表单事件-修改数据-修改字段
     */
    @Schema(description = "")
    private List<String> updateFieldList = new ArrayList<>();
    /**
     * 触发条件
     */
    @Schema(description = "触发条件")
    private List<SuperQueryJsonModel> ruleList = new ArrayList<>();
    /**
     * 条件规则匹配逻辑
     */
    @Schema(description = "条件规则匹配逻辑")
    private String ruleMatchLogic = SearchMethodEnum.AND.getSymbol();

    /**
     * 通知人类型
     */
    @Schema(description = "通知人类型")
    private List<String> msgUserType = new ArrayList<>();
    /**
     * 执行失败通知
     */
    @Schema(description = "执行失败通知")
    private MsgConfig failMsgConfig = new MsgConfig();
    /**
     * 开始执行通知
     */
    @Schema(description = "开始执行通知")
    private MsgConfig startMsgConfig = new MsgConfig();
    /**
     * cron表达式
     */
    @Schema(description = "cron表达式")
    private String cron;
    /**
     * 触发结束时间类型
     */
    @Schema(description = "触发结束时间类型")
    private Integer endTimeType;
    /**
     * 触发次数
     */
    @Schema(description = "触发次数")
    private Integer endLimit;
    /**
     * webhookUrl
     */
    @Schema(description = "webhookUrl")
    private String webhookUrl;
    /**
     * webhook获取接口字段Url
     */
    @Schema(description = "webhook获取接口字段Url")
    private String webhookGetFieldsUrl;
    /**
     * webhook获取接口字段识别码
     */
    @Schema(description = "webhook获取接口字段识别码")
    private String webhookRandomStr;
    /**
     * 通知触发消息id
     */
    @Schema(description = "通知触发消息id")
    private String noticeId;
    /**
     * 分组id
     */
    @Schema(description = "分组id")
    private String groupId;

    /*------------- 获取数据 -------------*/
    /**
     * 菜单id
     */
    private String id;
    /**
     * 表单类型 1-从表单中获取 2-从流程中获取 3-从数据接口中获取 4-从子表
     */
    @Schema(description = "表单类型")
    private Integer formType = 1;
    /**
     * 接口参数
     */
    @Schema(description = "接口参数")
    private List<IntegrateTplModel> interfaceTemplateJson = new ArrayList<>();
    /**
     * 表单字段
     */
    @Schema(description = "表单字段")
    private List<FieLdsModel> formFieldList = new ArrayList<>();
    /**
     * 排序
     */
    @Schema(description = "排序")
    private List<SortModel> sortList = new ArrayList<>();

    /*------------- 新增数据 -------------*/
    /**
     * 字段设置
     */
    @Schema(description = "字段设置")
    private List<TemplateJsonModel> transferList = new ArrayList<>();
    /**
     * 数据源
     */
    @Schema(description = "数据源")
    private String dataSourceForm;

    /*------------- 更新数据 -------------*/
    /**
     * 没有可修改的数据时，向对应表单中新增一条数据
     */
    @Schema(description = "是否新增数据")
    private Boolean unFoundRule = false;

    /*------------- 删除数据 -------------*/
    /**
     * 删除类型
     */
    @Schema(description = "删除类型")
    private Integer deleteType;
    /**
     * 表类型  0-主表  1-子表
     */
    @Schema(description = "表类型")
    private Integer tableType;
    /**
     * 子表
     */
    @Schema(description = "子表")
    private String subTable;
    /**
     * 删除条件  1-存在  2-不存在
     */
    @Schema(description = "删除条件")
    private Integer deleteCondition;

    /*------------- 数据接口节点 -------------*/
    /**
     * 数据接口参数
     */
    @Schema(description = "数据接口参数")
    private List<IntegrateTplModel> templateJson = new ArrayList<>();

    /*------------- 外部节点 -------------*/
    /**
     * 外部接口参数
     */
    @Schema(description = "外部接口参数")
    private Map<String, List<TemplateJsonModel>> outsideOptions = new HashMap<>();

    /*------------- 消息通知节点 -------------*/
    /**
     * 通知人来源类型
     */
    @Schema(description = "通知人来源类型")
    private Integer msgUserIdsSourceType = FieldEnum.FIELD.getCode();
    /**
     * 通知人
     */
    @Schema(description = "通知人")
    private List<String> msgUserIds = new ArrayList<>();
    /**
     * 消息id
     */
    @Schema(description = "消息id")
    private String msgId;
    /**
     * 消息名称
     */
    @Schema(description = "消息名称")
    private String msgName;
    /**
     * 消息接口参数
     */
    @Schema(description = "消息接口参数")
    private List<IntegrateTplModel> msgTemplateJson = new ArrayList<>();

    /*------------- 发起审批节点 -------------*/
    /**
     * 发起人
     */
    @Schema(description = "发起人")
    private List<String> initiator = new ArrayList<>();

    /**
     * 比例值
     */
    @Schema(description = "比例值")
    private Integer completeRatio = 100;

    /*------------- 创建日程 -------------*/
    /**
     * 日程标题
     */
    @Schema(description = "日程标题")
    private String title;
    /**
     * 日程内容
     */
    @Schema(description = "日程内容")
    private String contents;
    /**
     * 日程附件
     */
    @Schema(description = "日程附件")
    private String files;
    /**
     * 日程全天
     */
    @Schema(description = "日程全天")
    private Integer allDay = 0;
    /**
     * 日程开始日期
     */
    @Schema(description = "日程开始日期")
    private String startDay;
    /**
     * 日程开始时间
     */
    @Schema(description = "日程开始时间")
    private String startTime;
    /**
     * 日程时长
     */
    @Schema(description = "日程时长")
    private Integer duration = 0;
    /**
     * 日程结束日期
     */
    @Schema(description = "日程结束日期")
    private String endDay;
    /**
     * 日程结束时间
     */
    @Schema(description = "日程结束时间")
    private String endTime;
    /**
     * 日程创建人
     */
    @Schema(description = "日程创建人")
    private String creatorUserId;
    /**
     * 日程参与人
     */
    @Schema(description = "日程参与人")
    private List<String> toUserIds = new ArrayList<>();
    /**
     * 日程标签颜色
     */
    @Schema(description = "日程标签颜色")
    private String color;
    /**
     * 日程提醒时间
     */
    @Schema(description = "日程提醒时间")
    private Integer reminderTime = -1;
    /**
     * 日程提醒方式
     */
    @Schema(description = "日程提醒方式")
    private Integer reminderType = 1;
    /**
     * 日程发送配置
     */
    @Schema(description = "日程发送配置")
    private String send;
    /**
     * 日程发送配置名称
     */
    @Schema(description = "日程发送配置名称")
    private String sendName;
    /**
     * 日程分类
     */
    @Schema(description = "日程分类")
    private String category;
    /**
     * 日程重复提醒1.不重复 2.每天重复 3.每周重复 4.每月重复 5.每年重复
     */
    @Schema(description = "日程重复提醒")
    private Integer repetition = 1;
    /**
     * 日程结束重复
     */
    @Schema(description = "日程结束重复")
    private Long repeatTime;
    /**
     * 日程开始时间类型
     */
    @Schema(description = "开始时间类型")
    private Integer startDaySourceType = FieldEnum.FIELD.getCode();
    /**
     * 日程结束时间类型
     */
    @Schema(description = "结束时间类型")
    private Integer endDaySourceType = FieldEnum.FIELD.getCode();
    /**
     * 日程标题类型
     */
    @Schema(description = "日程标题类型")
    private Integer titleSourceType = FieldEnum.FIELD.getCode();
    /**
     * 日程内容类型
     */
    @Schema(description = "日程内容类型")
    private Integer contentsSourceType = FieldEnum.FIELD.getCode();
    /**
     * 日程创建人类型
     */
    @Schema(description = "日程创建人类型")
    private Integer creatorUserIdSourceType = FieldEnum.FIELD.getCode();
    /**
     * 日程参与人类型
     */
    @Schema(description = "日程参与人类型")
    private Integer toUserIdsSourceType = FieldEnum.FIELD.getCode();

}

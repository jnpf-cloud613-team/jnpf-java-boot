package jnpf.flowable.model.util;

import lombok.Data;

/**
 * 在线工作流开发
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Data
public class FlowNature {
    private FlowNature() {}

    /**
     * 子表数据标识
     */
    public static final  String SUB_TABLE = "@subTable";
    /**
     * 数据传递 全局参数
     */
    public static final  String GLOBAL_PARAMETER = "globalParameter";
    /**
     * 撤销流水号编码
     */
    public static final  String REVOKE_BILL_CODE = "workflow_revoke";
    /**
     * 撤销表单编码
     */
    public static final  String REVOKE_FORM_CODE = "revoke";

    /**
     * 结束编码、名称
     */
    public static final  String END_CODE = "end";
    public static final  String END_NAME = "结束";
    /**
     * 开始编码、名称
     */
    public static final  String START_CODE = "start";
    public static final  String START_NAME = "开始";
    /**
     * 触发节点编码、名称
     */
    public static final String TRIGGER_CODE = "";
    public static final  String TRIGGER_NAME = "触发节点";

    /**
     * 表单字段后缀
     */
    public static final  String FORM_FIELD_SUFFIX = "_jnpfId";

    /**
     * 系统编码
     */
    public static final  String SYSTEM_CODE = "jnpf";
    /**
     * 系统名称
     */
    public static final  String SYSTEM_NAME = "系统";
    /**
     * 系统头像
     */
    public static final  String SYSTEM_HEAD_ICON = "001.png";

    /**
     * 流程标题
     */
    public static final  Integer TITLE_TYPE = 0;

    /**
     * 发起权限
     **/
    public static final  Integer LAUNCH_PERMISSION = 1;

    /**
     * 流程父节点
     **/
    public static final  String PARENT_ID = "0";

    //----------------------记录、审批状态--------------------
    /**
     * 正常状态
     */
    public static final  Integer NORMAL = 0;
    /**
     * 作废状态（记录）
     */
    public static final  Integer INVALID = -1;
    /**
     * 有操作（如加签、同意等）变1 (审批)
     */
    public static final  Integer ACTION = 1;

    //----------------------驳回类型--------------------
    /**
     * 驳回开始
     **/
    public static final  String START = "0";

    /**
     * 驳回上一节点
     **/
    public static final  String UP = "1";

    /**
     * 自选节点
     **/
    public static final  String REJECT = "2";

    //----------------------办理类型--------------------

    /**
     * 不是办理节点
     **/
    public static final  Integer NOT_PROCESSING = 0;

    /**
     * 办理节点
     **/
    public static final  Integer PROCESSING = 1;

    //----------------------发起类型--------------------

    /**
     * 任务发起
     **/
    public static final  Integer TASK_INITIATION = 1;

    /**
     * 逐级发起
     **/
    public static final  Integer STEP_INITIATION = 2;

    //----------------------审批人类型--------------------

    /**
     * 候选人
     **/
    public static final  Integer CANDIDATES = 1;

    /**
     * 异常人
     **/
    public static final Integer CANDIDATES_ERROR = 2;

    /**
     * 选择分支
     **/
    public static final Integer BRANCH = 3;

    //----------------------外部事件状态--------------------

    /**
     * 成功
     **/
    public static final Integer SUCCESS = 0;

    /**
     * 失败
     **/
    public static final Integer LOSE = 1;

    //----------------------审批方式--------------------

    /**
     * 重新审批
     **/
    public static final Integer RESTART_TYPE = 1;

    /**
     * 当前审批
     **/
    public static final Integer PRESENT_TYPE = 2;

    //-------------------------审批类型---------------------------

    /**
     * 或签
     **/
    public static final Integer FIXED_APPROVER = 0;

    /**
     * 会签
     **/
    public static final Integer FIXED_JOINTLY_APPROVER = 1;

    /**
     * 依次
     **/
    public static final Integer IMPROPER_APPROVER = 2;

    //-------------------------会签审批方式---------------------------

    /**
     * 延后计算
     */
    public static final Integer DELAY = 2;

    /**
     * 实时计算
     */
    public static final Integer ACTUAL = 1;

    /**
     * 百分比
     **/
    public static final Integer PERCENT = 1;

    /**
     * 人数
     **/
    public static final Integer NUMBER = 2;

    //-------------------------审批状态---------------------------

    /**
     * 通过
     **/
    public static final Integer AUDIT_COMPLETION = 1;

    /**
     * 拒绝
     **/
    public static final Integer REJECT_COMPLETION = 0;

    //-------------------------同步类型---------------------------

    /**
     * 同步
     **/
    public static final Integer CHILD_SYNC = 0;

    /**
     * 异步
     **/
    public static final Integer CHILD_ASYNC = 1;

    //-------------------------加签类型---------------------------

    /**
     * 加签前
     */
    public static final Integer BEFORE = 1;

    /**
     * 加签后
     */
    public static final Integer LATER = 2;

    //-------------------------消息类型---------------------------

    /**
     * 发起消息
     */
    public static final Integer START_MSG = 0;

    /**
     * 审批消息
     */
    public static final Integer APPROVE_MSG = 1;

    /**
     * 结束消息
     */
    public static final Integer END_MSG = 2;

    //-------------------------逐级审批---------------------------

    /**
     * 发起人
     */
    public static final Integer INITIATOR = 1;

    /**
     * 上节点审批人
     */
    public static final Integer PREVIOUSLY = 2;

    /**
     * 组织
     */
    public static final Integer ORGANIZATION = 3;

    //---------------------流程撤回---------------------------
    /**
     * 不允许撤回
     */
    public static final Integer NOT_ALLOWED = 1;

    /**
     * 发起节点允许撤回
     */
    public static final Integer START_ALLOWED = 2;

    /**
     * 所有节点允许撤回
     */
    public static final Integer ALLOWED = 3;


    //---------------------撤回标识--------------------------
    /**
     * 发起撤回
     */
    public static final Integer INITIATE_FLAG = 1;

    /**
     * 审批撤回
     */
    public static final Integer APPROVAL_FLAG = 2;


    //---------------------归档---------------------------
    /**
     * 流程所有人
     */
    public static final Integer FLOW_ALL = 1;

    /**
     * 流程发起人
     */
    public static final Integer FLOW_INITIATOR = 2;

    /**
     * 最后节点审批人
     */
    public static final Integer FLOW_LAST = 3;

    //---------------------流程是否恢复---------------------------

    /**
     * 能恢复
     */
    public static final Integer RESTORE = 0;

    /**
     * 不能恢复
     */
    public static final Integer NOT_RESTORE = 1;


    //---------------------流程显示类型---------------------------
    /**
     * 全局
     */
    public static final Integer ALL_SHOW_TYPE = 0;

    /**
     * 流程
     */
    public static final Integer FLOW_SHOW_TYPE = 1;

    /**
     * 菜单
     */
    public static final Integer MENU_SHOW_TYPE = 2;

    //---------------------流程权限类型---------------------------

    /**
     * 全局
     */
    public static final Integer ALL = 1;

    /**
     * 权限
     */
    public static final Integer AUTHORITY = 2;

    //---------------------流程类型---------------------------

    /**
     * 标准
     */
    public static final Integer STANDARD = 0;

    /**
     * 简流
     */
    public static final  Integer SIMPLE = 1;

    /**
     * 任务
     */
    public static final  Integer QUEST = 2;

    /**
     * 自由
     */
    public static final  Integer FREE = 3;


}

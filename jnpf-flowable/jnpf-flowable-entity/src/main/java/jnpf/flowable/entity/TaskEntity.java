package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程引擎
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022年7月11日 上午9:18
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_task")
public class TaskEntity extends SuperExtendEntity<String> implements Serializable {

    /**
     * 任务编码
     */
    @TableField("F_EN_CODE")
    private String enCode;

    /**
     * 任务标题
     */
    @TableField("F_FULL_NAME")
    private String fullName;

    /**
     * 流程名称
     */
    @TableField("F_FLOW_NAME")
    private String flowName;

    /**
     * 流程编码
     */
    @TableField("F_FLOW_CODE")
    private String flowCode;

    /**
     * 任务状态
     */
    @TableField("F_STATUS")
    private Integer status;
    /**
     * 历史状态
     */
    @TableField("F_HIS_STATUS")
    private Integer hisStatus;

    /**
     * 流程分类
     */
    @TableField("F_FLOW_CATEGORY")
    private String flowCategory;

    /**
     * 流程类型
     */
    @TableField("F_FLOW_TYPE")
    private Integer flowType;

    /**
     * 流程版本
     */
    @TableField("F_FLOW_VERSION")
    private String flowVersion;

    /**
     * 同步异步（0：同步，1：异步）
     */
    @TableField("F_IS_ASYNC")
    private Integer isAsync;

    /**
     * 子流程参数
     */
    @TableField("F_SUB_PARAMETER")
    private String subParameter;

    /**
     * 父级实例id
     */
    @TableField("F_PARENT_ID")
    private String parentId;

    /**
     * 紧急程度
     */
    @TableField("F_URGENT")
    private Integer urgent;

    /**
     * 流程主键
     */
    @TableField("F_TEMPLATE_ID")
    private String templateId;

    /**
     * 流程版本主键
     */
    @TableField("F_FLOW_ID")
    private String flowId;

    /**
     * 流程引擎实例id
     */
    @TableField("F_INSTANCE_ID")
    private String instanceId;
    /**
     * 流程引擎类型;1.flowable,2,activity,3.camunda
     */
    @TableField("F_ENGINE_TYPE")
    private Integer engineType;

    /**
     * 委托用户
     */
    @TableField("f_delegate_user_id")
    private String delegateUserId;
    /**
     * 开始时间
     */
    @TableField("f_start_time")
    private Date startTime;
    /**
     * 结束时间
     */
    @TableField("f_end_time")
    private Date endTime;
    /**
     * 当前节点名称
     */
    @TableField("f_current_node_name")
    private String currentNodeName;
    /**
     * 当前节点编码
     */
    @TableField("f_current_node_code")
    private String currentNodeCode;
    /**
     * 冻结审批，退回前的任务、经办的信息
     */
    @TableField("f_reject_data_id")
    private String rejectDataId;
    /**
     * 子流程节点编码
     */
    @TableField("f_sub_code")
    private String subCode;
    /**
     * 全局参数
     */
    @TableField("f_global_parameter")
    private String globalParameter;
    /**
     * 是否能恢复（0：能，1：不能）
     */
    @TableField("F_RESTORE")
    private Integer isRestore;
    /**
     * 是否归档（null：未配置，0：未归档，1：已归档）
     */
    @TableField("F_IS_FILE")
    private Integer isFile;
    /**
     * 类型(0-功能 1-发起)
     */
    @TableField("F_TYPE")
    private Integer type;
    /**
     * 自由流程初始节点编码
     */
    @TableField("F_FREE_CODE")
    private String freeCode;
}

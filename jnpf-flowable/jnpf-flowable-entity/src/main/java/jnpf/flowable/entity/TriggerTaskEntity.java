package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 触发任务
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/10 16:52
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_trigger_task")
public class TriggerTaskEntity extends SuperExtendEntity<String> {
    /**
     * 标题
     */
    @TableField("F_FULL_NAME")
    private String fullName;
    /**
     * 重试任务开始时间
     */
    @TableField("F_PARENT_TIME")
    private Date parentTime;
    /**
     * 重试任务主键id
     */
    @TableField("F_PARENT_ID")
    private String parentId;
    /**
     * 标准任务主键
     */
    @TableField("F_TASK_ID")
    private String taskId;
    /**
     * 节点编码
     */
    @TableField("F_NODE_CODE")
    private String nodeCode;
    /**
     * 节点id
     */
    @TableField("F_NODE_ID")
    private String nodeId;
    /**
     * 同步异步
     */
    @TableField("F_IS_ASYNC")
    private Integer isAsync;
    /**
     * 开始时间
     */
    @TableField("F_START_TIME")
    private Date startTime;
    /**
     * 模板id
     */
    @TableField("F_FLOW_ID")
    private String flowId;
    /**
     * 数据
     */
    @TableField("F_DATA")
    private String data;
    /**
     * 数据主键
     */
    @TableField("F_DATA_ID")
    private String dataId;
    /**
     * 引擎主键
     */
    @TableField("F_INSTANCE_ID")
    private String instanceId;
    /**
     * 流程引擎类型;1.flowable,2,activity,3.camunda
     */
    @TableField("F_ENGINE_TYPE")
    private Integer engineType;
    /**
     * 状态
     */
    @TableField("F_STATUS")
    private Integer status;

}

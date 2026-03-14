package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/10 17:06
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_trigger_record")
public class TriggerRecordEntity extends SuperExtendEntity<String> {
    /**
     * 触发主键
     */
    @TableField("F_TRIGGER_ID")
    private String triggerId;
    /**
     * 任务主键
     */
    @TableField("F_TASK_ID")
    private String taskId;
    /**
     * 节点id
     */
    @TableField("F_NODE_ID")
    private String nodeId;
    /**
     * 节点编号
     */
    @TableField("F_NODE_CODE")
    private String nodeCode;
    /**
     * 节点名称
     */
    @TableField("F_NODE_NAME")
    private String nodeName;
    /**
     * 开始时间
     */
    @TableField("F_START_TIME")
    private Date startTime;
    /**
     * 结束时间
     */
    @TableField("F_END_TIME")
    private Date endTime;
    /**
     * 状态，0-通过 1-异常
     */
    @TableField("F_STATUS")
    private Integer status;
    /**
     * 错误提示
     */
    @TableField("F_ERROR_TIP")
    private String errorTip;
    /**
     * 错误数据
     */
    @TableField("F_ERROR_DATA")
    private String errorData;
}

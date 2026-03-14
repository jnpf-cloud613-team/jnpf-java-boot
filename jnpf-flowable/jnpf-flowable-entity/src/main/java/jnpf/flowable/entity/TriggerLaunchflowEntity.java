package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 15:50
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_trigger_launchflow")
public class TriggerLaunchflowEntity extends SuperExtendEntity<String> {
    /**
     * 触发id
     */
    @TableField("f_trigger_id")
    private String triggerId;
    /**
     * 任务id
     */
    @TableField("f_task_id")
    private String taskId;
    /**
     * 节点id
     */
    @TableField("f_node_id")
    private String nodeId;
    /**
     * 记录id
     */
    @TableField("f_record_id")
    private String recordId;
    /**
     * 节点编码
     */
    @TableField("f_node_code")
    private String nodeCode;
    /**
     * 节点名称
     */
    @TableField("f_node_name")
    private String nodeName;
    /**
     * 任务节点模型
     */
    @TableField("f_event_model")
    private String eventModel;
    /**
     * 任务节点模型
     */
    @TableField("f_extra_data")
    private String extraData;
    /**
     * 任务节点模型
     */
    @TableField("f_task_ids")
    private String taskIds;
    /**
     * 分组id
     */
    @TableField("f_group_id")
    private String groupId;
    /**
     * 比例值
     */
    @TableField("f_ratio")
    private Integer ratio;
}

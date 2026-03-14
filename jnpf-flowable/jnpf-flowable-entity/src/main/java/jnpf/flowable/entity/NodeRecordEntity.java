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
 * @since 2024/6/25 17:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_node_record")
public class NodeRecordEntity extends SuperExtendEntity<String> {
    /**
     * 任务id
     */
    @TableField("F_TASK_ID")
    private String taskId;
    /**
     * 节点id
     */
    @TableField("F_NODE_ID")
    private String nodeId;
    /**
     * 节点编码
     */
    @TableField("F_NODE_CODE")
    private String nodeCode;
    /**
     * 节点名称
     */
    @TableField("F_NODE_NAME")
    private String nodeName;
    /**
     * 节点状态，1-已提交 2-已通过 3-已拒绝 4-审批中 5-已退回 6-已撤回 7-等待中
     */
    @TableField("F_NODE_STATUS")
    private Integer nodeStatus;
}

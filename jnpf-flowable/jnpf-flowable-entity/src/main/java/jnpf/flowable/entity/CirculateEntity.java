package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/4/25 下午6:12
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_circulate")
public class CirculateEntity  extends SuperExtendEntity<String> {

    /**
     * 用户主键
     */
    @TableField("f_user_id")
    private String userId;

    /**
     * 任务主键
     */
    @TableField("f_task_id")
    private String taskId;

    /**
     * 节点编号
     */
    @TableField("f_node_code")
    private String nodeCode;

    /**
     * 节点名称
     */
    @TableField("f_node_name")
    private String nodeName;

    /**
     * 节点主键
     */
    @TableField("f_node_id")
    private String nodeId;

    /**
     * 经办主键
     */
    @TableField("f_operator_id")
    private String operatorId;

    /**
     * 是否已读
     */
    @TableField("f_read")
    private Integer circulateRead;
}

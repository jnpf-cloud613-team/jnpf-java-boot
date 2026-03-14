package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_event_log")
public class EventLogEntity extends SuperExtendEntity<String> {
    /**
     * 任务主键
     */
    @TableField("f_task_id")
    private String taskId;
    /**
     * 节点主建
     */
    @TableField("f_node_id")
    private String nodeId;
    /**
     * 节点名称
     */
    @TableField("f_node_name")
    private String nodeName;
    /**
     * 节点编码
     */
    @TableField("f_node_code")
    private String nodeCode;
    /**
     * 事件类型
     */
    @TableField("f_type")
    private String type;
    /**
     * 上一节点
     */
    @TableField("f_up_node")
    private String upNode;
    /**
     * 接口主建
     */
    @TableField("f_interface_id")
    private String interfaceId;
    /**
     * 执行参数
     */
    @TableField("f_data")
    private String data;
    /**
     * 执行结果
     */
    @TableField("f_result")
    private String result;
    /**
     * 执行状态 0.成功 1.失败
     */
    @TableField("f_status")
    private Integer status;
}

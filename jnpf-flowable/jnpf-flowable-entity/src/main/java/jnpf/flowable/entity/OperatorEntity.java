package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 经办
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 15:10
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_operator")
public class OperatorEntity extends SuperExtendEntity<String> implements Serializable {
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
     * 任务id
     */
    @TableField("f_task_id")
    private String taskId;
    /**
     * 节点id，更新节点是先删除后添加，所以节点id待删除
     */
    @TableField("f_node_id")
    private String nodeId;
    /**
     * 加签经办父级id
     */
    @TableField("f_parent_id")
    private String parentId;
    /**
     * 处理时间
     */
    @TableField("f_handle_time")
    private Date handleTime;
    /**
     * 处理人id
     */
    @TableField("f_handle_id")
    private String handleId;
    /**
     * 全部处理人
     */
    @TableField("f_handle_all")
    private String handleAll;
    /**
     * 处理状态，同意、拒绝
     */
    @TableField("f_handle_status")
    private Integer handleStatus;
    /**
     * 处理参数，加签信息
     */
    @TableField("f_handle_parameter")
    private String handleParameter;
    /**
     * 开始处理时间
     */
    @TableField("f_start_handle_time")
    private Date startHandleTime;
    /**
     * 签收时间
     */
    @TableField("f_sign_time")
    private Date signTime;
    /**
     * 截止时间
     */
    @TableField("f_duedate")
    private Date duedate;
    /**
     * 状态
     */
    @TableField("f_status")
    private Integer status;
    /**
     * 是否完成，有操作（如加签、同意等）变1，加签经办完成后变0
     */
    @TableField("f_completion")
    private Integer completion;
    /**
     * 流程引擎类型;1.flowable,2,activity,3.camunda
     */
    @TableField("f_engine_type")
    private Integer engineType;
    /**
     * 草稿数据
     */
    @TableField("f_draft_data")
    private String draftData;
    /**
     * 是否办理节点(0否 1是)
     */
    @TableField("f_is_processing")
    private Integer isProcessing;
}

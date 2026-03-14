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
@TableName("workflow_candidates")
public class CandidatesEntity extends SuperExtendEntity<String> {
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
     * 审批人id
     */
    @TableField("f_handle_id")
    private String handleId;
    /**
     * 审批人账号
     */
    @TableField("f_account")
    private String account;
    /**
     * 候选人
     */
    @TableField("f_candidates")
    private String candidates;
    /**
     * 经办主键
     */
    @TableField("f_operator_id")
    private String operatorId;
    /**
     * 审批类型(1-候选人 2-异常处理人)
     */
    @TableField("f_type")
    private Integer type;
}

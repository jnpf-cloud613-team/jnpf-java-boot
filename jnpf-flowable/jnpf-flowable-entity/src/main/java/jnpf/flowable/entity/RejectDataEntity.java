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
 * @since 2024/5/8 17:57
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_reject_data")
public class RejectDataEntity extends SuperExtendEntity<String> {
    /**
     * 任务json
     */
    @TableField("F_TASK_JSON")
    private String taskJson;
    /**
     * 经办json
     */
    @TableField("F_OPERATOR_JSON")
    private String operatorJson;
    /**
     * 外部json
     */
    @TableField("F_EVENT_LOG_JSON")
    private String eventLogJson;
    /**
     * 节点编码
     */
    @TableField("F_NODE_CODE")
    private String nodeCode;
}

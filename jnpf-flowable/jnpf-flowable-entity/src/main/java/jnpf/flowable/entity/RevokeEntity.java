package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 撤销
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/23 17:27
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_revoke")
public class RevokeEntity extends SuperExtendEntity<String> {
    /**
     * 任务主键
     */
    @TableField("F_TASK_ID")
    private String taskId;
    /**
     * 撤销任务主键
     */
    @TableField("F_REVOKE_TASK_ID")
    private String revokeTaskId;
    /**
     * 表单数据
     */
    @TableField("F_FORM_DATA")
    private String formData;
}

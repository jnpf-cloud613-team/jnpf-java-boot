package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务条件
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/8/23 17:27
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_task_line")
public class TaskLineEntity extends SuperExtendEntity<String> {
    /**
     * 任务主键
     */
    @TableField("F_TASK_ID")
    private String taskId;
    /**
     * 线的键
     */
    @TableField("F_LINE_KEY")
    private String lineKey;
    /**
     * 线的值
     */
    @TableField("F_LINE_VALUE")
    private String lineValue;
}

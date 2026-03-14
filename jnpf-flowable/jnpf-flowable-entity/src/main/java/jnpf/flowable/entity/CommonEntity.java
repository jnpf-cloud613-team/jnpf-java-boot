package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 常用流程
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/22 20:19
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_common")
public class CommonEntity extends SuperExtendEntity<String> {
    /**
     * 对象主键
     */
    @TableField("F_FLOW_ID")
    private String flowId;
}

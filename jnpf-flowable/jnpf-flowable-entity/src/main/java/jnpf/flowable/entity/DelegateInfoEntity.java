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
 * @since 2024/9/2 11:54
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_delegate_info")
public class DelegateInfoEntity extends SuperExtendEntity<String> {
    /**
     * 委托主键
     */
    @TableField("F_DELEGATE_ID")
    private String delegateId;
    /**
     * 被委托人id
     */
    @TableField("F_TO_USER_ID")
    private String toUserId;
    /**
     * 被委托人
     */
    @TableField("F_TO_USER_NAME")
    private String toUserName;
    /**
     * 状态(0.待确认 1.已接受 2.已拒绝)
     */
    @TableField("F_STATUS")
    private Integer status;
}

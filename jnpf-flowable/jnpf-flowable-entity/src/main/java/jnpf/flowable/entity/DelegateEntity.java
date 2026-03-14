package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 流程委托
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_delegate")
public class DelegateEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

    /**
     * 委托人id
     */
    @TableField("F_USER_ID")
    private String userId;
    /**
     * 委托人
     */
    @TableField("F_USER_NAME")
    private String userName;

    /**
     * 委托类型（0-发起委托，1-审批委托）
     */
    @TableField("F_TYPE")
    private Integer type;

    /**
     * 委托流程
     */
    @TableField("F_FLOW_ID")
    private String flowId;
    /**
     * 委托流程名称
     */
    @TableField("F_FLOW_NAME")
    private String flowName;

    /**
     * 开始时间
     */
    @TableField("F_START_TIME")
    private Date startTime;

    /**
     * 结束时间
     */
    @TableField("F_END_TIME")
    private Date endTime;

}

package jnpf.flowable.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程发起用户信息
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 9:36
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("workflow_launch_user")
public class LaunchUserEntity extends SuperExtendEntity<String> {
    /**
     * 组织主键
     */
    @TableField("f_organize_id")
    private String organizeId;
    /**
     * 岗位主键
     */
    @TableField("f_position_id")
    private String positionId;
    /**
     * 主管主键
     */
    @TableField("f_manager_id")
    private String managerId;
    /**
     * 下属用户
     */
    @TableField("f_subordinate")
    private String subordinate;
    /**
     * 任务主键
     */
    @TableField("f_task_id")
    private String taskId;
    /**
     * 公司下所有部门
     */
    @TableField("f_department")
    private String department;

    /**
     * 节点编码
     */
    @TableField("f_node_code")
    private String nodeCode;

    /**
     * 发起类型 1-任务发起 2-逐级发起
     */
    @TableField("f_type")
    private Integer type;
}

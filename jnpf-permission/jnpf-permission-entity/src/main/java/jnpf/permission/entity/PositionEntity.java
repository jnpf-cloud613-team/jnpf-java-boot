package jnpf.permission.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 岗位信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName("base_position")
public class PositionEntity extends PermissionEntityBase {
    /**
     * 是否默认岗位（0-否，1-是）
     */
    @TableField("F_DEFAULT_MARK")
    private Integer defaultMark;

    /**
     * 责任人
     */
    @TableField(value = "F_DUTY_USER", updateStrategy = FieldStrategy.ALWAYS)
    private String dutyUser;

    /**
     * 岗位类型
     */
    @TableField("F_TYPE")
    private String type;

    /**
     * 机构主键
     */
    @TableField("F_ORGANIZE_ID")
    private String organizeId;

    /**
     * 父级岗位
     */
    @TableField(value = "F_PARENT_ID", updateStrategy = FieldStrategy.ALWAYS)
    private String parentId;

    /**
     * 岗位树形
     */
    @TableField("F_POSITION_ID_TREE")
    private String positionIdTree;

    /**
     * 岗位约束(0-关闭，1启用)
     */
    @TableField("F_IS_CONDITION")
    private Integer isCondition;

    /**
     * 约束内容
     */
    @TableField("F_CONDITION_JSON")
    private String conditionJson;
}

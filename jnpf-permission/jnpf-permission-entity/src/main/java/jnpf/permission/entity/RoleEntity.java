package jnpf.permission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统角色
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Data
@TableName("base_role")
public class RoleEntity extends PermissionEntityBase{

    /**
     * 角色类型：user,Organize,Position
     */
    @TableField("F_TYPE")
    private String type;

    /**
     * 系统角色：1-是，2-自定义
     */
    @TableField("F_GLOBAL_MARK")
    private Integer globalMark;


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

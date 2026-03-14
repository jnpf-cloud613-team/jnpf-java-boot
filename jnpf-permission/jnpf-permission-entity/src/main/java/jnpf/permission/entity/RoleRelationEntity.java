package jnpf.permission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 * 角色关系表
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/2/26 18:12:34
 */
@Data
@TableName("base_Role_relation")
public class RoleRelationEntity extends SuperExtendEntity<String> {

    /**
     * 用户主键
     */
    @TableField("F_ROLE_ID")
    private String roleId;

    /**
     * 对象类型:posistion,organize
     */
    @TableField("F_OBJECT_TYPE")
    private String objectType;

    /**
     * 对象主键：组织主键，岗位主键
     */
    @TableField("F_OBJECT_ID")
    private String objectId;

}

package jnpf.permission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

@Data
@TableName("base_permission_group")
public class PermissionGroupEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

    /**
     * 名称
     */
    @TableField("f_full_name")
    private String fullName;

    /**
     * 编码
     */
    @TableField("F_en_code")
    private String enCode;

    /**
     * 权限成员
     */
    @TableField("F_permission_member")
    private String permissionMember;

    /**
     * 权限类型 1.自定义 0.全部
     */
    @TableField("F_type")
    private Integer type;

}

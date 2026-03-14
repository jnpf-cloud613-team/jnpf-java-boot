package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("base_visual_alias")
public class VisualAliasEntity extends SuperEntity<String> {
    /**
     * 功能表单id
     */
    @TableField("F_VISUAL_ID")
    private String visualId;
    /**
     * 表或字段别名
     */
    @TableField("F_ALIAS_NAME")
    private String aliasName;
    /**
     * 表名称
     */
    @TableField("F_TABLE_NAME")
    private String tableName;
    /**
     * 字段名称
     */
    @TableField("F_FIELD_NAME")
    private String fieldName;
}

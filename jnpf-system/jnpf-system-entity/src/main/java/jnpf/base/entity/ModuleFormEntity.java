package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 *
 * 表单权限
 * 版本： V3.1.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2021-09-14
 */
@Data
@TableName("base_module_form")
public class ModuleFormEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

    /**
     * 表单上级
     */
    @TableField("f_parent_id")
    private String parentId;

    /**
     * 表单名称
     */
    @TableField("f_full_name")
    private String fullName;

    /**
     * 表单编码
     */
    @TableField("f_en_code")
    private String enCode;

    /**
     * 扩展属性
     */
    @TableField("f_property_json")
    private String propertyJson;

    /**
     * 功能主键
     */
    @TableField("f_module_id")
    private String moduleId;

    /**
     * 字段规则 主从
     */
    @TableField("f_field_rule")
    private Integer fieldRule;

    /**
     * 绑定表格Id
     */
    @TableField("f_bind_table")
    private String bindTable;

    /**
     * 子表规则key
     */
    @TableField("f_child_table_key")
    private String childTableKey;

}

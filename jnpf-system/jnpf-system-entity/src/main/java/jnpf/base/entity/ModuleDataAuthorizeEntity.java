package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 数据权限配置
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Data
@TableName("base_module_authorize")
public class ModuleDataAuthorizeEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    /**
     * 字段名称
     */
    @TableField("f_full_name")
    private String fullName;

    /**
     * 时间类型
     */
    @TableField("f_format")
    private String format;

    /**
     * 字段编码
     */
    @TableField("f_en_code")
    private String enCode;

    /**
     * 字段类型
     */
    @TableField("f_type")
    private String type;

    /**
     * 条件符号
     */
    @TableField("f_condition_symbol")
    private String conditionSymbol;

    /**
     * 条件符号Json
     */
    @TableField("f_property_json")
    private String conditionSymbolJson;

    /**
     * 条件内容
     */
    @TableField("f_condition_text")
    private String conditionText;

    /**
     * 扩展属性
     */
    @TableField("f_property_json")
    private String propertyJson;

    /**
     * 菜单主键
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

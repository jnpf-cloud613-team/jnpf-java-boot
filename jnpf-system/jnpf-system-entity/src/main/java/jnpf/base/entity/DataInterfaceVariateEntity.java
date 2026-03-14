package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 数据接口变量
 */
@Data
@TableName("base_data_interface_variate")
public class DataInterfaceVariateEntity extends SuperExtendEntity<String> implements Serializable {

    /**
     * 数据接口id
     */
    @TableField("f_interface_id")
    private String interfaceId;

    /**
     * 变量名
     */
    @TableField("f_full_name")
    private String fullName;

    /**
     * 表达式
     */
    @TableField("f_expression")
    private String expression;

    /**
     * 变量值
     */
    @TableField("f_value")
    private String value;
}

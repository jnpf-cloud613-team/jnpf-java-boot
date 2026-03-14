package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 *
 * 常用字段表
 * 版本： V3.0.0
 * 版权： 引迈信息技术有限公司
 * 作者： 管理员/admin
 * 日期： 2020-07-23 09:54
 */
@Data
@TableName("base_common_fields")
public class ComFieldsEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    @TableField("f_field_name")
    private String fieldName;

    @TableField("f_field")
    private String field;

    @TableField("f_data_type")
    private String datatype;

    @TableField("f_data_length")
    private String datalength;

    @TableField("f_allow_null")
    private Integer allowNull;

}


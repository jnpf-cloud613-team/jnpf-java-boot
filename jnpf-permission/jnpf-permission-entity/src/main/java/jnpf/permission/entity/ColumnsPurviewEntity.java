package jnpf.permission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;


/**
 * 模块列表权限
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/3/15 9:20
 */
@Data
@TableName("base_columns_purview")
public class ColumnsPurviewEntity extends SuperExtendEntity.SuperExtendEnabledEntity<String> {

    /**
     * 列表字段数组
     */
    @TableField("f_field_list")
    private String fieldList;
    /**
     * 模块ID
     */
    @TableField("f_module_id")
    private String moduleId;

}

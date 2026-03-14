package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2024/5/6 上午10:15
 */
@Data
@TableName("base_module_data")
public class ModuleDataEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    /**
     * 功能主键
     */
    @TableField("f_module_id")
    private String moduleId;

    /**
     * 功能类型
     */
    @TableField("f_module_type")
    private String moduleType;

    /**
     * 关联系统id
     */
    @TableField("f_system_id")
    private String systemId;

}

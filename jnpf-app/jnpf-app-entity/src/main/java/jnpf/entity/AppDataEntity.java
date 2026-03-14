package jnpf.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * app常用数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021-08-08
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("base_app_data")
public class AppDataEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

    /**
     * 对象类型
     */
    @TableField("f_object_type")
    private String objectType;

    /**
     * 对象主键
     */
    @TableField("f_object_id")
    private String objectId;

    /**
     * 数据
     */
    @TableField("f_object_data")
    private String objectData;

    /**
     * 关联系统id
     */
    @TableField("f_system_id")
    private String systemId;

}

package jnpf.permission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 * 类功能
 *
 * @author JNPF开发平台组 YanYu
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2022/1/27
 */
@Data
public class PermissionEntityBase extends SuperExtendEntity.SuperExtendDEEntity<String> {

    /**
     * 名称
     */
    @TableField("f_full_name")
    private String fullName;

    /**
     * 编码
     */
    @TableField("f_en_code")
    private String enCode;

    /**
     * 扩展属性
     */
    @TableField("f_property_json")
    private String propertyJson;

}


package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 按钮权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Data
@TableName("base_module_button")
public class ModuleButtonEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    /**
     * 按钮上级
     */
    @TableField("f_parent_id")
    private String parentId;

    /**
     * 按钮名称
     */
    @TableField("f_full_name")
    private String fullName;

    /**
     * 按钮编码
     */
    @TableField("f_en_code")
    private String enCode;

    /**
     * 按钮图标
     */
    @TableField("f_icon")
    private String icon;

    /**
     * 请求地址
     */
    @TableField("f_url_address")
    private String urlAddress;

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

}

package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统功能
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
@Data
@TableName("base_module")
public class ModuleEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {

    /**
     * 功能上级
     */
    @TableField("f_parent_id")
    private String parentId;

    /**
     * 功能类别：1-目录 2-页面 3-功能 4-字典 5-报表（原） 6-大屏 7-外链 8-门户, 9-流程，10-报表，11-回传
     */
    @TableField("f_type")
    private Integer type;

    /**
     * 功能名称
     */
    @TableField("f_full_name")
    private String fullName;

    /**
     * 功能编码
     */
    @TableField("f_en_code")
    private String enCode;

    /**
     * 路由地址
     */
    @TableField("f_url_address")
    private String urlAddress;

    /**
     * 页面地址
     */
    @TableField("f_page_address")
    private String pageAddress;

    /**
     * 按钮权限
     */
    @TableField("f_is_button_authorize")
    private Integer isButtonAuthorize;

    /**
     * 列表权限
     */
    @TableField("f_is_column_authorize")
    private Integer isColumnAuthorize;

    /**
     * 数据权限
     */
    @TableField("f_is_data_authorize")
    private Integer isDataAuthorize;

    /**
     * 表单权限
     */
    @TableField("f_is_form_authorize")
    private Integer isFormAuthorize;

    /**
     * 扩展属性
     */
    @TableField("f_property_json")
    private String propertyJson;

    /**
     * 菜单图标
     */
    @TableField("f_icon")
    private String icon;
    /**
     * 链接目标
     */
    @TableField("f_link_target")
    private String linkTarget;
    /**
     * 菜单分类 Web、App
     */
    @TableField("f_category")
    private String category;
    /**
     * 关联功能id
     */
    @TableField("f_module_id")
    private String moduleId;

    /**
     * 关联系统id
     */
    @TableField("f_system_id")
    private String systemId;

    /**
     * 是否隐藏：0-否，1-是隐藏
     */
    @TableField("f_no_show")
    private Integer noShow;

}

package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Data
@TableName("base_system")
public class SystemEntity extends SuperExtendEntity.SuperExtendDEEntity<String> implements Serializable {
    /**
     * 系统名称
     */
    @TableField("F_FULL_NAME")
    private String fullName;

    /**
     * 系统编号
     */
    @TableField("F_EN_CODE")
    private String enCode;

    /**
     * 门户数组
     */
    @TableField("F_PORTAL_ID")
    private String portalId;

    /**
     * app门户数组
     */
    @TableField("F_APP_PORTAL_ID")
    private String appPortalId;

    /**
     * 系统图标
     */
    @TableField("F_ICON")
    private String icon;

    /**
     * 是否是主系统（0-不是，1-是）
     */
    @TableField("F_IS_MAIN")
    private Integer isMain;

    /**
     * 扩展属性
     */
    @TableField("F_PROPERTY_JSON")
    private String propertyJson;

    /**
     * 偏好配置
     */
    @TableField("F_PREFERENCE_JSON")
    private String preferenceJson;

    /**
     * 图标背景色
     */
    @TableField("F_BACKGROUND_COLOR")
    private String backgroundColor;

    /**
     * 所属用户
     */
    @TableField("F_USER_ID")
    private String userId;

    /**
     * 授权id列表
     */
    @TableField("F_AUTHORIZE_ID")
    private String authorizeId;

}

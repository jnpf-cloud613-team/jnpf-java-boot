package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 在线开发表单外链实体
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/12/30 11:10:44
 */
@Data
@TableName("base_visual_link")
public class VisualdevShortLinkEntity extends SuperExtendEntity.SuperExtendEnabledEntity<String> {

    /**
     * 短链接
     */
    @TableField("F_SHORT_LINK")
    private String shortLink;

    /**
     * 外链填单开关
     */
    @TableField("F_FORM_USE")
    private Integer formUse;

    /**
     * 外链填单
     */
    @TableField("F_FORM_LINK")
    private String formLink;

    /**
     * 外链密码开关
     */
    @TableField("F_FORM_PASS_USE")
    private Integer formPassUse;

    /**
     * 外链填单密码
     */
    @TableField("F_FORM_PASSWORD")
    private String formPassword;

    /**
     * 公开查询开关
     */
    @TableField("F_COLUMN_USE")
    private Integer columnUse;

    /**
     * 公开查询
     */
    @TableField("F_COLUMN_LINK")
    private String columnLink;

    /**
     * 查询密码开关
     */
    @TableField("F_COLUMN_PASS_USE")
    private Integer columnPassUse;

    /**
     * 公开查询密码
     */
    @TableField("F_COLUMN_PASSWORD")
    private String columnPassword;

    /**
     * 查询条件
     */
    @TableField("F_COLUMN_CONDITION")
    private String columnCondition;

    /**
     * 显示内容
     */
    @TableField("F_COLUMN_TEXT")
    private String columnText;

    /**
     * PC端链接
     */
    @TableField("F_REAL_PC_LINK")
    private String realPcLink;

    /**
     * App端链接
     */
    @TableField("F_REAL_APP_LINK")
    private String realAppLink;

    /**
     * 用户id
     */
    @TableField("F_USER_ID")
    private String userId;

}

package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 打印模板-实体类
 *
 * @author JNPF开发平台组 YY
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月30日
 */
@Data
@EqualsAndHashCode
@TableName("base_print_template")
public class PrintDevEntity extends SuperExtendEntity.SuperExtendDescriptionEntity<String> {

    /**
     * 名称
     */
    @TableField("F_FULL_NAME")
    private String fullName;

    /**
     * 编码
     */
    @TableField("F_EN_CODE")
    private String enCode;

    /**
     * 分类
     */
    @TableField("F_CATEGORY")
    private String category;


    /**
     * 状态：0-未发布，1-已发布，2-已修改
     */
    @TableField("F_STATE")
    private Integer state;


    /**
     * 通用-将该模板设为通用(0-表单用，1-业务打印模板用)
     */
    @TableField("F_COMMON_USE")
    private Integer commonUse;

    /**
     * 发布范围：1-公开，2-权限设置
     */
    @TableField("F_VISIBLE_TYPE")
    private Integer visibleType;

    /**
     * 图标
     */
    @TableField("F_ICON")
    private String icon;

    /**
     * 图标颜色
     */
    @TableField("F_ICON_BACKGROUND")
    private String iconBackground;

    /**
     * 系统id
     */
    @TableField("F_SYSTEM_ID")
    private String systemId;
}

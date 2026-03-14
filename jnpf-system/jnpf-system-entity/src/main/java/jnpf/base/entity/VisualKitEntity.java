package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 表单套件
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/22 10:29:10
 */
@Data
@TableName("base_visual_kit")
public class VisualKitEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {
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
     * 分类（数据字典）
     */
    @TableField("F_CATEGORY")
    private String category;

    /**
     * 图标
     */
    @TableField("F_ICON")
    private String icon;

    /**
     * 套件设计内容
     */
    @TableField("F_FORM_DATA")
    private String formData;

}

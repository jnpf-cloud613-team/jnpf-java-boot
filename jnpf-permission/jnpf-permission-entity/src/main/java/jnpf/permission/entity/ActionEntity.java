package jnpf.permission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("base_action")
public class ActionEntity extends SuperExtendEntity<String> {

    /**
     * 动作名称
     */
    @TableField("f_full_name")
    private String fullName;

    /**
     * 动作编码
     */
    @TableField("f_en_code")
    private String enCode;

    /**
     * 说明
     */
    @TableField("f_description")
    private String description;


    /**
     * 是否系统（1-自定义 2-自定义）
     */
    @TableField("f_type")
    private Integer type;

    /**
     * 启用
     */
    @TableField("f_enabled_mark")
    private String enabledMark;
}

package jnpf.permission.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import jnpf.base.entity.SuperExtendEntity;
import lombok.Data;

/**
 * 身份管理
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/4 18:07:42
 */
@Data
@TableName("base_standing")
public class StandingEntity extends SuperExtendEntity.SuperExtendDEEntity<String> {

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
     * 是否系统（1-系统 2-自定义）
     */
    @TableField("F_IS_SYSTEM")
    private Integer isSystem;

}

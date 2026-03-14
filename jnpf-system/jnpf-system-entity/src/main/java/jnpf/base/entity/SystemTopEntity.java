package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 应用置顶Entity
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025-09-05
 */
@Data
@TableName("base_system_top")
public class SystemTopEntity extends SuperEntity<String> {
    /**
     * 用户id
     */
    @TableField("F_USER_ID")
    private String userId;
    /**
     * 用户身份id
     */
    @TableField("F_STAND_ID")
    private String standId;
    /**
     * 对象主键
     */
    @TableField("F_OBJECT_ID")
    private String objectId;
    /**
     * 置顶类型（home-首页，appCenter-应用中心）
     */
    @TableField("F_TYPE")
    private String type;
}

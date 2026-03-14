package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 跨应用数据entity
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/30 14:26:19
 */
@Data
@TableName("base_system_share")
public class SystemShareEntity extends SuperExtendEntity<String> {
    /**
     * 当前系统id
     */
    @TableField("F_SYSTEM_ID")
    private String systemId;

    /**
     * 来源系统id
     */
    @TableField("F_SOURCE_ID")
    private String sourceId;

    /**
     * 对象主键(当前是在线开发功能id)
     */
    @TableField("F_OBJECT_ID")
    private String objectId;
}

package jnpf.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version v5.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/2/20 17:33:31
 */
@Data
@TableName("base_ai_chat")
public class AiChatEntity extends SuperEntity<String> {
    /**
     * 会话标题
     */
    @TableField("F_FULL_NAME")
    private String fullName;
}

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
@TableName("base_ai_history")
public class AiHistoryEntity extends SuperEntity<String> {
    /**
     * 会话id
     */
    @TableField("F_CHAT_ID")
    private String chatId;

    /**
     * 问题
     */
    @TableField("F_QUESTION_TEXT")
    private String questionText;

    /**
     * 会话内容
     */
    @TableField("F_CONTENT")
    private String content;

    /**
     * 类型：0-ai，1-用户
     */
    @TableField("F_TYPE")
    private Integer type;
}

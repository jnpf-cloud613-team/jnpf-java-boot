package jnpf.base.model.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version v5.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/2/20 18:07:04
 */
@Data
@Schema(description = "ai会话模型")
public class AiChatVo {
    @Schema(description = "会话id")
    private String id;
    @Schema(description = "会话标题")
    private String fullName;
    @Schema(description = "创建时间")
    private Long creatorTime;
}

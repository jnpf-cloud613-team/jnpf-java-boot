package jnpf.base.model.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version v5.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/2/21 9:49:27
 */
@Data
@Schema(description = "对话记录")
public class AiHisVo {
    @Schema(description = "记录id")
    private String id;
    @Schema(description = "问题内容")
    private String questionText;
    @Schema(description = "对话内容")
    private String content;
    @Schema(description = "类型：0-ai，1-用户")
    private Integer type;
    @Schema(description = "创建时间")
    private Long creatorTime;
}

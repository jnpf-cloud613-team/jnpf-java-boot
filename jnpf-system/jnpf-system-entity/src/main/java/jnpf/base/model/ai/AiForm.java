package jnpf.base.model.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version v5.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/2/21 9:49:33
 */
@Data
@Schema(description = "会话id")
public class AiForm {
    @Schema(description = "会话id")
    private String id;
    @Schema(description = "会话标题")
    private String fullName;

    @Schema(description = "对话记录列表")
    private List<AiHisVo> data;
}

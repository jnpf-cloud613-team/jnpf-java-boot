package jnpf.base.model.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version v5.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/2/20 17:18:16
 */
@Data
@Schema(description = "ai参数模型")
public class AiParam extends Pagination {
    @Schema(description = "会话id")
    public String id;
}

package jnpf.onlinedev.model.personal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 个性化设置json属性
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/11/5 18:02:11
 */
@Data
@Schema(description = "个性化设置json属性")
public class VisualPersonalJson {
    @Schema(description = "字段名称")
    private String label;
    @Schema(description = "字段key")
    private String key;
    @Schema(description = "是否展示")
    private boolean show;
    @Schema(description = "对齐方式")
    private String fixed;
}

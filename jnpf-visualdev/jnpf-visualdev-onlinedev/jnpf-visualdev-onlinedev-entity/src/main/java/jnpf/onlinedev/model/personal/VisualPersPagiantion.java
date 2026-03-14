package jnpf.onlinedev.model.personal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 个性化视图参数对象
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/11/6 10:08:30
 */
@Data
@Schema(description = "个性化视图参数对象")
public class VisualPersPagiantion {

    @Schema(description = "菜单id")
    private String menuId;

    @Schema(description = "功能id")
    private String modelId;
}

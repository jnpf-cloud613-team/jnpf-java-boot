package jnpf.permission.model.authorize;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class DataValuesQuery {

    @Schema(description = "类型")
    private String type;
    @Schema(description = "菜单id集合")
    private String moduleIds;
    @Schema(description = "类型：organize,role")
    private String objectType;
}

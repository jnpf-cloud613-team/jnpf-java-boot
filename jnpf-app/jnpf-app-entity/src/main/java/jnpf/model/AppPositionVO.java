package jnpf.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * app应用
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021-08-08
 */
@Data
@Schema(description = "常用模型")
public class AppPositionVO {
    @Schema(description = "岗位id")
    private String id;
    @Schema(description = "岗位名称")
    private String name;
}

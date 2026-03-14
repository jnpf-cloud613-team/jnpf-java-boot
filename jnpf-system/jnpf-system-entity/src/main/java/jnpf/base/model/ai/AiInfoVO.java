package jnpf.base.model.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:30
 */
@Data
public class AiInfoVO {
    @Schema(description = "id")
    private String id;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "模型")
    private String model;
    @Schema(description = "路径")
    private String baseUrl;
    @Schema(description = "凭证")
    private String credential;
    @Schema(description = "状态(0-禁用，1-启用)")
    private Integer enabledMark;
}

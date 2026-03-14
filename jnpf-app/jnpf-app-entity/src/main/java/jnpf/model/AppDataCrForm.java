package jnpf.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * app常用数据
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021-07-08
 */
@Data
@Schema(description = "常用模型")
public class AppDataCrForm {
    @NotBlank(message = "必填")
    @Schema(description = "应用类型")
    private String objectType;
    @NotBlank(message = "必填")
    @Schema(description = "应用主键")
    private String objectId;
    @Schema(description = "数据")
    private String objectData;
    @Schema(description = "系统主键")
    private String systemId;
}

package jnpf.base.model.comfields;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class ComFieldsCrForm {
    @Schema(description = "字段名")
    @NotBlank(message = "必填")
    private String fieldName;
    @Schema(description = "字段")
    @NotBlank(message = "必填")
    private String field;
    @Schema(description = "类型")
    @NotBlank(message = "必填")
    private String dataType;
    @Schema(description = "长度")
    @NotBlank(message = "必填")
    private String dataLength;
    @Schema(description = "是否必填")
    @NotNull(message = "必填")
    private Integer allowNull;
}

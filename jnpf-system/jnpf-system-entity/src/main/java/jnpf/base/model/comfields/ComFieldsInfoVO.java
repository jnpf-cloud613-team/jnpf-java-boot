package jnpf.base.model.comfields;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class ComFieldsInfoVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "字段名")
    private String fieldName;
    @Schema(description = "类型")
    private String dataType;
    @Schema(description = "字段")
    @NotBlank(message = "必填")
    private String field;
    @Schema(description = "长度")
    private String dataLength;
    @Schema(description = "是否必填")
    private Integer allowNull;
    @Schema(description = "创建时间")
    private long creatorTime;
}

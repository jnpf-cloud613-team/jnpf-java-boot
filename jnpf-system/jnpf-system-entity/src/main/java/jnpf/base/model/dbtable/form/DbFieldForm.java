package jnpf.base.model.dbtable.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 表字段表单信息
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class DbFieldForm {

    @NotBlank(message = "必填")
    @Schema(description = "字段名")
    private String field;

    @NotBlank(message = "必填")
    @Schema(description = "字段说明")
    private String fieldName;

    @NotBlank(message = "必填")
    @Schema(description = "数据类型")
    private String dataType;

    @NotBlank(message = "必填")
    @Schema(description = "数据长度")
    private String dataLength;

    @NotNull(message = "必填")
    @Schema(description = "允许空")
    private Integer allowNull;

    @NotBlank(message = "必填")
    @Schema(description = "插入位置")
    private String index;

    @Schema(description = "主键")
    private Integer primaryKey;


    @Schema(description = "是否自增 内部使用")
    private Integer autoIncrement;

    @Getter
    @Schema(description = "是否自增")
    private Integer identity;


}

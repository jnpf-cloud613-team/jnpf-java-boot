package jnpf.base.model.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021-09-14
 */
@Data
public class FormBatchForm {
    @Schema(description = "菜单id")
    @NotBlank(message = "必填")
    private String moduleId;
    @Schema(description = "数据")
    private Object formJson;
    @Schema(description = "排序码")
    private Long sortCode;
    @Schema(description = "规则")
    private Integer fieldRule;
    @Schema(description = "绑定表")
    private String bindTable;
}

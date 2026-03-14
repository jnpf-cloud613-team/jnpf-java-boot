package jnpf.base.model.dictionarydata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class DictionaryDataCrForm {
    @NotBlank(message = "必填")
    @Schema(description = "项目代码")
    private String enCode;

    @Schema(description = "有效标志")
    private Integer enabledMark;

    @NotBlank(message = "必填")
    @Schema(description = "上级项目名称")
    private String fullName;

    @Schema(description = "说明")
    private String description;

    @NotBlank(message = "必填")
    @Schema(description = "上级id,没有传0")
    private String parentId;
    @Schema(description = "分类id")
    private String dictionaryTypeId;
    @Schema(description = "排序码")
    private long sortCode;
}

package jnpf.base.model.button;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author 引迈
 */
@Data
public class ModuleButtonCrForm {
    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "有效标志")
    private Integer enabledMark;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "父级id")
    private String parentId;
    @Schema(description = "菜单id")
    private String moduleId;
    @Schema(description = "排序码")
    private Long sortCode;
}

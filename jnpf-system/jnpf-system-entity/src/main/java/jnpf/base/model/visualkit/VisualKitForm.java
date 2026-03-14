package jnpf.base.model.visualkit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 套件表单信息
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/22 11:50:38
 */
@Data
@Schema(description = "套件表单信息")
public class VisualKitForm {

    @Schema(description = "id")
    private String id;

    @NotBlank
    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "编码")
    private String enCode;

    @NotBlank
    @Schema(description = "分类（数据字典）")
    private String category;

    @NotBlank
    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Long sortCode;

    @Schema(description = "状态")
    private Integer enabledMark;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "套件设计内容")
    private String formData;
}

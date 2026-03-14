package jnpf.flowable.model.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/25 19:41
 */
@Data
public class TemplateTreeListVo {
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "数量")
    private Integer num;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "主键")
    private String id;
    @Schema(description = "流程分类")
    private String category;
    @Schema(description = "排序码")
    private Long sortCode;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "图标背景色")
    private String iconBackground;
    @Schema(description = "有效标志")
    private Integer enabledMark;
    @Schema(description = "是否选择")
    private Boolean disabled = false;
    @Schema(description = "子节点")
    private List<TemplateTreeListVo> children;
}

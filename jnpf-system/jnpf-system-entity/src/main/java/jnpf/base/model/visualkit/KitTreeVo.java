package jnpf.base.model.visualkit;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "套件树形属性")
public class KitTreeVo {

    @Schema(description = "id")
    private String id;

    @Schema(description = "parentId")
    private String parentId;

    @Schema(description = "是否有子数据")
    private boolean hasChildren = false;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "分类（数据字典）")
    private String category;

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

    @Schema(description = "套件设计内容")
    private List<KitTreeVo> children;
}

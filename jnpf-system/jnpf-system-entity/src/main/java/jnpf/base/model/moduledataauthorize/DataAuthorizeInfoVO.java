package jnpf.base.model.moduledataauthorize;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DataAuthorizeInfoVO {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "分类")
    private String type;

    @Schema(description = "条件")
    private String conditionSymbol;

    @Schema(description = "条件文本")
    private String conditionText;
    @Schema(description = "菜单id")
    private String moduleId;

    @Schema(description = "说明")
    private String description;
    @Schema(description = "规则")
    private Integer fieldRule;
    @Schema(description = "绑定表")
    private String bindTable;
    private String childTableKey;
    private String format;
}

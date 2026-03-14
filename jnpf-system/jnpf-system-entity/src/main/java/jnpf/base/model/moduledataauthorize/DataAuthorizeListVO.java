package jnpf.base.model.moduledataauthorize;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DataAuthorizeListVO {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "时间格式")
    private String format;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "分类")
    private String type;

    @Schema(description = "条件")
    private String conditionSymbol;

    @Schema(description = "条件名称")
    private String conditionSymbolName;

    @Schema(description = "条件文本")
    private String conditionText;

    @Schema(description = "条件")
    private String conditionName;

    @Schema(description = "绑定表")
    private String bindTable;

    @Schema(description = "规则")
    private String fieldRule;

    @Schema(description = "关联字段")
    private String childTableKey;
}

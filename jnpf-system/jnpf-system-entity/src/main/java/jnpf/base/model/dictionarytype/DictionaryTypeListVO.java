package jnpf.base.model.dictionarytype;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DictionaryTypeListVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "父级id")
    private String parentId;
    @Schema(description = "是否有下级")
    private Boolean hasChildren;
    @Schema(description = "是否为树")
    private Integer isTree;
    @Schema(description = "子集集合")
    private List<DictionaryTypeListVO> children = new ArrayList<>();
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "排序码")
    private long sortCode;
    @Schema(description = "类型")
    private String category;
}

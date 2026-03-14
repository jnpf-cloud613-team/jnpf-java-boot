package jnpf.model.document;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class DocumentFolderTreeVO {
    @Schema(description ="名称")
    private String fullName;
    @Schema(description ="图标")
    private String icon;
    @Schema(description ="是否有下级菜单")
    private Boolean hasChildren;
    @Schema(description ="主键")
    private String id;
    @Schema(description ="父级主键")
    private String parentId;

    @Schema(description ="子数据")
    private List<DocumentFolderTreeVO> children;
}

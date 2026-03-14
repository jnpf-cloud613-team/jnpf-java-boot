package jnpf.permission.model.organize;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

@Data
@Schema(description = "组织参数")
public class OrganizePagination extends Pagination {
    @Schema(description = "状态")
    private Integer enabledMark;
    @Schema(description = "类型")
    private String category;
    @Schema(description = "查询key")
    private String[] selectKey;
    @Schema(description = "菜单id")
    private String moduleId;
    @Schema(description = "父级id")
    private String parentId;
    @Schema(description = "1-全部数据")
    private Integer dataType;
}

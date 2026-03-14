package jnpf.permission.model.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

@Data
public class RolePagination extends Pagination {
    @Schema(description = "状态")
    private Integer enabledMark;
    @Schema(description = "类型（user-用户，Organize-组织，Position-岗位）")
    private String type;
    @Schema(description = "选中的key")
    private String[] selectKey;
    @Schema(description = "菜单id")
    private String moduleId;
    @Schema(description = "1-全部数据")
    private Integer dataType;
}

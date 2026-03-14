package jnpf.permission.model.position;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

@Data
@Schema(description = "岗位查询参数")
public class PositionPagination extends Pagination {
    @Schema(description = "组织id")
    private String organizeId;
    @Schema(description = "是否启用")
    private Integer enabledMark;
    @Schema(description = "是否查询默认岗位0-否，1-是，不传则不判断")
    private Integer defaultMark;
    @Schema(description = "1-全部数据")
    private Integer dataType;
}

package jnpf.onlinedev.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RelationQuery {
    @Schema(description = "菜单id")
    private String modelId;
    @Schema(description = "字段对象")
    private String columnOptions;
    @Schema(description = "高级查询条件json")
    private String superQueryJson;

    private Long pageSize;
    private Long currentPage;
    private Long total;
    private String sidx;
}

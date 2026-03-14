package jnpf.base.model.datainterface;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

import java.io.Serializable;

@Data
public class PaginationDataInterfaceSelector extends Pagination implements Serializable {
    /**
     * 1  鉴权、真分页、SQL的增加、修改、删除类型
     * 2  鉴权、SQL的增加、修改、删除类型
     * 3  鉴权、真分页、SQL的查询类型
     */
    @Schema(description = "来源类型")
    private Integer sourceType;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "分类id")
    private String category;
}

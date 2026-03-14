package jnpf.base.model.datainterface;


import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

@Data
public class PaginationDataInterface extends Pagination {
    @Schema(description = "分类id")
    private String category;
    /**
     * 请求方式
     */
    @Schema(description = "是否分页 0-不分页 1-分页")
    private Integer hasPage;
    @Schema(description = "有效标志")
    private Integer enabledMark;
    @Schema(description = "类型")
    private String type;

    /**
     * 请求方式
     */
    @Schema(description = "请求方式")
    private String dataType;
}

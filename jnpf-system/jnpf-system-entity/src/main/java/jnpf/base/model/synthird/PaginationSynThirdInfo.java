package jnpf.base.model.synthird;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

@Data
public class PaginationSynThirdInfo extends Pagination {

    @Schema(description = "搜索类型:同步失败，未同步")
    private String resultType;
    @Schema(description = "搜索类型:组织，用户")
    private String type;
    @Schema(description = "第三方类型")
    private String thirdType;
}

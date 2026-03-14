package jnpf.base.model.module;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

@Data
@Schema( description = "从菜单中获取表单列表参数")
public class ModulePagination extends Pagination {
    @Schema( description = "应用id")
    private String systemId;

    @Schema( description = "菜单类型：门户8，在线3")
    private Integer type;

    @Schema( description = "web、app")
    private String category;
}

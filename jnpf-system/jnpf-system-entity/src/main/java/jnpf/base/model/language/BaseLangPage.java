package jnpf.base.model.language;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

@Data
@Schema(description = "翻译列表查询参数")
public class BaseLangPage extends Pagination {
    @Schema(description = "类型：0-客户端，1-服务端")
    private Integer type;
}

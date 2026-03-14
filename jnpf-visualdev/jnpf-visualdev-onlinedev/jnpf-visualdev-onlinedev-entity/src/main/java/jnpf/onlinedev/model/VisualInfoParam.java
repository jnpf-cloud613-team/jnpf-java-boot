package jnpf.onlinedev.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "详情接口传参")
@Data
public class VisualInfoParam {
    @Schema(description = "数据值")
    private Object id;
    @Schema(description = "数据字段")
    private String propsValue;
    @Schema(description = "菜单id")
    private String menuId;
}

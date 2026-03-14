package jnpf.base.model.home;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.model.module.ModuleModel;
import lombok.Data;

@Data
@Schema(description = "首页模型")
public class MenuModel extends ModuleModel {
    @Schema(description = "图标背景色")
    private String iconBackground;

    @Schema(description = "应用编码")
    private String systemCode;

    @Schema(description = "应用编码")
    private String systemName;

    @Schema(description = "是否后台")
    private Integer isBackend;
}

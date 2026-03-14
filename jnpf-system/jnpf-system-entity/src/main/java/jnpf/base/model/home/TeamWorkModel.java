package jnpf.base.model.home;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "协作模型")
public class TeamWorkModel {
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "首页模型")
    private String urlAddress;
    @Schema(description = "数量")
    private Long count;
}

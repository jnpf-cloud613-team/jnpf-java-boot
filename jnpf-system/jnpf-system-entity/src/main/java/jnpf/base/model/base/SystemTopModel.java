package jnpf.base.model.base;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "应用置顶表单")
public class SystemTopModel {

    @NotNull(message = "置顶数据id必填")
    @Schema(description = "置顶数据id")
    private String id;

    @NotNull(message = "置顶类型id必填：home-首页，appCenter-应用中心")
    @Schema(description = "类型：home-首页，appCenter-应用中心")
    private String type = "appCenter";

    @NotNull(message = "动作类型必填：0-取消置顶，1-置顶")
    @Schema(description = "动作类型：0-取消，1-置顶")
    private Integer actionType;
}

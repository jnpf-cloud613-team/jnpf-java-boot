package jnpf.base.model.home;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@Schema(description = "首页模型")
@NoArgsConstructor
@AllArgsConstructor
public class HomeModel {
    @Schema(description = "模块编码")
    private String code;
    @Schema(description = "是否启用：0-禁用，1-启用")
    @Builder.Default
    private Integer enable = 0;
    @Schema(description = "数据")
    private Object data;

    //最近和收藏功能属性
    @Schema(description = "流程数据是否展示")
    @Builder.Default
    private Boolean flowEnabled = false;
    @Schema(description = "流程数据")
    private List<Object> flowList;
    @Schema(description = "应用数据")
    private List<Object> appList;

    public HomeModel(String code, Integer enable, Object data) {
        this.code = code;
        this.enable = enable;
        this.data = data;
    }
}

package jnpf.base.model.home;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@Schema(description = "图表模型")
@NoArgsConstructor
@AllArgsConstructor
public class ChartModel {
    @Schema(description = "类型名称")
    private String name;
    @Schema(description = "数据")
    private Object data;
    @Schema(description = "柱形图x轴数据")
    private List<String> category;

    public ChartModel(String name, Object data) {
        this.name = name;
        this.data = data;
    }
}

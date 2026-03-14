package jnpf.base.model.dataset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DataFormModel {

    @Schema(description = "策略")
    private Integer resultFilter;

    @Schema(description = "数目")
    private Integer resultNum;

    @Schema(description = "指定数据")
    private String specifyData;

}

package jnpf.base.model.export;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class VisualScreenExportVo {
    @Schema(description = "大屏分类")
    private List<Object> category;
    @Schema(description = "大屏基本信息")
    private List<Object> list;
}

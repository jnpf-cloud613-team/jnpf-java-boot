package jnpf.visualdata.model.visual;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.visualdata.entity.VisualCategoryEntity;
import lombok.Data;

import java.util.List;

@Data
public class VisualCateModel {
    @Schema(description = "大屏分类信息")
    private List<VisualCategoryEntity> category;
    @Schema(description = "大屏配置信息")
    private List<VisualModel> list;
}

package jnpf.onlinedev.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.UserInfo;
import jnpf.base.entity.VisualdevEntity;
import jnpf.base.model.VisualDevJsonModel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Schema(description = "在线方法接口变量")
public class VisualParamModel {
    private String menuId;
    private VisualDevJsonModel visualDevJsonModel;
    private PaginationModel pagination;
    private UserInfo userInfo;

    private VisualdevEntity visualdevEntity;
    private Map<String, Object> data;
    private List<Map<String, Object>> dataList;
    private String id;
    @Schema(description = "是否外链")
    @Builder.Default
    private Boolean isLink = false;

    @Schema(description = "子表仅修改")
    @Builder.Default
    private Boolean isUpload = false;

    @Schema(description = "子表仅修改")
    @Builder.Default
    private Boolean onlyUpdate = false;
}

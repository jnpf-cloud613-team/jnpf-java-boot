package jnpf.base.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.entity.VisualdevEntity;
import jnpf.database.model.entity.DbLinkEntity;
import jnpf.model.visualjson.FieLdsModel;
import jnpf.model.visualjson.TableModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证参数模型")
public class CheckFormModel {
    @Schema(description = "表单字段")
    private List<FieLdsModel> formFieldList;
    @Schema(description = "数据")
    private Map<String, Object> dataMap;
    @Schema(description = "连接")
    private DbLinkEntity linkEntity;
    @Schema(description = "表列表")
    private List<TableModel> tableModelList;
    @Schema(description = "表单信息")
    private VisualdevEntity visualdevEntity;
    @Schema(description = "表单id值")
    private String id;
    @Schema(description = "是否数据传递")
    @Builder.Default
    private Boolean isTransfer = false;

    @Schema(description = "是否外链")
    @Builder.Default
    private Boolean isLink = false;
}

package jnpf.base.model.dataset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jnpf.util.treeutil.SumTree;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "数据集合详情")
public class DataSetInfo implements Serializable {
    @Schema(description = "主键")
    private String id;

    @Schema(description = "关联数据类型")
    private String objectType;

    @Schema(description = "关联数据类型")
    private String objectId;

    @NotBlank
    @Schema(description = "数据集名称")
    private String fullName;

    @NotBlank
    @Schema(description = "数据库连接")
    private String dbLinkId;

    @Schema(description = "数据sql语句")
    private String dataConfigJson;

    @Schema(description = "参数json")
    private String parameterJson;

    @Schema(description = "字段json")
    private String fieldJson;

    @Schema(description = "类型：1-sql语句，2-配置式,3-数据接口")
    private Integer type;

    @Schema(description = "配置式json")
    private String visualConfigJson;

    @Schema(description = "配置式json")
    private String filterConfigJson;

    @Schema(description = "数据接口名称")
    private String treePropsName;

    @Schema(description = "数据接口id")
    private String interfaceId;

    @Schema(description = "字段信息")
    private List<SumTree<TableTreeModel>> children;
}

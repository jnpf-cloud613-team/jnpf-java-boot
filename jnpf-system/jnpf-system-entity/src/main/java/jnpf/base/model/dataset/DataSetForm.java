package jnpf.base.model.dataset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "数据集合表单")
public class DataSetForm implements Serializable {

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

    @Schema(description = "是否分页")
    private boolean noPage;

    @Schema(description = "数据id")
    private String formId;

    @Schema(description = "数据接口id")
    private String interfaceId;

    @Schema(description = "结果集筛选 1-所有数据，2-前n条数据，3-后n条数据，4-奇数条数据，5-偶数条数据，6-指定数据")
    private String resultFilter;

    @Schema(description = "用户指定数据")
    private String specifiedData;
}

package jnpf.base.model.datainterface;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class DataInterfaceCrForm {
    @Schema(description ="编码")
    @NotNull(message = "接口编码不能为空")
    private String enCode;
    @Schema(description ="接口名称")
    @NotNull(message = "接口名称不能为空")
    private String fullName;
    @Schema(description ="分类id")
    @NotNull(message = "分类id不能为空")
    private String category;
    @Schema(description ="数据类型")
    @NotNull(message = "数据类型不能为空")
    private Integer type;
    @Schema(description ="是否分页")
    private int hasPage;
    @Schema(description ="请求方式")
    private int action;
    @Schema(description ="排序")
    private Long sortCode;
    @Schema(description ="状态(0-默认，禁用，1-启用)")
    private Integer enabledMark;
    @Schema(description ="说明备注")
    private String description;
    @Schema(description ="字段JSON")
    private String fieldJson;
    @Schema(description ="参数json")
    private String parameterJson;
    @Schema(description ="后置接口")
    private Integer isPostPosition;
    @Schema(description ="数据配置json")
    private String dataConfigJson;
    @Schema(description ="数据统计json")
    private String dataCountJson;
    @Schema(description ="数据回显json")
    private String dataEchoJson;
    @Schema(description ="数据处理json")
    private String dataJsJson;
    @Schema(description ="异常验证json")
    private String dataExceptionJson;
}

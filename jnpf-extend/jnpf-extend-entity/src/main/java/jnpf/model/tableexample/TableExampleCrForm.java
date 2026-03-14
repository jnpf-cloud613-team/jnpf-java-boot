package jnpf.model.tableexample;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class TableExampleCrForm {
    @NotBlank(message = "必填")
    @Schema(description ="负责人")
    private String principal;

    @Schema(description ="交互日期")
    private long interactionDate;

    @Schema(description ="立顶人")
    private String jackStands;

    @NotBlank(message = "必填")
    @Schema(description ="项目编码")
    private String projectCode;

    @Schema(description ="项目阶段")
    private String projectPhase;

    @Schema(description ="已用金额",example = "1")
    private Long tunesAmount;

    @NotBlank(message = "必填")
    @Schema(description ="项目类型")
    private String projectType;

    @Schema(description ="费用金额",example = "1")
    private Long costAmount;

    @Schema(description ="预计收入",example = "1")
    private Long projectedIncome;

    @Schema(description ="备注")
    private String description;

    @NotBlank(message = "必填")
    @Schema(description ="项目名称")
    private String projectName;

    @Schema(description ="客户名称")
    private String customerName;

}

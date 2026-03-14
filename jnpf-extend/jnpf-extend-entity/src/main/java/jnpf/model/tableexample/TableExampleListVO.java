package jnpf.model.tableexample;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
public class TableExampleListVO {
    @Schema(description ="主键")
    private String id;

    @Schema(description ="项目名称")
    private String projectName;

    @Schema(description ="项目编码")
    private String projectCode;

    @Schema(description ="项目类型")
    private String projectType;

    @Schema(description ="项目阶段")
    private String projectPhase;

    @Schema(description ="客户名称")
    private String customerName;

    @Schema(description ="负责人")
    private String principal;

    @Schema(description ="立顶人")
    private String jackStands;

    @Schema(description ="交付日期")
    private long interactionDate;

    @Schema(description ="费用金额",example = "1")
    private Long costAmount;

    @Schema(description ="已用金额",example = "1")
    private Long tunesAmount;

    @Schema(description ="预计收入",example = "1")
    private Long projectedIncome;

    @Schema(description ="登记人")
    private String registrant;
    @Schema(description ="登记人")
    private long registerDate;

    @Schema(description ="备注")
    private String description;

    @Schema(description ="批注")
    private String postilJson;
    @Schema(description ="标记")
    private String sign;
    private String postilCount;
}

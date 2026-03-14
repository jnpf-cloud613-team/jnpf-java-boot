package jnpf.model.tableexample;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class TableExampleListDTO {

    @Schema(description ="负责人")
    private String principal;

    @Schema(description ="交互日期")
    private Date interactionDate;

    @Schema(description ="立顶人")
    private String jackStands;

    @Schema(description ="项目编码")
    private String projectCode;

    @Schema(description ="项目阶段")
    private String projectPhase;

    @Schema(description ="已用金额")
    private Long tunesAmount;

    @Schema(description ="项目类型")
    private String projectType;

    @Schema(description ="费用金额")
    private Long costAmount;

    @Schema(description ="预计收入")
    private Long projectedIncome;

    @Schema(description ="备注")
    private String description;

    @Schema(description ="项目名称")
    private String projectName;

    @Schema(description ="客户名称")
    private String customerName;




    @Schema(description ="批注总数")
    private String postilCount;

    @Schema(description ="批注列表Json")
    private String postilJson;

    @Schema(description ="编辑时间")
    private Date lastModifyTime;

    @Schema(description ="编辑用户")
    private String lastModifyUserId;



    @Schema(description ="标记")
    private String sign;

    @Schema(description ="登记人")
    private String registrant;

    @Schema(description ="登记时间")
    private Date registerDate;

    @Schema(description ="自然主键")
    private String id;

    @Schema(description ="排序码")
    private String sortCode;

    @Schema(description ="有效标志")
    private Integer enabledMark;

}

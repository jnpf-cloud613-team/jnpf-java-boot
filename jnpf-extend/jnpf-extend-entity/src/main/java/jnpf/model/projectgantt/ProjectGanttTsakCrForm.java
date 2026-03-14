package jnpf.model.projectgantt;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ProjectGanttTsakCrForm {
    @NotBlank(message = "必填")
    @Schema(description ="父级Id")
    private String parentId;
    private String projectId;

    @NotNull(message = "必填")
    @Schema(description ="完成进度")
    private Integer schedule;

    @NotBlank(message = "必填")
    @Schema(description ="项目名称")
    private String fullName;

    @NotBlank(message = "必填")
    @Schema(description ="参与人员")
    private String managerIds;

    @NotNull(message = "必填")
    @Schema(description ="开始时间")
    private long startTime;

    @NotNull(message = "必填")
    @Schema(description ="结束时间")
    private long endTime;

    @NotNull(message = "必填")
    @Schema(description ="项目工期")
    private BigDecimal timeLimit;

    @Schema(description ="项目描述")
    private String description;
    @Schema(description ="标记颜色")
    private String signColor;
    @Schema(description ="标记")
    private String sign;
}

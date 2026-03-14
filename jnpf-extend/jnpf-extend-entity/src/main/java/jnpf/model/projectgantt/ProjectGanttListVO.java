package jnpf.model.projectgantt;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ProjectGanttListVO {
    @Schema(description ="主键id")
    private String id;
    @Schema(description ="项目编码")
    private String enCode;
    @Schema(description ="项目名称")
    private String fullName;
    @Schema(description ="项目工期")
    private String timeLimit;
    @Schema(description ="开始时间")
    private long startTime;
    @Schema(description ="结束时间")
    private long endTime;
    private Integer schedule;
    @Schema(description ="参与人员")
    private String managerIds;
    @Schema(description ="状态")
    private Integer state;
    @Schema(description ="详情")
    private List<ProjectGanttManagerIModel> managersInfo;
}

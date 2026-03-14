package jnpf.model.projectgantt;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ProjectGanttTaskInfoVO {
    @Schema(description ="开始时间")
    private long startTime;
    @Schema(description ="完成进度")
    private String schedule;
    @Schema(description ="项目工期")
    private String timeLimit;
    @Schema(description ="项目名称")
    private String fullName;
    @Schema(description ="父级id")
    private String parentId;
    @Schema(description ="主键id")
    private String id;
    @Schema(description ="结束时间")
    private long endTime;
    @Schema(description ="参与人员")
    private String managerIds;
    @Schema(description ="项目描述")
    private String description;
    @Schema(description ="项目主键")
    private String projectId;
    @Schema(description ="标记颜色")
    private String signColor;
    @Schema(description ="标记")
    private String sign;
}

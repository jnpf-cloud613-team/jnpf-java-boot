package jnpf.model.projectgantt;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ProjectGanttTreeVO {
    @Schema(description ="主键")
    private String id;
    @Schema(description ="父级主键")
    private String parentId;
    @Schema(description ="名称")
    private String fullName;
    @Schema(description ="开始时间")
    private String startTime;
    @Schema(description ="结束时间")
    private String endTime;
    @Schema(description ="标记")
    private String sign;
    @Schema(description ="标记颜色")
    private String signColor;
    @Schema(description ="完成进度")
    private String schedule;
    @Schema(description ="是否有子集")
    private Boolean hasChildren;
    @Schema(description ="开始时间")
    private List<ProjectGanttTreeVO> children;
}

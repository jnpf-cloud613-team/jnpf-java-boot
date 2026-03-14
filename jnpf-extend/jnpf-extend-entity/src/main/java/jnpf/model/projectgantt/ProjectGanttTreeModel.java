package jnpf.model.projectgantt;

import jnpf.util.treeutil.SumTree;
import lombok.Data;

@Data
public class ProjectGanttTreeModel extends SumTree<ProjectGanttTreeModel> {

    private Integer schedule;

    private String fullName;

    private long startTime;

    private long endTime;

    private String signColor;
    private String sign;
}

package jnpf.permission.model.position;

import jnpf.util.treeutil.SumTree;
import lombok.Data;

@Data
public class PositionTreeModel extends SumTree<PositionTreeModel> {
    private String id;
    private String fullName;
    private String enCode;
    private String organizeId;
    private String icon;
    private Integer defaultMark;
    private Integer isDutyPosition;
    private Integer allowDuty;
    private String orgNameTree;
}

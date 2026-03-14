package jnpf.permission.model.organize;

import jnpf.util.treeutil.SumTree;
import lombok.Data;

import java.util.List;


@Data
public class OrganizeModel extends SumTree<OrganizeModel> {
    private String fullName;
    private String enCode;
    private Long creatorTime;
    private String manager;
    private String description;
    private int enabledMark;
    private String icon;
    private String category;
    private long sortCode;
    private String organizeIdTree;
    private String orgNameTree;
    private String organize;
    private List<String> organizeIds;
    private String lastFullName;
    private String deptId;
    private String parentId;
}

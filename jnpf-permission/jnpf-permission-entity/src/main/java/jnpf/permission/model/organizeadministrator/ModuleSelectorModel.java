package jnpf.permission.model.organizeadministrator;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.util.treeutil.SumTree;
import lombok.Data;

import java.io.Serializable;

@Data
public class ModuleSelectorModel extends SumTree<ModuleSelectorModel> implements Serializable {
    private String id;
    private String fullName;
    private String enCode;
    private String parentId;
    private String icon;
    private Integer type;
    private Long sortCode;
    private String category;
    private String propertyJson;

    private String systemId;
    private Boolean hasModule;

    @Schema(description = "是否有权限")
    private Integer isPermission;

    private boolean disabled;

    private long creatorTime;
}

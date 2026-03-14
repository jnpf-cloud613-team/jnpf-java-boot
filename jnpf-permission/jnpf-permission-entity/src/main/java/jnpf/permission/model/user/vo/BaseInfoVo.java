package jnpf.permission.model.user.vo;

import jnpf.permission.model.user.UserIdListVo;
import lombok.Data;

@Data
public class BaseInfoVo extends UserIdListVo {
    private String orgNameTree;
    private String organizeIdTree;
    private String parentId;
    private String parentName;
}

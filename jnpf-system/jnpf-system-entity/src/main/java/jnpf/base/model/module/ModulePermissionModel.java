package jnpf.base.model.module;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ModulePermissionModel implements Serializable {

    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "是否有权限 0-未开启 1-有权限 2-无权限")
    private int type;
    private List<ModulePermissionBaseModel> list;

    @Data
    public static class ModulePermissionBaseModel {
        private String id;
        private String fullName;
    }
}

package jnpf.permission.model.permissiongroup;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

import java.io.Serializable;

@Data
public class PaginationPermissionGroup extends Pagination implements Serializable {
    @Schema(description = "状态")
    private Integer enabledMark;
}

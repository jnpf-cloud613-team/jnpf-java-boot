package jnpf.permission.model.rolerelaiton;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/6 9:32:32
 */
@Data
@Schema(description = "角色关联参数")
public class RoleListPage extends Pagination {
    @Schema(description = "组织id")
    private String positionId;
    @Schema(description = "岗位id")
    private String organizeId;
}

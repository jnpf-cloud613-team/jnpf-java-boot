package jnpf.permission.model.rolerelaiton;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.base.Pagination;
import lombok.Data;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/6 9:32:32
 */
@Data
@Schema(description = "角色关联参数")
public class RoleRelationPage extends Pagination {
    @Schema(description = "角色id")
    private String roleId;
    @Schema(description = "类型：user,organize,position")
    private String type;
    @Schema(description = "id列表：用户id，组织id，岗位id")
    private List<String> idList;
}

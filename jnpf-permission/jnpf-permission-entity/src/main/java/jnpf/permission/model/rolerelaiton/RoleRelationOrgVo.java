package jnpf.permission.model.rolerelaiton;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色关联列表vo：组织和岗位共用
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/6 9:19:26
 */
@Data
@Schema(description = "角色关联列表vo：组织和岗位共用")
public class RoleRelationOrgVo {

    @Schema(description = "组织或岗位id")
    private String id;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "全名")
    private String orgNameTree;

    @Schema(description = "组织id")
    private String organizeId;
}

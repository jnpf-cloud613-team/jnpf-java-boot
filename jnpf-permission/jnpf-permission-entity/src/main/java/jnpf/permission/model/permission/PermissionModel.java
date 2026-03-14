package jnpf.permission.model.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.util.treeutil.SumTree;
import lombok.Data;


/**
 * 个人信息设置 我的组织/我的岗位/（我的角色：暂无）
 *
 * @author JNPF开发平台组 YanYu
 * @version V3.2.0
 * @copyright 引迈信息技术有限公司
 * @date 2022/1/25
 */
@Data
public class PermissionModel extends SumTree<PermissionModel> {

    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "id")
    private String id;
    @Schema(description = "是否为默认")
    private Boolean isDefault;

}

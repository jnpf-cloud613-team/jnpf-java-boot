package jnpf.permission.model.position;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class PosistionCurrentModel {
    @Schema(description = "id")
    private String id;
    @Schema(description = "所属组织id")
    private String organizeId;
    @Schema(description = "所属组织名称")
    private String orgTreeName;
    @Schema(description = "岗位id")
    private String positionId;
    @Schema(description = "岗位名称")
    private String fullName;
    @Schema(description = "是否为默认")
    private Boolean isDefault;

    @Schema(description = "上级岗位id")
    private String parentId;
    @Schema(description = "上级岗位名称")
    private String parentName;
    @Schema(description = "上级责任人")
    private String managerName;
    @Schema(description = "上级责任人id")
    private String managerId;

}

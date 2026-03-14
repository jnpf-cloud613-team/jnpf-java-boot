package jnpf.permission.model.authorize;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class SaveBatchForm {
    @Schema(description = "角色id集合")
    private String[] roleIds;
    @Schema(description = "岗位id集合")
    private String[] positionIds;
    @Schema(description = "用户id集合")
    private String[] userIds;
    @Schema(description = "组织id集合")
    private String[] orgIds;

    @Schema(description = "菜单id集合")
    private String[] module;
    @Schema(description = "按钮id集合")
    private String[] button;
    @Schema(description = "列表id集合")
    private String[] column;
    @Schema(description = "数据权限方案id集合")
    private String[] resource;
    @Schema(description = "表单id集合")
    private String[] form;

    @Schema(description = "系统id集合")
    private String[] systemIds;
}

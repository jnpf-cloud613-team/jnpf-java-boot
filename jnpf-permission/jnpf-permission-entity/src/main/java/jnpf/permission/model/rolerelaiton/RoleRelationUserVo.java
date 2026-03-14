package jnpf.permission.model.rolerelaiton;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色关联列表vo：用户单用
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/6 9:19:26
 */
@Data
@Schema(description = "角色关联列表vo：用户单用")
public class RoleRelationUserVo {
    @Schema(description = "用户id")
    private String id;

    @Schema(description = "账号")
    private String account;

    @Schema(description = "名称")
    private String realName;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "手机号")
    private String mobilePhone;

    @Schema(description = "岗位")
    private String position;

    @Schema(description = "类型")
    private Integer enabledMark;
}

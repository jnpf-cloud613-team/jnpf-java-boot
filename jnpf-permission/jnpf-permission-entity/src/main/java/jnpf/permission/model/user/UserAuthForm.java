package jnpf.permission.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 个人权限参数
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/4/25 17:35:36
 */
@Data
@Schema(description = "个人权限参数")
public class UserAuthForm {
    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "角色/岗位id")
    private String objectId;

    @Schema(description = "类型：角色/岗位")
    private String objectType;
}

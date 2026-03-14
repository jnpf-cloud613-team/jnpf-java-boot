package jnpf.permission.model.user.mod;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/4/14 15:45
 */
@Data
public class UserByRoleModel implements Serializable {
    /**
     * 关键字
     */
    @Schema(description = "关键字")
    private String keyword;

    /**
     * 组织id
     */
    @Schema(description = "组织id")
    private String organizeId;

    /**
     * 角色id
     */
    @Schema(description = "角色id")
    private String roleId;

}

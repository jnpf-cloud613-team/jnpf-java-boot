package jnpf.permission.model.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
@Builder
public class UserSubordinateVO {
    private String id;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "用户名")
    private String userName;
    @Schema(description = "部门")
    private String department;
    @Schema(description = "岗位")
    private String position;

    @Schema(description = "是否显示下级按钮")
    private Boolean isLeaf;
}

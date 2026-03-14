package jnpf.permission.model.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class RoleListVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "角色类型：user,Organize,Position")
    private String type;
    @Schema(description = "状态")
    private Integer enabledMark;
    @Schema(description = "是否系统：1是")
    private Integer isSystem;

    @Schema(description = "排序")
    private Long sortCode;
    @Schema(description = "备注")
    private String description;
    @Schema(description = "状态")
    private Long creatorTime;
}

package jnpf.model;



import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021-07-08
 */
@Data
@Schema(description = "常用模型")
public class AppUsersVO {
    @Schema(description = "用户id")
    private String userId;
    @Schema(description = "用户账号")
    private String userAccount;
    @Schema(description = "用户姓名")
    private String userName;
    @Schema(description = "用户头像")
    private String headIcon;
    @Schema(description = "组织主键")
    private String organizeId;
    @Schema(description = "组织名称")
    private String organizeName;
    @Schema(description = "角色主键")
    private String roleId;
    @Schema(description = "角色名称")
    private String roleName;
    @Schema(description = "性别")
    private String gender;
    @Schema(description = "岗位")
    private List<AppPositionVO> positionIds;
    @Schema(description = "生日")
    private Long birthday;
    @Schema(description = "手机")
    private String mobilePhone;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "直属主管")
    private String manager;

}

package jnpf.permission.model.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "账号")
    private String account;
    @Schema(description = "姓名")
    private String realName;
    @Schema(description = "姓名/账号")
    private String fullName;
    @Schema(description = "手机")
    private String mobilePhone;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "部门")
    private String organize;
    @Schema(description = "头像")
    private String headIcon;
    @Schema(description = "所属岗位")
    private String position;
}

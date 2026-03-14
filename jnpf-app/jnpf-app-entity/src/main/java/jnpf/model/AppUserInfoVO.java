package jnpf.model;


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
@Schema(description = "常用模型")
public class AppUserInfoVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "户名")
    private String realName;
    @Schema(description = "部门名称")
    private String organizeName;
    @Schema(description = "账号")
    private String account;
    @Schema(description = "岗位名称")
    private String positionName;
    @Schema(description = "办公电话")
    private String telePhone;
    @Schema(description = "办公座机")
    private String landline;
    @Schema(description = "手机号码")
    private String mobilePhone;
    @Schema(description = "用户头像")
    private String headIcon;
    @Schema(description = "邮箱")
    private String email;
}

package jnpf.permission.model.organizeadministrator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 组织关系表模型
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/5/30 9:23
 */
@Data
public class OrganizeAdministratorListVo implements Serializable {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "账号")
    private String account;
    @Schema(description = "真实姓名")
    private String realName;
    @Schema(description = "性别")
    private String gender;
    @Schema(description = "手机号")
    private String mobilePhone;
    @Schema(description = "组织id")
    private String organizeId;
    @Schema(description = "创建时间")
    private Long creatorTime;
    @Schema(description = "管理组")
    private String managerGroup;

}

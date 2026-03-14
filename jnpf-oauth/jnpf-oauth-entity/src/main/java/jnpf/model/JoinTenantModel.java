package jnpf.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class JoinTenantModel {

    @Schema(description = "企业名称")
    private String companyName;

    @Schema(description = "联系电话")
    private String mobile;

    @Schema(description = "手机验证码")
    private String mobileCode;

    @Schema(description = "联系人")
    private String userName;

    @Schema(description = "应用编码")
    private String appCode;
}

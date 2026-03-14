package jnpf.message.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class UserOnlineVO {
    private String userId;
    private String userName;
    private String loginTime;
    private String loginIPAddress;
    private String loginSystem;
    @Schema(description = "所属组织")
    private String organize;
    @Schema(description = "浏览器")
    private String loginBrowser;
    @Schema(description = "登录地址")
    private String loginAddress;
}

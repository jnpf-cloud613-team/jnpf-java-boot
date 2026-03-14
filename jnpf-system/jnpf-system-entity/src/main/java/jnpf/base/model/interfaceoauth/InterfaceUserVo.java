package jnpf.base.model.interfaceoauth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 授权用户展示
 *
 * @author JNPF开发平台组
 * @version V3.4.7
 * @copyright 引迈信息技术有限公司
 * @date 2021/9/20 9:22
 */
@Data
public class InterfaceUserVo {

    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "用户密钥")
    private String userKey;
}

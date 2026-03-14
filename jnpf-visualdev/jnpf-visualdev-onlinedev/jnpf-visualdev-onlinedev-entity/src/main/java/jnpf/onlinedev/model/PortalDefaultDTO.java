package jnpf.onlinedev.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 类功能
 *
 * @author JNPF开发平台组 YanYu
 * @version v3.4.6
 * @copyrignt 引迈信息技术有限公司
 * @date 2023-03-24
 */
@Data
public class PortalDefaultDTO {

    @Schema(description = "默认门户ID")
    private String defaultPortalId;

    @Schema(description = "系统ID")
    private String systemId;
}

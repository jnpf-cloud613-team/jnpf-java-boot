package jnpf.base.model.interfaceoauth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 授权接口列表
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/6/9 18:12
 */
@Data
public class IdentInterfaceListModel {

    @Schema(description = "接口认证id")
    private String interfaceIdentId;

    @Schema(description = "接口id")
    private String dataInterfaceIds;
}

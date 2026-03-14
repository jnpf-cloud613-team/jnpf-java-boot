package jnpf.base.model.interfaceoauth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 授权用户表单
 *
 * @author JNPF开发平台组
 * @version V3.4.7
 * @copyright 引迈信息技术有限公司
 * @date 2021/9/20 9:22
 */
@Data
public class InterfaceUserForm {

    @Schema(description = "接口认证id")
    private String interfaceIdentId;

    @Schema(description = "授权用户列表")
    private List<String> userIds;
}

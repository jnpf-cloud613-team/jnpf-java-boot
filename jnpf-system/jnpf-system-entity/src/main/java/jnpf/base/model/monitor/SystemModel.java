package jnpf.base.model.monitor;

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
public class SystemModel {
    @Schema(description = "系统")
    private String os;
    @Schema(description = "服务器IP")
    private String ip;
    @Schema(description = "运行时间")
    private String day;
}

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
public class MemoryModel {
    @Schema(description = "总内存")
    private String total;
    @Schema(description = "空闲内存")
    private String available;
    @Schema(description = "已使用")
    private String used;
    @Schema(description = "已使用百分比")
    private String usageRate;
}

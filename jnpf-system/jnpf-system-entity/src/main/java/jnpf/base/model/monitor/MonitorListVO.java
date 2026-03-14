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
public class MonitorListVO {
    @Schema(description = "系统信息")
    private SystemModel system;
    @Schema(description = "CPU信息")
    private CpuModel cpu;
    @Schema(description = "内存信息")
    private MemoryModel memory;
    @Schema(description = "硬盘信息")
    private DiskModel disk;
    @Schema(description = "当前时间")
    private Long time;
}

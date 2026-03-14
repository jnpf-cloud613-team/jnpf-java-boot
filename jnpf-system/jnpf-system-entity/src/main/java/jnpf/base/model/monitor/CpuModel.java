package jnpf.base.model.monitor;

import com.alibaba.fastjson.annotation.JSONField;
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
public class CpuModel {
    @Schema(description = "cpu名称")
    private String name;
    @Schema(description = "物理CPU个数")
    @JSONField(name="package")
    private String packageName;
    @Schema(description = "CPU内核个数")
    private String core;
    @Schema(description = "内核个数")
    private int coreNumber;
    @Schema(description = "逻辑CPU个数")
    private String logic;
    @Schema(description = "CPU已用百分比")
    private String used;
    @Schema(description = "未用百分比")
    private String idle;
}

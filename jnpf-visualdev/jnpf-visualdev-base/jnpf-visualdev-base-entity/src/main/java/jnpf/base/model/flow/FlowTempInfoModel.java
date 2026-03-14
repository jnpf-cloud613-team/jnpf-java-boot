package jnpf.base.model.flow;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/15 9:17
 */
@Data
@Schema(description="流程引擎信息模型")
public class FlowTempInfoModel {
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "流程引擎id")
    private String id;
    @Schema(description = "是否启用")
    private Integer enabledMark;
}

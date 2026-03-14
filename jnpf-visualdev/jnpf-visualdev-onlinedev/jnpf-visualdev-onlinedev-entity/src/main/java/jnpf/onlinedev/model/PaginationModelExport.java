package jnpf.onlinedev.model;



import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @author JNPF开发平台组
 * @date 2021/3/16
 */
@Data
@Schema(description="导出参数")
public class PaginationModelExport extends PaginationModel {
    @Schema(description = "导出selectKey")
    private String[] selectKey;
    @Schema(description = "导出选中数据")
    private Object[] selectIds;
    @Schema(description = "导出json")
    private String json;
}

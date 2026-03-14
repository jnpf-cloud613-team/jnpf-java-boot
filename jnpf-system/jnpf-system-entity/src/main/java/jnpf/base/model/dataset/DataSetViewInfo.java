package jnpf.base.model.dataset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 数据集预览对象
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/9/10 10:52:52
 */
@Data
@Schema(description = "数据集预览对象")
public class DataSetViewInfo {
    @Schema(description = "字段列表")
    private List<Map<String,String>> previewColumns;

    @Schema(description = "数据列表")
    private List<Map<String, Object>> previewData;

    @Schema(description = "数据集预览对象")
    private String previewSqlText;
}

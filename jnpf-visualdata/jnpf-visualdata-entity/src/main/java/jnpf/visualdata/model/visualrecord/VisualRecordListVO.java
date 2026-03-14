package jnpf.visualdata.model.visualrecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年7月7日
 */
@Data
public class VisualRecordListVO {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "数据集类型")
    private Integer dataType;

}

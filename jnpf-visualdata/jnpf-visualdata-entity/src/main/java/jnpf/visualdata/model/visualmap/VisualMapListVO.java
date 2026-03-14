package jnpf.visualdata.model.visualmap;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
@Data
public class VisualMapListVO {
    @Schema(description ="地图名称")
    private String name;
    @Schema(description ="主键")
    private String id;

    @Schema(description ="地图编码")
    private String code;

    @Schema(description ="地图级别")
    private Integer level;

    @Schema(description ="上级名称")
    private String parentName;

    @Schema(description ="是否存在下级")
    private Boolean hasChildren;
}

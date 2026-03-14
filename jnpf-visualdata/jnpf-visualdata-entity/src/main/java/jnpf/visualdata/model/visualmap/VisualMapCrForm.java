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
public class VisualMapCrForm {
    @Schema(description ="地图名称")
    private String name;
    @Schema(description ="地图数据")
    private String data;

    @Schema(description ="地图编码")
    private String code;

    @Schema(description ="地图级别")
    private Integer level;

    @Schema(description ="上级ID")
    private String parentId;

    @Schema(description ="上级编码")
    private String parentCode;

}

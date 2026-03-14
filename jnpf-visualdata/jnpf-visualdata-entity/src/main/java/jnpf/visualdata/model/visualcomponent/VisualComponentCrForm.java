package jnpf.visualdata.model.visualcomponent;
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
public class VisualComponentCrForm {


    @Schema(description = "主键")
    private String id;

    @Schema(description = "主键")
    private String name;

    @Schema(description = "组件内容")
    private String content;

    @Schema(description = "组件类型")
    private Integer type;

    @Schema(description = "组件图片")
    private String img;
}

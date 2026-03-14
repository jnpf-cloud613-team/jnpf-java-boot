package jnpf.visualdata.model.visual;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.visualdata.model.VisualPagination;
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
public class VisualPaginationModel extends VisualPagination {
    @Schema(description ="分类")
    private Integer category;
    @Schema(description ="名称")
    private String title;
    @Schema(description ="名称")
    private String categoryValue;
    @Schema(description ="名称")
    private String name;
    @Schema(description ="名称")
    private String globalName;
    @Schema(description ="名称")
    private String assetsName;
    @Schema(description = "组件类型(0,1)")
    private Integer type;
    @Schema(description ="上级编码")
    private String parentId;

}

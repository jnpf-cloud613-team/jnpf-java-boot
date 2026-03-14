package jnpf.base.model.province;

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
public class ProvinceInfoVO {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "有效标志")
    private Integer enabledMark;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "分类")
    private String type;

    @Schema(description = "上级id")
    private String parentId;
    @Schema(description = "上级名称")
    private String parentName;
    @Schema(description = "排序码")
    private long sortCode;
}

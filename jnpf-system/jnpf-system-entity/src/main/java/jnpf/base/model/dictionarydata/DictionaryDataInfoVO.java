package jnpf.base.model.dictionarydata;

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
public class DictionaryDataInfoVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "父级主键")
    private String parentId;
    @Schema(description = "说明")
    private String description;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "有效标志")
    private Integer enabledMark;
    @Schema(description = "分类id")
    private String dictionaryTypeId;
    @Schema(description = "排序码")
    private long sortCode;
}

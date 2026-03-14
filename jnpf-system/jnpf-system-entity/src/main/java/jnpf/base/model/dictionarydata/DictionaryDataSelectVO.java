package jnpf.base.model.dictionarydata;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DictionaryDataSelectVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "父级主键")
    private String parentId;
    @Schema(description = "是否有子集")
    private Boolean hasChildren;
    @Schema(description = "子集集合")
    private List<DictionaryDataSelectVO> children;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "分类id")
    private String dictionaryTypeId;
}

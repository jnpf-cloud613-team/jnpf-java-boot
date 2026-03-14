package jnpf.base.model.dictionarytype;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class DictionaryTypeSelectVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "父级主键")
    private String parentId;
    @Schema(description = "是否有子集")
    private Boolean hasChildren;
    @Schema(description = "子集集合")
    private List<DictionaryTypeSelectVO> children = new ArrayList<>();
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
}

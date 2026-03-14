package jnpf.permission.model.organize;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class OrganizeSelectorVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "父主键")
    private String parentId;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "组织类型")
    private String category;
    @Schema(description = "图标")
    private String icon;

    @Schema(description = "是否有下级菜单")
    private Boolean hasChildren;
    @Schema(description = "")
    private Boolean isLeaf;

    @Schema(description = "组织id树名称")
    private String organizeIdTree;
    @Schema(description = "父主全名")
    private String parentName;
    @Schema(description = "机构路径全名")
    private String orgNameTree;

    @Schema(description = "组织id树")
    private List<String> organizeIds;
}

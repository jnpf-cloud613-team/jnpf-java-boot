package jnpf.permission.model.organize;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.permission.model.permission.PermissionVoBase;
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
public class OrganizeInfoVO extends PermissionVoBase {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "分类")
    private String category;

    @Schema(description = "父级主键")
    private String parentId;
    @Schema(description = "父级名称")
    private String parentName;
    @Schema(description = "父级分类")
    private String parentCategory;

    @Schema(description = "状态")
    private Integer enabledMark;
    @Schema(description = "备注")
    private String description;
    @Schema(description = "扩展属性")
    private String propertyJson;
    @Schema(description = "排序")
    private Long sortCode;

    @Schema(description = "组织id树")
    private List<String> organizeIdTree;
}

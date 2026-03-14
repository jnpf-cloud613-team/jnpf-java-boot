package jnpf.permission.model.organize;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class OrganizePositonVO {
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
    @Schema(description = "组织id")
    private String organizeId;
    @Schema(description = "组织全名")
    private String organize;
    @Schema(description = "机构路径全名")
    private String orgNameTree;

    @Schema(description = "类型:组织或者岗位")
    private String type;
    @Schema(description = "是否责任岗位(0-否，1-是)")
    private Integer isDutyPosition = 0;
}

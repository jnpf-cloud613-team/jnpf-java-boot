package jnpf.permission.model.organize;

import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:15
 */
@Data
public class OrganizeListVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "父主键")
    private String parentId;
    @Schema(description = "父主全名")
    private String parentName;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "编码")
    private String enCode;
    @Schema(description = "备注")
    private String description;
    @Schema(description = "状态")
    private Integer enabledMark;
    private Long creatorTime;
    @Schema(description = "是否有下级菜单")
    private Boolean hasChildren;
    @Schema(description = "下级菜单列表")
    private List<OrganizeListVO> children;
    @Schema(description = "排序")
    private Long sortCode;
    private String organizeIdTree;
    @Schema(description = "")
    private Boolean isLeaf;
    @JSONField(name = "category")
    private String category;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "修改用户")
    private String lastFullName;

    @Schema(description = "机构路径全名")
    private String orgNameTree;

    @Schema(description = "组织id树")
    private List<String> organizeIds;
}

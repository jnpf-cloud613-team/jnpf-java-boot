package jnpf.permission.model.role;

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
public class RoleSelectorVO {

    @Schema(description = "ID")
    private String id;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "类型")
    private String type;
    @Schema(description = "数量")
    private Long num;
    @Schema(description = "前端解析唯一标识")
    private String onlyId;
    @Schema(description = "父节点ID")
    private String parentId;
    @Schema(description = "子类对象集合")
    private List<RoleSelectorVO> children;
    @Schema(description = "是否含有子类对象集合")
    private Boolean hasChildren;
    @Schema(description = "")
    private Boolean isLeaf;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "是否系统")
    private Integer globalMark;


    private String organize;
    @Schema(description = "组织id树")
    private List<String> organizeIds;
}

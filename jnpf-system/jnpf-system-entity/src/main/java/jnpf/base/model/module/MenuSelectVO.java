package jnpf.base.model.module;

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
public class MenuSelectVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "父主键")
    private String parentId;
    @Schema(description = "名称")
    private String fullName;
    @Schema(description = "是否按钮权限")
    private Integer isButtonAuthorize;
    @Schema(description = "是否列表权限")
    private Integer isColumnAuthorize;
    @Schema(description = "是否数据权限")
    private Integer isDataAuthorize;
    @Schema(description = "排序码")
    private Long sortCode;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "是否有下级菜单")
    private Boolean hasChildren;
    @Schema(description = "下级菜单列表")
    private List<MenuSelectVO> children;

    @Schema(description = "外链")
    private String linkTarget;
    @Schema(description = "编码")
    private String enCode;


    @Schema(description = "系统id")
    private String systemId;

    @Schema(description = "分类")
    private Integer type;
}

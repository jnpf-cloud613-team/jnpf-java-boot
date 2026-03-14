package jnpf.base.model.module;

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
public class MenuListVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "是否有下级菜单")
    private Boolean hasChildren;
    @Schema(description = "上级ID")
    private String parentId;
    @Schema(description = "状态")
    private Integer enabledMark;
    @Schema(description = "菜单名称")
    private String fullName;
    @Schema(description = " 图标")
    private String icon;
    @Schema(description = "路由地址")
    private String urlAddress;
    @Schema(description = "页面地址")
    private String pageAddress;
    @Schema(description = "菜单类型", example = "1")
    private Integer type;
    @Schema(description = "表单类型type=3时有表单类型：2-列表，4-视图")
    private Integer webType;
    @Schema(description = "是否开启权限：tpye为3即功能表单时判断")
    private String propertyJson;
    @Schema(description = "是否开启权限：tpye为3即功能表单时判断")
    private Integer hasPermission;
    @Schema(description = "下级菜单列表")
    private List<MenuListVO> children;
    private Integer isButtonAuthorize;
    private Integer isColumnAuthorize;
    private Integer isDataAuthorize;
    private Integer isFormAuthorize;
    private Long sortCode;

    private String systemId;
    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "是否隐藏：0-否，1-是隐藏")
    private Integer noShow;
}

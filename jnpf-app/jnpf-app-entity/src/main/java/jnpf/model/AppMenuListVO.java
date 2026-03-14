package jnpf.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * app应用
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021-07-08
 */
@Data
@Schema(description = "常用模型")
public class AppMenuListVO {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "是否有下级菜单")
    private Boolean hasChildren;
    @Schema(description = "父级id")
    private String parentId;
    @Schema(description = "菜单编码")
    private String enCode;
    @Schema(description = "菜单名称")
    private String fullName;
    @Schema(description = " 图标")
    private String icon;
    @Schema(description = "是否常用")
    private Boolean isData;
    @Schema(description = "链接地址")
    private String urlAddress;
    @Schema(description = "页面地址")
    private String pageAddress;
    @Schema(description = "菜单类型",example = "1")
    private Integer type;
    @Schema(description = "扩展字段")
    private String propertyJson;
    @Schema(description = "下级菜单列表")
    private List<AppMenuListVO> children;
}

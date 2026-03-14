package jnpf.base.model.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/6/21 15:58
 */
@Data
public class SystemVO implements Serializable {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "门户数组")
    private String portalId;

    @Schema(description = "APP门户数组")
    private String appPortalId;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序码")
    private Long sortCode;

    @Schema(description = "有效标志")
    private Integer enabledMark;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "扩展属性")
    private String propertyJson;

    @Schema(description = "偏好配置")
    private String preferenceJson;

    @Schema(description = "图标背景色")
    private String backgroundColor;

}

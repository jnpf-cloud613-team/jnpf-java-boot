package jnpf.permission.model.socails;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 第三方信息
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/7/14 11:00:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "第三方信息")
public class SocialsUserVo {
    @Schema(description = "类型")
    private String enname;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "描述")
    private String describetion;
    @Schema(description = "版本")
    private String since;
    @Schema(description = "logo")
    private String logo;
    @Schema(description = "官网api文档")
    private String apiDoc;
    @Schema(description = "是否首页展示")
    private boolean isLatest;
    @Schema(description = "图标")
    private String icon;
    @Schema(description = "绑定对象")
    private SocialsUserModel entity;
    @Schema(description = "获取登录地址")
    private String renderUrl;
}

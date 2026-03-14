package jnpf.base.model.shortlink;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 在线表单外链显示类
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/12/30 11:25:16
 */
@Data
@Schema(description="外链提交表单")
public class VisualdevShortLinkForm {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "短链接key")
    private String shortLink;
    @Schema(description = "表单开关0-关闭,1-开启")
    private Integer formUse;
    @Schema(description = "表单外链")
    private String formLink;
    @Schema(description = "表单密码开关0-关闭,1-开启")
    private Integer formPassUse;
    @Schema(description = "表单密码")
    private String formPassword;
    @Schema(description = "列表开关0-关闭,1-开启")
    private Integer columnUse;
    @Schema(description = "列表外链")
    private String columnLink;
    @Schema(description = "列表密码开关0-关闭,1-开启")
    private Integer columnPassUse;
    @Schema(description = "列表密码")
    private String columnPassword;
    @Schema(description = "列表查询自选")
    private String columnCondition;
    @Schema(description = "列表展示字段")
    private String columnText;
    @Schema(description = "是否启用")
    private Integer enabledMark;
}

package jnpf.base.model.shortlink;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 外链请求参数
 *
 * @author JNPF开发平台组
 * @version V3.4.2
 * @copyright 引迈信息技术有限公司
 * @date 2022/12/30 16:28:23
 */
@Data
@Schema(description="外链入口参数")
public class VisualdevShortLinkModel {
    @Schema(description = "类型：form-表单,list-列表")
    private String type;
    @Schema(description = "租户id")
    private String tenantId;
}

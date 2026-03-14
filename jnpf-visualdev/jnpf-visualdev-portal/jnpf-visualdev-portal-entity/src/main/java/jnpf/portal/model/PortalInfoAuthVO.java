package jnpf.portal.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @author JNPF开发平台组
 * @date 2021/3/16
 */
@Data
@Schema(description="门户信息")
public class PortalInfoAuthVO {
    @Schema(description = "表单josn")
    private String formData;
    @Schema(description = "门户类型0-用户配置1-自定义外链")
    private Integer type;
    @Schema(description = "链接路径")
    private String customUrl;
    @Schema(description = "链接路径")
    private String appCustomUrl;
    @Schema(description = "链接类型")
    private Integer linkType;
    @Schema(description = "锁定开关0-未锁定1锁定")
    private Integer enabledLock;
}

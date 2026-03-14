package jnpf.permission.model.authorize;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/12 15:31
 */
@Data
public class ResourceVO {
    @Schema(description = "资源主键")
    private String id;
    @Schema(description = "资源名称")
    private String fullName;
    @Schema(description = "资源编码")
    private String enCode;
    @Schema(description = "条件规则")
    private String conditionJson;
    @Schema(description = "规则描述")
    private String conditionText;
    @Schema(description = "功能主键")
    private String moduleId;
}

package jnpf.base.model.share;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 跨应用单Vo
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025-07-30
 */
@Data
@Schema(description = "跨应用单详情")
public class SystemShareVo implements Serializable {
    @Schema(description = "id")
    private String id;

    @Schema(description = "来源系统id")
    private String sourceId;

    @Schema(description = "对象主键")
    private String objectId;

    @Schema(description = "来源系统名称")
    private String sourceName;

    @Schema(description = "对象名称（功能名称）")
    private String objectName;

    @Schema(description = "回显名称")
    private String showName;
}

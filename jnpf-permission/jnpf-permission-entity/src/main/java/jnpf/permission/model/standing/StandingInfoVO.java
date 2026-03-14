package jnpf.permission.model.standing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 身份详情
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/3/4 18:39:14
 */
@Data
public class StandingInfoVO implements Serializable {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "名称")
    private String fullName;

    @Schema(description = "编码")
    private String enCode;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "排序")
    private String sortCode;
}

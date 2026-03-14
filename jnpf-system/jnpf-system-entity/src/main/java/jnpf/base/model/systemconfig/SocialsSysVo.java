package jnpf.base.model.systemconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.model.SocialsSysConfig;
import lombok.Data;

/**
 * 同步配置展示字段
 *
 * @author JNPF开发平台组
 * @version v6.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/4/11 11:43:58
 */
@Data
@Schema(description = "同步配置展示字段")
public class SocialsSysVo extends SocialsSysConfig {
    @Schema(description = "微信组织选择禁用（同步过的就禁用）")
    private Boolean qyhDisabled = false;
    @Schema(description = "钉钉组织选择禁用（同步过的就禁用）")
    private Boolean dingDisabled = false;
}

package jnpf.base.model.online;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-12-15
 */
@Data
public class BatchOnlineModel implements Serializable {
    @Schema(description = "id集合")
    private String[] ids;
}

package jnpf.message.model.messagemonitor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021/3/16 10:10
 */
@Data
public class MsgDelForm  {
   @Schema(description = "id集合")
   private String[] ids;
}


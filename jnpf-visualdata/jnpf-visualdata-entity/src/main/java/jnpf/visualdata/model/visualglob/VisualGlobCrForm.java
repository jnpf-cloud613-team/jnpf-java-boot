package jnpf.visualdata.model.visualglob;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.5.0
 * @copyright 引迈信息技术有限公司
 * @date 2023年7月7日
 */
@Data
public class VisualGlobCrForm {


    @Schema(description = "主键")
    private String id;

    @Schema(description = "变量名称")
    private String globalName;

    @Schema(description = "变量Key")
    private String globalKey;

    @Schema(description = "组变量值")
    private String globalValue;
}

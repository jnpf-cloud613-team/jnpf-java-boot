package jnpf.base.model.language;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/6/20 10:06:37
 */
@Data
@Schema(description = "多语言对象")
public class BaseLangVo {

    @Schema(description = "翻译标记")
    private String enCode;

    @Schema(description = "翻译内容")
    private String fullName;

}

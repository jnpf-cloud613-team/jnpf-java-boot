package jnpf.base.model.language;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author JNPF开发平台组
 * @version v5.0.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/6/25 15:30:40
 */
@Data
@Schema(description = "多语言参数模型")
public class BaseLangModel {

    @Schema(description = "文件名称")
    private String fileName;
}

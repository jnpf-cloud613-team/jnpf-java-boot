package jnpf.base.model.ocr;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ocr接口参数
 *
 * @author JNPF开发平台组
 * @version v6.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2025/7/29 16:58:47
 */
@Data
@Schema(description = "ocr接口参数")
public class OcrForm {
    @NotNull(message = "组件类型不能为空")
    @Schema(description = "组件类型：文本，身份正面，身份反面，营业执照，发票，驾驶证，行驶证，银行卡，火车票")
    private String type;

    @NotNull(message = "文件路径不能为空")
    @Schema(description = "文件路径")
    private String url;
}

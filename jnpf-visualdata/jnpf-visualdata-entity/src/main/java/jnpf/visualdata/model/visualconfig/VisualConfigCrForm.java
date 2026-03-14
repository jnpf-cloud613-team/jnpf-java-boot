package jnpf.visualdata.model.visualconfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年6月15日
 */
@Data
public class VisualConfigCrForm {
    @Schema(description ="大屏详情")
    private String detail;
    @Schema(description ="内容")
    private String component;
}

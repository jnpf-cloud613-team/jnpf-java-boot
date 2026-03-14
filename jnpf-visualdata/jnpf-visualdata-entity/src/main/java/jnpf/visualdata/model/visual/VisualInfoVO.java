package jnpf.visualdata.model.visual;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.visualdata.model.visualconfig.VisualConfigInfoModel;
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
public class VisualInfoVO {
    @Schema(description ="大屏基本信息")
    private VisualInfoModel visual;
    @Schema(description ="大屏配置")
    private VisualConfigInfoModel config;
}

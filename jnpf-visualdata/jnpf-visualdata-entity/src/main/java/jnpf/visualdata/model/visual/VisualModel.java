package jnpf.visualdata.model.visual;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.visualdata.entity.VisualConfigEntity;
import jnpf.visualdata.entity.VisualEntity;
import lombok.Data;

/**
 * 大屏导出
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年7月10日
 */
@Data
public class VisualModel {
    @Schema(description ="大屏基本信息")
    private VisualEntity entity;
    @Schema(description ="大屏配置信息")
    private VisualConfigEntity configEntity;
}

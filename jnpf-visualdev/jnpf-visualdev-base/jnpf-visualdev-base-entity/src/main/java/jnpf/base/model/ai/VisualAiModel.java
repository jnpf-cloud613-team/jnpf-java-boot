package jnpf.base.model.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.model.ai.AiFormModel;
import lombok.Data;

import java.util.List;

/**
 * ai模型列表
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/12/2 16:09:38
 */
@Data
@Schema(description = "ai模型列表")
public class VisualAiModel {
    @Schema(description = "模型编码")
    private String enCode;
    @Schema(description = "模型名称")
    private String fullName;
    @Schema(description = "ai模型列表")
    private List<AiFormModel> aiModelList;
}

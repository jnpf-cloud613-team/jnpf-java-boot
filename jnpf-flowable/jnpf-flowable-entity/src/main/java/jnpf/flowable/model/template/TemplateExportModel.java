package jnpf.flowable.model.template;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.flowable.entity.TemplateEntity;
import jnpf.flowable.entity.TemplateNodeEntity;
import jnpf.flowable.model.templatejson.TemplateJsonExportModel;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 定义导出
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/17 10:38
 */
@Data
public class TemplateExportModel implements Serializable {
    @Schema(description = "流程定义")
    private TemplateEntity template;

    @Schema(description = "流程定义版本")
    private TemplateJsonExportModel flowVersion;

    @Schema(description = "流程节点")
    private List<TemplateNodeEntity> nodeList;
}

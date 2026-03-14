package jnpf.flowable.model.templatejson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 版本导出
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/17 10:48
 */
@Data
public class TemplateJsonExportModel implements Serializable {
    /**
     * 流程模板id
     */
    @Schema(description = "流程模板id")
    private String templateId;

    /**
     * 可见类型 1.公开 2.权限设置
     */
    @Schema(description = "可见类型 1.公开 2.权限设置")
    private Integer visibleType;

    /**
     * 流程版本
     */
    @Schema(description = "流程版本")
    private String flowVersion;

    /**
     * 状态(0.设计,1.启用,2.历史)
     */
    @Schema(description = "状态(0.设计,1.启用,2.历史)")
    private Integer state;

    /**
     * 流程模板
     */
    @Schema(description = "流程模板")
    private String flowXml;

    /**
     * 消息配置id
     */
    @Schema(description = "消息配置id")
    private String sendConfigIds;
}

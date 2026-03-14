package jnpf.flowable.model.flowable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 引擎任务
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 9:17
 */
@Data
public class FlowableTaskModel implements Serializable {
    /**
     * 任务ID
     */
    @Schema(name = "taskId", description = "任务ID")
    private String taskId;
    /**
     * 任务名称
     */
    @Schema(name = "taskName", description = "任务名称")
    private String taskName;
    /**
     * 任务Key
     */
    @Schema(name = "taskKey", description = "任务Key")
    private String taskKey;
    /**
     * 实例ID
     */
    @Schema(name = "instanceId", description = "实例ID")
    private String instanceId;
}

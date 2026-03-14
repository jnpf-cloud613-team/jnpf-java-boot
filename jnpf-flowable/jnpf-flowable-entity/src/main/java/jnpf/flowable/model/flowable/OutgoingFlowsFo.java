package jnpf.flowable.model.flowable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/18 13:34
 */
@Data
public class OutgoingFlowsFo implements Serializable {
    /**
     * 部署ID
     */
    @Schema(name = "deploymentId", description = "部署ID")
    private String deploymentId;
    /**
     * 节点Key
     */
    @Schema(name = "taskKey", description = "节点Key")
    private String taskKey;
    /**
     * 任务ID
     */
    @Schema(name = "taskId", description = "任务ID")
    private String taskId;
}

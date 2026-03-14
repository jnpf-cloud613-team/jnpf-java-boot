package jnpf.flowable.model.flowable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/8 9:13
 */
@Data
public class BackFo {
    /**
     * 任务ID
     */
    @Schema(name = "taskId", description = "任务ID")
    private String taskId;
    /**
     * 目标节点ID
     */
    @Schema(name = "targetKey", description = "目标节点ID")
    private String targetKey;
}

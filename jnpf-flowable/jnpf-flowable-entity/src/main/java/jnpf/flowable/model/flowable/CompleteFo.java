package jnpf.flowable.model.flowable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/9 9:53
 */
@Data
public class CompleteFo {
    /**
     * 任务ID
     */
    @Schema(name = "taskId", description = "任务ID")
    private String taskId;
    /**
     * 变量
     */
    @Schema(name = "variables", description = "变量")
    private Map<String, Object> variables;
}

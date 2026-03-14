package jnpf.flowable.model.flowable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 启动实例参数类
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/17 21:14
 */
@Data
public class InstanceStartFo implements Serializable {
    /**
     * 部署ID
     */
    @Schema(name = "deploymentId", description = "部署ID")
    private String deploymentId;
    /**
     * 变量
     */
    @Schema(name = "variables", description = "变量")
    private Map<String, Object> variables;
}

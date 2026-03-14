package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.flowable.model.util.FlowNature;
import lombok.Data;

@Data
public class FileConfig {
    /**
     * 流程归档配置
     */
    @Schema(description = "流程归档配置")
    private Boolean on = false;
    /**
     * 1：当前流程所有人  2：流程发起人  3：最后节点审批人
     */
    @Schema(description = "查看权限")
    private Integer permissionType = FlowNature.FLOW_ALL;
    /**
     * 归档路径
     */
    @Schema(description = "归档路径")
    private String parentId;
    /**
     * 归档模板
     */
    @Schema(description = "归档模板")
    private String templateId;
}

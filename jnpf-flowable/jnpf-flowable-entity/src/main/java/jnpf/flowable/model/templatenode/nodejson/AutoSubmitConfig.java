package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AutoSubmitConfig {
    /**
     * 相邻节点审批人重复
     */
    @Schema(description = "相邻节点审批人重复")
    private Boolean adjacentNodeApproverRepeated = false;
    /**
     * 审批人审批过该流程
     */
    @Schema(description = "审批人审批过该流程")
    private Boolean approverHasApproval = false;
    /**
     * 发起人与审批人重复
     */
    @Schema(description = "发起人与审批人重复")
    private Boolean initiatorApproverRepeated = false;
}

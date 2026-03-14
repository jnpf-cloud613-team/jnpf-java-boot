package jnpf.flowable.model.templatenode.nodejson;

import jnpf.flowable.model.util.FlowNature;
import lombok.Data;

@Data
public class ApproversConfig {

    /**
     * 审批起点 1：发起人 2：上节点审批人
     */
    private Integer start = FlowNature.INITIATOR;
    /**
     * 审批终点 1：发起人 2：上节点审批人  3：组织机构的
     */
    private Integer end = FlowNature.INITIATOR;
    /**
     * 直属主管
     */
    private Integer level = 1;
    /**
     * 组织层级
     */
    private Integer originLevel = 1;
}

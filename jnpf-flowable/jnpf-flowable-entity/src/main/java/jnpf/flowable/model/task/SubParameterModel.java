package jnpf.flowable.model.task;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务子流程参数
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/21 9:43
 */
@Data
public class SubParameterModel implements Serializable {
    /**
     * 子流程 上一级任务 节点编码（如果子流程是合流节点 就存最后一个审批的分流节点）
     */
    private String parentCode;
    /**
     * flowable task id
     */
    private String nodeId;
}

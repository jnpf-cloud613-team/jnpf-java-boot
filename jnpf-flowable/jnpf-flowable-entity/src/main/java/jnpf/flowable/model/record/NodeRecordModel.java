package jnpf.flowable.model.record;

import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/6/25 17:18
 */
@Data
public class NodeRecordModel {
    /**
     * 任务id
     */
    private String taskId;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 节点编码
     */
    private String nodeCode;
    /**
     * 节点名称
     */
    private String nodeName;
    /**
     * 节点状态，1-已提交 2-已通过 3-已拒绝 4-审批中 5-已退回 6-已撤回
     */
    private Integer nodeStatus;
}

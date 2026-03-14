package jnpf.flowable.model.free;

import jnpf.flowable.model.templatenode.nodejson.CounterSignConfig;
import jnpf.flowable.model.util.FlowNature;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class FreeModel  implements Serializable {
    /**
     * 是否结束
     */
    private Boolean isEnd = true;

    /**
     * 用户
     */
    private List<String> approvers = new ArrayList<>();

    /**
     * 节点id
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 审批类型（0：或签 1：会签  2：依次审批）
     */
    private Integer counterSign = FlowNature.FIXED_APPROVER;

    /**
     * 会签比例
     */
    private CounterSignConfig counterSignConfig = new CounterSignConfig();

    /**
     * 依次顺序
     */
    private List<String> approversSortList = new ArrayList<>();


}

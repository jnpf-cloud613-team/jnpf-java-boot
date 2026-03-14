package jnpf.flowable.model.task;

import jnpf.flowable.model.templatenode.FlowErrorModel;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 审批返回
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/6/20 15:49
 */
@Data
public class AuditModel {
    /**
     * 是否结束
     */
    private Boolean isEnd = false;
    /**
     * 异常处理
     */
    private Set<FlowErrorModel> errorCodeList = new HashSet<>();
    /**
     * 任务主键
     */
    private String taskId;
}

package jnpf.flowable.model.flowable;

import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/6/18 17:21
 */
@Data
public class FlowableHistoricModel {
    /**
     * 任务ID
     */
    private String taskId;
    /**
     * 节点编码
     */
    private String code;
    /**
     * 开始时间
     */
    private Long startTime;
}

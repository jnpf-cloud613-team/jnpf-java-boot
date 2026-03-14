package jnpf.flowable.model.record;

import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.model.task.FlowModel;
import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/26 17:10
 */
@Data
public class RecordModel {
    /**
     * 记录操作类型，RecordEnum
     */
    private Integer type;
    /**
     * 审批原因
     */
    private FlowModel flowModel = new FlowModel();
    /**
     * 流转操作人，如加签给谁
     */
    private String userId;
    /**
     * 经办对象
     */
    private OperatorEntity operator = new OperatorEntity();

}

package jnpf.flowable.model.util;

import jnpf.flowable.entity.OperatorEntity;
import jnpf.flowable.model.task.FlowModel;
import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/10/9 8:59
 */
@Data
public class SystemAuditModel {
    private OperatorEntity operator;
    private FlowModel flowModel;
}

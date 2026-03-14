package jnpf.flowable.model.template;

import lombok.Data;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/7/22 11:15
 */
@Data
public class FlowConfigModel {
    /**
     * 发起可见类型(1-全部 2-权限)
     */
    private Integer visibleType = 1;
}

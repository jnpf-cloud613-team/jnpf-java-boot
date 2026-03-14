package jnpf.flowable.model.flowable;

import lombok.Data;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/6/4 15:59
 */
@Data
public class CompensateFo {
    /**
     * 实例主键
     */
    private String instanceId;
    /**
     * 原先的节点
     */
    private List<String> source;
}

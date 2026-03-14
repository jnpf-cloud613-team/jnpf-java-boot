package jnpf.flowable.model.flowable;

import lombok.Data;

import java.util.List;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/5/9 16:36
 */
@Data
public class JumpFo {
    /**
     * 实例ID
     */
    private String instanceId;
    /**
     * 源节点集合
     */
    private List<String> source;
    /**
     * 目标节点集合
     */
    private List<String> target;
}

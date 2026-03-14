package jnpf.flowable.enums;

import lombok.Getter;

/**
 * 分流规则（网关类型）
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/19 9:21
 */
@Getter
public enum DivideRuleEnum {
    /**
     * 根据条件多分支流转(包容网关)
     */
    INCLUSION("inclusion"),
    /**
     * 根据条件单分支流转（排它网关）
     */
    EXCLUSIVE("exclusive"),
    /**
     * 所有分支都流转（并行网关）
     */
    PARALLEL("parallel"),
    /**
     * 选择分支
     */
    CHOOSE("choose"),
    /**
     * 网关
     */
    GATEWAY("gateway"),
    /**
     * 汇合网关
     */
    CONFLUENCE("confluence"),

    ;

    private final String type;

    DivideRuleEnum(String type) {
        this.type = type;
    }
}

package jnpf.flowable.enums;

import lombok.Getter;

@Getter
public enum NodeTypeEnum {
    /**
     * 没有经过
     */
    NONE("-1", "没有经过"),
    /**
     * 经过
     */
    PASS("0", "经过"),
    /**
     * 当前
     */
    CURRENT("1", "当前"),
    /**
     * 未经过
     */
    NO_PASS("2", "未经过"),
    /**
     * 异常
     */
    EXCEPTION("3", "异常"),

    ;

    private final String type;
    private final String message;

    NodeTypeEnum(String type, String message) {
        this.type = type;
        this.message = message;
    }
}

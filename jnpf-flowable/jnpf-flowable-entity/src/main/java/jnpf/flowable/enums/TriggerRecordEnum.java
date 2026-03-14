package jnpf.flowable.enums;

import lombok.Getter;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/9/11 9:20
 */
@Getter
public enum TriggerRecordEnum {
    /**
     * 通过
     */
    PASSED(0, "通过"),
    /**
     * 异常
     */
    EXCEPTION(1, "异常"),
    /**
     * 进行中
     */
    WAIT(2, "进行中"),
    ;

    private final Integer code;
    private final String message;

    TriggerRecordEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

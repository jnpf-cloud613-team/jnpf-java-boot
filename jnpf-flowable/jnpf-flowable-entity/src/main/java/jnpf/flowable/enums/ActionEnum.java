package jnpf.flowable.enums;

import lombok.Getter;

@Getter
public enum ActionEnum {

    /**
     * 无
     */
    NONE(0, "无"),
    /**
     * 同意
     */
    AUDIT(1, "同意"),
    /**
     * 拒绝
     */
    REJECT(2, "拒绝"),
    /**
     * 退回
     */
    BACK(3, "退回"),
    /**
     * 办理
     */
    PROCESSING(4, "办理"),

    ;


    private final Integer code;
    private final String message;

    ActionEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据状态code获取枚举名称
     *
     * @return
     */
    public static ActionEnum getByCode(Integer code) {
        for (ActionEnum status : ActionEnum.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return ActionEnum.NONE;
    }
}

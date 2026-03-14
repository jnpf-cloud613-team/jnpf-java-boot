package jnpf.flowable.enums;

import lombok.Getter;

@Getter
public enum TemplateStatueEnum {

    /**
     * 未上架
     */
    NONE(0, "未上架"),
    /**
     * 上架
     */
    UP(1, "上架"),
    /**
     * 下架-继续审批
     */
    DOWN_CONTINUE(2, "下架-继续审批"),
    /**
     * 下架-隐藏审批
     */
    DOWN_HIDDEN(3, "下架-隐藏审批"),
    ;

    private final Integer code;
    private final String message;

    TemplateStatueEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

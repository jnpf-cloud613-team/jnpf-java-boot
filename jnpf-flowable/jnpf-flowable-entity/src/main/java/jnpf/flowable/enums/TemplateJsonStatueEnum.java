package jnpf.flowable.enums;

import lombok.Getter;

@Getter
public enum TemplateJsonStatueEnum {
    /**
     * 设计
     */
    DESIGN(0, "设计"),
    /**
     * 启用
     */
    START(1, "启用"),
    /**
     * 历史
     */
    HISTORY(2, "历史"),
    ;

    private final Integer code;
    private final String message;

    TemplateJsonStatueEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

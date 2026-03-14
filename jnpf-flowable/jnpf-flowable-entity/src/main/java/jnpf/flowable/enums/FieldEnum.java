package jnpf.flowable.enums;

import lombok.Getter;

@Getter
public enum FieldEnum {
    /**
     * 字段
     */
    FIELD(1, "字段"),
    /**
     * 自定义
     */
    CUSTOM(2, "自定义"),
    /**
     * 节点、条件的系统参数
     */
    CONDITION(3,"系统参数"),
    /**
     * 接口的系统参数
     */
    SYSTEM(4, "系统参数"),
    /**
     * 全局变量
     */
    GLOBAL(5, "全局变量");

    private final Integer code;
    private final String message;

    FieldEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

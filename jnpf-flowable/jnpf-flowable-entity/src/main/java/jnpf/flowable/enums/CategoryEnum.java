package jnpf.flowable.enums;


import lombok.Getter;

@Getter
public enum CategoryEnum {
    /**
     * 没有经过
     */
    NONE("-1", "无"),
    /**
     * 待签事宜
     */
    SIGN("0", "待签事宜"),
    /**
     * 待办事宜
     */
    TODO("1", "待办事宜"),
    /**
     * 在办事宜
     */
    DOING("2", "在办事宜"),
    /**
     * 已办事宜
     */
    DONE("3", "已办事宜"),
    /**
     * 抄送事宜
     */
    CIRCULATE("4", "抄送事宜"),
    /**
     * 批量在办事宜
     */
    BATCH_DOING("5", "批量在办事宜"),

    ;


    private final String type;
    private final String message;

    CategoryEnum(String type, String message) {
        this.type = type;
        this.message = message;
    }


    public static CategoryEnum getType(String type) {
        for (CategoryEnum status : CategoryEnum.values()) {
            if (status.getType().equals(type)) {
                return status;
            }
        }
        return CategoryEnum.NONE;
    }
}

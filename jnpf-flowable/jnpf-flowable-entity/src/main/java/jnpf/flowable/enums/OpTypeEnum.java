package jnpf.flowable.enums;

import lombok.Getter;

@Getter
public enum OpTypeEnum {

    /**
     * 我发起的新建/编辑
     */
    LAUNCH_CREATE("-1", "我发起的新建/编辑"),
    /**
     * 我发起的详情
     */
    LAUNCH_DETAIL("0", "我发起的详情"),
    /**
     * 待签事宜
     */
    SIGN("1", "待签事宜"),
    /**
     * 待办事宜
     */
    TODO("2", "待办事宜"),
    /**
     * 在办事宜
     */
    DOING("3", "在办事宜"),
    /**
     * 已办事宜
     */
    DONE("4", "已办事宜"),
    /**
     * 抄送事宜
     */
    CIRCULATE("5", "抄送事宜"),
    /**
     * 流程监控
     */
    MONITOR("6", "流程监控"),

    ;

    private final String type;
    private final String message;

    OpTypeEnum(String type, String message) {
        this.type = type;
        this.message = message;
    }

    /**
     * 根据状态code获取枚举名称
     *
     * @return
     */
    public static OpTypeEnum getType(String type) {
        for (OpTypeEnum status : OpTypeEnum.values()) {
            if (status.getType().equals(type)) {
                return status;
            }
        }
        return OpTypeEnum.LAUNCH_CREATE;
    }
}

package jnpf.flowable.enums;

import lombok.Getter;

@Getter
public enum EventEnum {
    /**
     * 无
     */
    NONE(0, "无"),
    /**
     * 发起
     */
    INIT(1, "发起"),
    /**
     * 结束
     */
    END(2, "结束"),
    /**
     * 发起撤回
     */
    FLOW_RECALL(3, "发起撤回"),
    /**
     * 同意
     */
    APPROVE(4, "同意"),
    /**
     * 拒绝
     */
    REJECT(5, "拒绝"),
    /**
     * 节点撤回
     */
    RECALL(6, "节点撤回"),
    /**
     * 超时
     */
    OVERTIME(7, "超时"),
    /**
     * 提醒
     */
    NOTICE(8, "提醒"),
    /**
     * 退回
     */
    BACK(9, "退回"),


    ;

    private final Integer status;
    private final String message;

    EventEnum(int status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * 根据状态status获取枚举名称
     *
     * @return
     */
    public static EventEnum getEventStatus(Integer status) {
        for (EventEnum eventEnum : EventEnum.values()) {
            if (eventEnum.getStatus().equals(status)) {
                return eventEnum;
            }
        }
        return EventEnum.NONE;
    }
}

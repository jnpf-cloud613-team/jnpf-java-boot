package jnpf.flowable.enums;

import lombok.Getter;

@Getter
public enum NodeEnum {
    /**
     * 开始
     */
    START("start"),
    /**
     * 结束
     */
    END("end"),
    /**
     * 全局
     */
    GLOBAL("global"),
    /**
     * 子流程
     */
    SUB_FLOW("subFlow"),
    /**
     * 线
     */
    CONNECT("connect"),
    /**
     * 审批
     */
    APPROVER("approver"),
    /**
     * 办理
     */
    PROCESSING("processing"),
    /**
     * 外部
     */
    OUTSIDE("outside"),
    /**
     * 触发事件
     */
    TRIGGER("trigger"),
    /**
     * 事件触发
     */
    EVENT_TRIGGER("eventTrigger"),
    /**
     * 定时触发
     */
    TIME_TRIGGER("timeTrigger"),
    /**
     * 通知触发
     */
    NOTICE_TRIGGER("noticeTrigger"),
    /**
     * webhook触发
     */
    WEBHOOK_TRIGGER("webhookTrigger"),
    /**
     * 获取数据
     */
    GET_DATA("getData"),
    /**
     * 新增数据
     */
    ADD_DATA("addData"),
    /**
     * 更新数据
     */
    UPDATE_DATA("updateData"),
    /**
     * 删除数据
     */
    DELETE_DATA("deleteData"),
    /**
     * 数据接口
     */
    DATA_INTERFACE("dataInterface"),
    /**
     * 消息通知
     */
    MESSAGE("message"),
    /**
     * 发起审批
     */
    LAUNCH_FLOW("launchFlow"),
    /**
     * 创建日程
     */
    SCHEDULE("schedule");

    private final String type;

    NodeEnum(String type) {
        this.type = type;
    }

    /**
     * 根据状态code获取枚举名称
     *
     * @return
     */
    public static NodeEnum getNode(String code) {
        for (NodeEnum status : NodeEnum.values()) {
            if (status.getType().equals(code)) {
                return status;
            }
        }
        return NodeEnum.START;
    }
}

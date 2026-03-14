package jnpf.flowable.enums;

import lombok.Getter;


@Getter
public enum NodeStateEnum {
    /**
     * 已提交
     */
    SUBMIT(1, "已提交"),
    /**
     * 已通过
     */
    PASS(2, "已通过"),
    /**
     * 已拒绝
     */
    REJECT(3, "已拒绝"),
    /**
     * 审批中
     */
    APPROVAL(4, "审批中"),
    /**
     * 已退回
     */
    BACK(5, "已退回"),
    /**
     * 已撤回
     */
    RECALL(6, "已撤回"),
    /**
     * 等待中
     */
    WAIT(7, "等待中"),
    /**
     * 待办理
     */
    TRANSACT(8, "待办理");

    private final Integer code;
    private final String message;

    NodeStateEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}

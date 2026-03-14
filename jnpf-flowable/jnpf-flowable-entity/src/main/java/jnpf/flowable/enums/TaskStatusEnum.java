package jnpf.flowable.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/17 19:03
 */
@Getter
public enum TaskStatusEnum {
    /**
     * 待提交
     */
    TO_BE_SUBMIT(0, "待提交"),
    /**
     * 进行中
     */
    RUNNING(1, "进行中"),
    /**
     * 已通过
     */
    PASSED(2, "已通过"),
    /**
     * 已拒绝
     */
    REJECTED(3, "已拒绝"),
    /**
     * 已终止
     */
    CANCEL(4, "已终止"),
    /**
     * 已暂停
     */
    PAUSED(5, "已暂停"),
    /**
     * 撤销中
     */
    REVOKING(6, "撤销中"),
    /**
     * 已撤销
     */
    REVOKED(7, "已撤销"),
    /**
     * 已退回
     */
    BACKED(8, "已退回"),
    /**
     * 已撤回
     */
    RECALL(9, "已撤回"),
    /**
     * 异常
     */
    EXCEPTION(10, "异常"),

    /**
     * 用于嵌入的任务流程
     */
    WAITING(-2, "未激活"),

    ;

    private final Integer code;
    private final String message;

    TaskStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

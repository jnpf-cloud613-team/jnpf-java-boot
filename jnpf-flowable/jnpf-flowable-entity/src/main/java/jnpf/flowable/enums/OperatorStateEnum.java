package jnpf.flowable.enums;

import lombok.Getter;

/**
 * 经办类型
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/23 17:23
 */
@Getter
public enum OperatorStateEnum {

    // 待去除
    WAIT_SIGN(0, "待签收"),

    RUNING(1, "流转中"),

    ADD_SIGN(2, "加签"),

    TRANSFER(3, "转审"),

    ASSIGNED(4, "指派"),

    BACK(5, "退回"),

    RECALL(6, "撤回"),

    ASSIST(7, "协办"),

    REVOKE(8, "撤销"),

    TRANSFER_PROCESSING(9, "转办"),

    //无用节点
    FUTILITY(-1, "无用节点"),
    // 用于嵌入的任务流程
    WAITING(-2, "未激活")
    ;

    private final Integer code;
    private final String message;

    OperatorStateEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}

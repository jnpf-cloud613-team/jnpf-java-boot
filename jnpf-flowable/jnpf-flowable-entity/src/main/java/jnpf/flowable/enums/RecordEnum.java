package jnpf.flowable.enums;

import lombok.Getter;

/**
 * 类的描述
 *
 * @author JNPF@YinMai Info. Co., Ltd
 * @version 5.0.x
 * @since 2024/4/26 17:32
 */
@Getter
public enum RecordEnum {
    //拒绝
    REJECT(0, "拒绝"),
    //同意
    AUDIT(1, "同意"),
    //提交
    SUBMIT(2, "提交"),
    //退回
    BACK(3, "退回"),
    //撤回
    RECALL(4, "撤回"),
    //加签
    ADD_SIGN(5, "加签"),
    //减签
    SUBTRACT_SIGN(6, "减签"),
    //转审
    TRANSFER(7, "转审"),
    //暂停
    PAUSE(8, "暂停"),
    //重启
    REBOOT(9, "重启"),
    //复活
    ACTIVATE(10, "复活"),
    //指派
    ASSIGN(11, "指派"),
    //催办
    PRESS(12, "催办"),
    //协办
    ASSIST(13, "协办"),
    //撤销
    REVOKE(14, "撤销"),
    //终止
    CANCEL(15, "终止"),

    AUDIT_REVOKE(16, "同意撤销"),

    REJECT_REVOKE(17, "拒绝撤销"),

    TRANSFER_PROCESSING(18, "转办");

    private final Integer code;
    private final String message;

    RecordEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}

package jnpf.flowable.enums;

import lombok.Getter;

/**
 * 异常规则
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/6/17 10:57
 */
@Getter
public enum ErrorRuleEnum {
    /**
     * 1.超级管理员
     */
    ADMINISTRATOR(1, "超级管理员"),
    /**
     * 2.指定人员
     */
    INITIATOR(2, "指定人员"),
    /**
     * 3.上一节点审批人指定处理人
     */
    NODE(3, "上一节点审批人指定处理人"),
    /**
     * 4.默认审批通过
     */
    PASS(4, "默认审批通过"),
    /**
     * 5.无法提交
     */
    NOT_SUBMIT(5, "无法提交"),
    /**
     * 6.发起者本人处理
     */
    CREATOR_USER_ID(6, "发起者本人处理");

    private final Integer code;
    private final String message;

    ErrorRuleEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据状态code获取枚举名称
     *
     * @return
     */
    public static ErrorRuleEnum getByCode(Integer code) {
        for (ErrorRuleEnum status : ErrorRuleEnum.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return ErrorRuleEnum.ADMINISTRATOR;
    }

}

package jnpf.flowable.enums;

import lombok.Getter;

/**
 * 附件条件
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/6/14 16:50
 */
@Getter
public enum ExtraRuleEnum {
    /**
     * 无条件
     */
    NONE(1, "无条件"),
    /**
     * 同一部门
     */
    ORGANIZE(2, "同一部门"),
    /**
     * 同一岗位
     */
    POSITION(3, "同一岗位"),
    /**
     * 发起人上级
     */
    MANAGER(4, "发起人上级"),
    /**
     * 发起人下属
     */
    SUBORDINATE(5, "发起人下属"),
    /**
     * 同一公司
     */
    DEPARTMENT(6, "同一公司"),
    /**
     * 同一角色
     */
    ROLE(7, "同一角色"),
    /**
     * 同一分组
     */
    GROUP(8, "同一分组");

    private final Integer code;
    private final String message;

    ExtraRuleEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据状态code获取枚举名称
     *
     * @return
     */
    public static ExtraRuleEnum getByCode(Integer code) {
        for (ExtraRuleEnum status : ExtraRuleEnum.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return ExtraRuleEnum.NONE;
    }

}

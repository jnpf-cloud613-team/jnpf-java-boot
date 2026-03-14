package jnpf.flowable.enums;

import lombok.Getter;

/**
 * 经办对象
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
@Getter
public enum OperatorEnum {

    /**
     * 发起者主管
     */
    LAUNCH_CHARGE(1, "发起者主管"),
    /**
     * 部门经理
     */
    DEPARTMENT_CHARGE(2, "部门经理"),
    /**
     * 发起者本人
     */
    INITIATOR_ME(3, "发起者本人"),
    /**
     * 变量
     */
    VARIATE(4, "变量"),
    /**
     * 环节
     */
    LINK(5, "环节"),
    /**
     * 指定人
     */
    NOMINATOR(6, "指定人"),
    /**
     * 服务
     */
    SERVE(9, "服务"),
    /**
     * 逐级
     */
    STEP(10, "逐级");


    private final Integer code;
    private final String message;

    OperatorEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

}

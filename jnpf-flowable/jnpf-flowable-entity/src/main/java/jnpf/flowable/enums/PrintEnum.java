package jnpf.flowable.enums;

import lombok.Getter;

/**
 * 打印条件
 *
 * @author ：JNPF开发平台组
 * @version: V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date ：2022/6/14 16:50
 */
@Getter
public enum PrintEnum {
    /**
     * 不限制
     */
    NONE(1, "不限制"),
    /**
     * 节点结束
     */
    NODE_END(2, "节点结束"),
    /**
     * 流程结束
     */
    FLOW_END(3, "流程结束"),
    /**
     * 条件设置
     */
    CONDITIONS(4, "条件设置");

    private final Integer code;
    private final String message;

    PrintEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据状态code获取枚举名称
     *
     * @return
     */
    public static PrintEnum getPrint(Integer code) {
        for (PrintEnum status : PrintEnum.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return PrintEnum.NONE;
    }

}

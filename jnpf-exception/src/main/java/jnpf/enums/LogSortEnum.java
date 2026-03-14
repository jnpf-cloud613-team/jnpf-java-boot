package jnpf.enums;

/**
 * 日志分类
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月26日 上午9:18
 */
public enum LogSortEnum {
    /**
     * 登录
     */
    LOGIN(1, "登录"),
    /**
     * 访问
     */
    VISIT(2, "访问"),
    /**
     * 操作
     */
    OPERATE(3, "操作"),
    /**
     * 异常
     */
    EXCEPTION(4, "异常"),
    /**
     * 请求
     */
    REQUEST(5, "请求");

    private int code;
    private String message;

    LogSortEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }


    public String getMessage() {
        return message;
    }


    /**
     * 根据状态code获取枚举名称
     *
     * @return
     */
    public static String getMessageByCode(Integer code) {
        for (LogSortEnum status : LogSortEnum.values()) {
            if (status.getCode().equals(code)) {
                return status.message;
            }
        }
        return null;
    }

    /**
     * 根据状态code获取枚举值
     *
     * @return
     */
    public static LogSortEnum getByCode(Integer code) {
        for (LogSortEnum status : LogSortEnum.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}

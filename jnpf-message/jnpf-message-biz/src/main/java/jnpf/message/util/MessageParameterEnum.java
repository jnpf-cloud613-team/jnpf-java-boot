package jnpf.message.util;

/**
 * 获取消息参数
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-10
 */
public enum MessageParameterEnum {

    /**
     * 接收者ID
     */
    PARAMETER_TOUSERID("toUserId"),
    PARAMETER_MESSAGETYPE("messageType"),
    PARAMETER_MESSAGECONTENT("messageContent"),
    PARAMETER_TOKEN("token"),
    PARAMETER_METHOD("method"),
    PARAMETER_MOBILEDEVICE("mobileDevice"),
    ;

    private String value;

    MessageParameterEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

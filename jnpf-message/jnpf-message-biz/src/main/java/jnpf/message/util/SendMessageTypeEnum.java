package jnpf.message.util;

/**
 * 消息类型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-10
 */
public enum SendMessageTypeEnum {

    /**
     * 文本消息
     */
    MESSAGE_TEXT("text"),
    /**
     * 语音消息
     */
    MESSAGE_VOICE("voice"),
    /**
     * 图片消息
     */
    MESSAGE_IMAGE("image");

    SendMessageTypeEnum() {
    }

    private String message;

    SendMessageTypeEnum(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

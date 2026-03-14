package jnpf.message.util;

/**
 * Websocket连接类型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-12
 */
public class ConnectionType {

    ConnectionType() {
    }

    /**
     * 建立连接
     */
    public static final String CONNECTION_ONCONNECTION = "OnConnection";

    /**
     * 发型消息
     */
    public static final String CONNECTION_SENDMESSAGE = "SendMessage";

}

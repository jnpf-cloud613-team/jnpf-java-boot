package jnpf.message.model.websocket.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 消息模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-07
 */
@Data
public class MessageModel implements Serializable {

    protected String method;

}

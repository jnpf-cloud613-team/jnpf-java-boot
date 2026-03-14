package jnpf.message.model.websocket.receivemessage;

import jnpf.message.model.websocket.model.MessageModel;
import lombok.Data;

import java.io.Serializable;

/**
 * 返回接受消息模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-13
 */
@Data
public class ReceiveMessageVO extends MessageModel implements Serializable {

    private String formUserId;

    private Long dateTime;

    private String headIcon;

    private Long latestDate;

    private String realName;

    private String account;

    private String messageType;

    private Object formMessage;

}

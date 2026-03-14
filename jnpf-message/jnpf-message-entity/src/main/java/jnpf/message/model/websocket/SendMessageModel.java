package jnpf.message.model.websocket;

import jnpf.base.UserInfo;
import jnpf.message.entity.MessageEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 发送消息到mq模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageModel implements Serializable {

    private List<String> toUserIds;

    private MessageEntity entity;

    private UserInfo userInfo;

    private Integer messageType;

}

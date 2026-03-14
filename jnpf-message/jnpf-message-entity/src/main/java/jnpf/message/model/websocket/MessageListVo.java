package jnpf.message.model.websocket;

import lombok.Data;

import java.io.Serializable;

/**
 * 消息列表单个模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-05
 */
@Data
public class MessageListVo implements Serializable {

    private String content;

    private String contentType;

    private String id;

    private Long receiveTime;

    private String receiveUserId;

    private Long sendTime;

    private String sendUserId;

    private Integer state;

}

package jnpf.message.model.websocket.receivemessage;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jnpf.message.model.websocket.model.MessageModel;
import lombok.Data;

import java.io.Serializable;

/**
 * 接受消息模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-12
 */
@Data
public class ReceiveMessageModel extends MessageModel implements Serializable {

    private String formUserId;

    private Long dateTime;

    private String headIcon;

    private Long latestDate;

    private String realName;

    private String account;

    private String messageType;

    @JsonIgnore
    private String userId;

    @JsonIgnore
    private transient Object formMessage;

    // 如果需要序列化 formMessage，可以提供替代字段
    public String getFormMessageAsString() {
        return formMessage != null ? JSON.toJSONString(formMessage) : null;
    }
}

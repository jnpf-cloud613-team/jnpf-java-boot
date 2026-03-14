package jnpf.message.model.websocket.savamessage;

import com.alibaba.fastjson.annotation.JSONField;
import jnpf.message.model.websocket.model.MessageModel;
import lombok.Data;

import java.io.Serializable;

/**
 * 保存消息模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-12
 */
@Data
public class SavaMessageModel extends MessageModel implements Serializable {

    @JSONField(name = "UserId")
    private String userId;

    private String toUserId;

    private Long dateTime;

    private String headIcon;

    private Long latestDate;

    private String realName;

    private String account;

    private String toAccount;

    private String toRealName;

    private String toHeadIcon;

    private String messageType;

    private Object toMessage;

}

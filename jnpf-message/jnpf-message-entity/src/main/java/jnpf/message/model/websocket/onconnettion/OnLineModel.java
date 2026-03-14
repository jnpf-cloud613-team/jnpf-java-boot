package jnpf.message.model.websocket.onconnettion;

import jnpf.message.model.websocket.model.MessageModel;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户在线推送模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-10
 */
@Data
public class OnLineModel extends MessageModel implements Serializable {

    /**
     * 在线用户
     */
    private String userId;

    public OnLineModel(String method, String userId) {
        super.method = method;
        this.userId = userId;
    }
}

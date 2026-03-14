package jnpf.message.model.websocket.onclose;

import jnpf.message.model.websocket.model.MessageModel;
import lombok.Data;

import java.io.Serializable;

/**
 * 关闭连接model
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-12
 */
@Data
public class OnCloseModel extends MessageModel implements Serializable {

    private String userId;

    public OnCloseModel(String userId, String method) {
        this.userId = userId;
        super.method = method;
    }
}

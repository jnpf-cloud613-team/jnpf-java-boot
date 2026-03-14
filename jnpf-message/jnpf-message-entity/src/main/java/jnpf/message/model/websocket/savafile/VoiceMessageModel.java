package jnpf.message.model.websocket.savafile;

import lombok.Data;

import java.io.Serializable;

/**
 * 语音消息模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-12
 */
@Data
public class VoiceMessageModel extends MessageTypeModel implements Serializable {

    private String length;

    public VoiceMessageModel(String length, String path) {
        this.length = length;
        super.path = path;
    }

}

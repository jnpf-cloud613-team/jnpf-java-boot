package jnpf.message.model.websocket.savafile;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片和语音共有属性
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-12
 */
@Data
public class MessageTypeModel implements Serializable {

    protected String path;

}

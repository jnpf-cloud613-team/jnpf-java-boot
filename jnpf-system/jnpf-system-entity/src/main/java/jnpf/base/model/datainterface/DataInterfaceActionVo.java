package jnpf.base.model.datainterface;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据接口调用返回模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-12-24
 */
@Data
public class DataInterfaceActionVo implements Serializable {

    private Object data;

}

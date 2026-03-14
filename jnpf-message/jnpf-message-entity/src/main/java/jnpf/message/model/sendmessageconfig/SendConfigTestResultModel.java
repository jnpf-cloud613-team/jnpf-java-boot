package jnpf.message.model.sendmessageconfig;

import lombok.Data;

/**
 * 版本： V3.2.0
 * 版权: 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-19
 */
@Data
public class SendConfigTestResultModel {


    /**
     * 消息类型
     **/
    private String messageType;

    /**
     * 是否成功
     **/
    private String isSuccess;

    /**
     * 失败原因
     **/
    private String result;


}

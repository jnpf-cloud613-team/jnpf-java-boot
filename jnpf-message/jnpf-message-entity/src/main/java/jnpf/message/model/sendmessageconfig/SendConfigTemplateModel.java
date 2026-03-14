package jnpf.message.model.sendmessageconfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 版本： V3.2.0
 * 版权: 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-19
 */
@Data
public class SendConfigTemplateModel {

    private String id;

    /**
     * 消息发送配置id
     **/
    @JsonProperty("sendConfigId")
    private String sendConfigId;

    /**
     * 消息类型
     **/
    @JsonProperty("messageType")
    private String messageType;

    /**
     * 消息模板id
     **/
    @JsonProperty("templateId")
    private String templateId;

    /**
     * 账号配置id
     **/
    @JsonProperty("accountConfigId")
    private String accountConfigId;

    /**
     * 接收人
     **/
    private List<String> toUser;

    /**
     * 模板参数
     **/
    private Object paramJson;

    /**
     * 消息模板名称
     **/
    private String msgTemplateName;


}

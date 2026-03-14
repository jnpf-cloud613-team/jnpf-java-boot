

package jnpf.message.model.messagemonitor;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.alibaba.fastjson.annotation.JSONField;


/**
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-22
 */
@Data
public class MessageMonitorListVO {
    @Schema(description = "主键")
    private String id;

    /**
     * 消息类型
     **/
    @Schema(description = "消息类型")
    @JSONField(name = "messageType")
    private String messageType;

    /**
     * 消息来源
     **/
    @Schema(description = "消息来源")
    @JsonProperty("messageSource")
    private String messageSource;

    /**
     * 发送时间
     **/
    @Schema(description = "发送时间")
    @JSONField(name = "sendTime")
    private Long sendTime;

    /**
     * 标题
     **/
    @Schema(description = "标题")
    @JSONField(name = "title")
    private String title;


}
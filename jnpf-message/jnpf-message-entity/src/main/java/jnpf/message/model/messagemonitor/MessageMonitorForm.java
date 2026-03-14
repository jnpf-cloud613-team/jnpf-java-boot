

package jnpf.message.model.messagemonitor;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-22
 */
@Data
public class MessageMonitorForm {
    /**
     * 主键
     */
    @Schema(description = "主键")
    private String id;
    /**
     * 账号id
     **/
    @Schema(description = "账号id")
    @JsonProperty("accountId")
    private String accountId;

    /**
     * 账号名称
     **/
    @Schema(description = "账号名称")
    @JsonProperty("accountName")
    private String accountName;

    /**
     * 账号编码
     **/
    @Schema(description = "账号编码")
    @JsonProperty("accountCode")
    private String accountCode;

    /**
     * 消息类型
     **/
    @Schema(description = "消息类型")
    @JsonProperty("messageType")
    private String messageType;

    /**
     * 发送时间
     **/
    @Schema(description = "发送时间")
    @JsonProperty("sendTime")
    private String sendTime;

    /**
     * 消息模板id
     **/
    @Schema(description = "消息模板id")
    @JsonProperty("messageTemplateId")
    private String messageTemplateId;

    /**
     * 标题
     **/
    @Schema(description = "标题")
    @JsonProperty("title")
    private String title;

    /**
     * 内容
     **/
    @Schema(description = "内容")
    @JsonProperty("content")
    private String content;


}

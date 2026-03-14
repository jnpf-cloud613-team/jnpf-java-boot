


package jnpf.message.model.sendconfigrecord;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-09-21
 */
@Data
public class SendConfigRecordInfoVO {
    /**
     * 主键
     **/
    @Schema(description = "主键")
    @JsonProperty("id")
    private String id;

    /**
     * 发送配置id
     **/
    @Schema(description = "发送配置id")
    @JsonProperty("sendConfigId")
    private String sendConfigId;

    /**
     * 消息来源
     **/
    @Schema(description = "消息来源")
    @JsonProperty("messageSource")
    private String messageSource;

    /**
     * 被引用id
     **/
    @Schema(description = "被引用id")
    @JsonProperty("usedId")
    private String usedId;

    /**
     * 创建时间
     **/
    @Schema(description = "创建时间")
    @JsonProperty("creatorTime")
    private Long creatorTime;

    /**
     * 创建人员
     **/
    @Schema(description = "创建人员")
    @JsonProperty("creatorUserId")
    private String creatorUserId;

    /**
     * 修改时间
     **/
    @Schema(description = "修改时间")
    @JsonProperty("lastModifyTime")
    private Long lastModifyTime;

    /**
     * 修改人员
     **/
    @Schema(description = "修改人员")
    @JsonProperty("lastModifyUserId")
    private String lastModifyUserId;
}




package jnpf.message.model.sendmessageconfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jnpf.message.entity.SendConfigTemplateEntity;
import lombok.Data;

import java.util.List;

/**
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-19
 */
@Data
public class SendMessageConfigInfoVO {
    /**
     * 主键
     **/
    @Schema(description = "主键")
    @JsonProperty("id")
    private String id;

    /**
     * 名称
     **/
    @Schema(description = "名称")
    @JsonProperty("fullName")
    private String fullName;

    /**
     * 编码
     **/
    @Schema(description = "编码")
    @JsonProperty("enCode")
    private String enCode;


    /**
     * 模板类型
     **/
    @Schema(description = "模板类型")
    @JsonProperty("templateType")
    private String templateType;

    /**
     * 消息来源
     **/
    @Schema(description = "消息来源")
    @JsonProperty("messageSource")
    private String messageSource;

    @Schema(description = "消息源名称")
    private String messageSourceName;

    /**
     * 排序
     **/
    @Schema(description = "排序")
    @JsonProperty("sortCode")
    private Integer sortCode;

    /**
     * 状态
     **/
    @Schema(description = "状态")
    @JsonProperty("enabledMark")
    private String enabledMark;

    /**
     * 说明
     **/
    @Schema(description = "说明")
    @JsonProperty("description")
    private String description;

    /**
     * 子表数据
     **/
    @Schema(description = "子表数据")
    @JsonProperty("sendConfigTemplateList")
    private List<SendConfigTemplateEntity> sendConfigTemplateList;
}

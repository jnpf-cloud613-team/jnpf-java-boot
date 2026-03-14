package jnpf.base.model.messagetemplate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-12-11
 */
@Data
public class MessageTemplateSelector implements Serializable {
    @Schema(description = "主键")
    private String id;
    @Schema(description = "模板名称")
    private String fullName;
    @Schema(description = "消息类型")
    private String category;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "内容")
    private String content;
    @Schema(description = "模板参数JSON")
    private String templateJson;
    @Schema(description = "编码")
    private String enCode;
}

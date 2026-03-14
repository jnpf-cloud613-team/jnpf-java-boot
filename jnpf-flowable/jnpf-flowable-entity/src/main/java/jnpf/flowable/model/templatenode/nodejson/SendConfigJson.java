package jnpf.flowable.model.templatenode.nodejson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 版本： V3.2.0
 * 版权: 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-19
 */
@Data
public class SendConfigJson {

    @Schema(description = "主键")
    private String id;

    /**
     * 消息发送配置主键
     **/
    @Schema(description = "消息发送配置主键")
    private String sendConfigId;

    /**
     * 消息类型
     **/
    @Schema(description = "消息类型")
    private String messageType;

    /**
     * 消息模板主键
     **/
    @Schema(description = "消息模板主键")
    private String templateId;

    /**
     * 账号配置主键
     **/
    @Schema(description = "账号配置主键")
    private String accountConfigId;

    /**
     * 接收人
     **/
    @Schema(description = "接收人")
    private List<String> toUser;

    /**
     * 模板参数
     **/
    @Schema(description = "模板参数")
    private List<TemplateJsonModel> paramJson = new ArrayList<>();

    /**
     * 消息模板名称
     **/
    @Schema(description = "消息模板名称")
    private String msgTemplateName;


}

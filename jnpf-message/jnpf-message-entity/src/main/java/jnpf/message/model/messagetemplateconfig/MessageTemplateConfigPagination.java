package jnpf.message.model.messagetemplateconfig;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jnpf.base.Pagination;

/**
 * @版本： V3.2.0
 * @版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @作者： JNPF开发平台组
 * @日期： 2022-08-18
 */
@Data
public class MessageTemplateConfigPagination extends Pagination {

    @Schema(description = "selectKey")
    private String selectKey;

    @Schema(description = "json")
    private String json;

    @Schema(description = "数据类型")
    private String dataType;

    @Schema(description = "特殊查询json")
    private String superQueryJson;



    /**
     * 模板类型
     */
    @Schema(description = "模板类型")
    private String templateType;

    /**
     * 消息类型
     */
    @Schema(description = "消息类型")
    private String messageType;

    /**
     * 关键词
     */
    @Schema(description = "关键词")
    private String keyword;

    /**
     * 状态
     */
    @Schema(description = "状态")
    private String enabledMark;

    /**
     * 消息来源
     */
    @Schema(description = "消息来源")
    private String messageSource;

    /**
     * 菜单id
     */
    @Schema(description = "菜单id")
    private String menuId;
}
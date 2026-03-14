package jnpf.message.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 消息会话列表
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-05-29
 */
@Data
public class ImReplyListVo {
    /**
     * id
     */
    @Schema(description = "id")
    private String id;

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String realName;

    /**
     * 头像
     */
    @Schema(description = "头像")
    private String headIcon;

    /**
     * 最新消息
     */
    @Schema(description = "最新消息")
    private String latestMessage;

    /**
     * 最新时间
     */
    @Schema(description = "最新时间")
    private Long latestDate;

    /**
     * 未读消息
     */
    @Schema(description = "未读消息")
    private Integer unreadMessage;

    /**
     * 消息类型
     */
    @Schema(description = "消息类型")
    private String messageType;

    /**
     * Account
     */
    @Schema(description = "Account")
    private String account;

}

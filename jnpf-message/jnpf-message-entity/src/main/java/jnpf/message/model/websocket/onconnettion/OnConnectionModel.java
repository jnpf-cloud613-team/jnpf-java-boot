package jnpf.message.model.websocket.onconnettion;

import jnpf.message.model.ImUnreadNumModel;
import jnpf.message.model.websocket.model.MessageModel;
import lombok.Data;

import java.util.List;

/**
 * 刚连接websocket时推送的模型
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2021-07-07
 */
@Data
public class OnConnectionModel extends MessageModel {
    private static final long serialVersionUID = 1L;

    private List<String> onlineUsers;

    private transient List<ImUnreadNumModel> unreadNums;

    private Integer unreadNoticeCount;

    private String noticeDefaultText;

    private Integer unreadMessageCount;

    private Integer unreadScheduleCount;

    private Integer unreadSystemMessageCount;

    private Integer unreadFormCount;

    private String messageDefaultText;

    private Long messageDefaultTime;

    private Integer unreadTotalCount;

    private String userId;
}

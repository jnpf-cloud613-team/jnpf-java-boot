package jnpf.message.service;


import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.service.SuperService;
import jnpf.message.entity.MessageEntity;
import jnpf.message.entity.MessageReceiveEntity;
import jnpf.message.model.NoticePagination;
import jnpf.message.model.message.MessageInfoVO;
import jnpf.message.model.message.NoticeVO;
import jnpf.message.model.message.PaginationMessage;

import java.util.List;

/**
 * 消息实例
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface MessageService extends SuperService<MessageEntity> {

    /**
     * 列表（通知公告）
     *
     * @param pagination
     * @return
     */
    List<MessageEntity> getNoticeList(NoticePagination pagination);

    /**
     * 列表（通知公告）
     * 门户专用
     *
     * @return
     */
    List<MessageEntity> getDashboardNoticeList(List<String> typeList);


    /**
     * 获取全部数据
     *
     * @param pagination
     * @return
     */
    List<MessageReceiveEntity> getMessageList3(PaginationMessage pagination);

    /**
     * 列表（通知公告/系统消息/私信消息）
     *
     * @param pagination
     * @return
     */
    List<MessageReceiveEntity> getMessageList(Pagination pagination);

    /**
     * 信息
     *
     * @param id 主键值
     * @return
     */
    MessageEntity getInfo(String id);

    /**
     * 默认消息
     *
     * @param type 类别:1-通知公告/2-系统消息
     * @return
     */
    MessageEntity getInfoDefault(int type);

    /**
     * 删除
     *
     * @param entity 实体对象
     */
    void delete(MessageEntity entity);

    /**
     * 创建
     *
     * @param entity 实体对象
     */
    void create(MessageEntity entity);

    /**
     * 更新
     *
     * @param entity 实体对象
     */
    boolean update(String id, MessageEntity entity);

    /**
     * 消息已读（单条）
     *
     * @param messageId 消息主键
     */
    MessageReceiveEntity messageRead(String messageId);

    /**
     * 消息已读（全部）
     */
    void messageRead(List<String> idList);

    /**
     * 删除记录
     *
     * @param messageIds 消息Id
     */
    void deleteRecord(List<String> messageIds);

    /**
     * 获取消息未读数量
     *
     * @param userId 用户主键
     * @return
     */
    int getUnreadCount(String userId, Integer type);

    /**
     * 发送消息
     *
     * @param toUserIds 发送用户
     * @param title     标题
     * @param bodyText  内容
     */
    void sentMessage(List<String> toUserIds, String title, String bodyText);

    /**
     * 发送消息
     *
     * @param toUserIds 发送用户
     * @param title     标题
     * @param bodyText  内容
     */
    void sentMessage(List<String> toUserIds, String title, String bodyText, UserInfo userInfo, Integer source, Integer type);

    /**
     * 发送消息
     *
     * @param toUserIds   发送用户
     * @param title       标题
     * @param bodyText    内容
     * @param testMessage 是否为测试消息
     */
    void sentMessage(List<String> toUserIds, String title, String bodyText, UserInfo userInfo, Integer source, Integer type, boolean testMessage);

    /**
     * 退出在线的WebSocket 可选参数
     *
     * @param token  Token 精准退出用户
     * @param userId 退出用户的全部会话
     */
    void logoutWebsocketByToken(String token, String userId);

    /**
     * 通过过期时间刷新状态
     *
     * @return
     */
    Boolean updateEnabledMark();

    List<NoticeVO> getNoticeList(List<String> list);

    /**
     * 首页获取当前用户信息列表
     *
     * @return
     */
    List<MessageInfoVO> getUserMessageList();
}

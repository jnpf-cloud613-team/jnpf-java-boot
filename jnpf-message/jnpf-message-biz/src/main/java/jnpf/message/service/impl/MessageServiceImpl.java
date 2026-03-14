package jnpf.message.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.entity.DictionaryDataEntity;
import jnpf.base.mapper.DictionaryDataMapper;
import jnpf.base.service.SuperServiceImpl;
import jnpf.message.entity.MessageEntity;
import jnpf.message.entity.MessageMonitorEntity;
import jnpf.message.entity.MessageReceiveEntity;
import jnpf.message.mapper.MessageMapper;
import jnpf.message.mapper.MessageMonitorMapper;
import jnpf.message.mapper.MessagereceiveMapper;
import jnpf.message.model.NoticePagination;
import jnpf.message.model.message.MessageInfoVO;
import jnpf.message.model.message.NoticeVO;
import jnpf.message.model.message.PaginationMessage;
import jnpf.message.service.MessageService;
import jnpf.message.util.OnlineUserProvider;
import jnpf.message.util.PushMessageUtil;
import jnpf.permission.entity.UserEntity;
import jnpf.permission.service.UserService;
import jnpf.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息实例
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageServiceImpl extends SuperServiceImpl<MessageMapper, MessageEntity> implements MessageService {
    private final UserService userApi;
    private final DictionaryDataMapper dictionaryDataMapper;
    private final MessagereceiveMapper messagereceiveMapper;
    private final MessageMonitorMapper messageMonitorMapper;

    @Override
    public List<MessageEntity> getNoticeList(NoticePagination pagination) {
        return this.baseMapper.getNoticeList(pagination);
    }

    @Override
    public List<MessageEntity> getDashboardNoticeList(List<String> typeList) {
        List<MessageEntity> list = new ArrayList<>(16);
        // 判断哪些消息是自己接收的
        QueryWrapper<MessageReceiveEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageReceiveEntity::getUserId, UserProvider.getUser().getUserId());
        queryWrapper.lambda().eq(MessageReceiveEntity::getType, 1);
        List<MessageReceiveEntity> receiveEntities = messagereceiveMapper.selectList(queryWrapper);
        for (int i = 0; i < receiveEntities.size(); i++) {
            // 得到message
            MessageReceiveEntity messageReceiveEntity = receiveEntities.get(i);
            try {
                MessageEntity entity = JsonUtil.getJsonToBean(messageReceiveEntity.getBodyText(), MessageEntity.class);
                if (entity != null) {
                    if (StringUtil.isNotEmpty(entity.getId())) {
                        MessageEntity messageEntity = this.getInfo(entity.getId());
                        if (messageEntity != null &&
                                (typeList != null && !typeList.isEmpty() && typeList.contains(messageEntity.getCategory()) || typeList == null || typeList.isEmpty())
                                && Objects.equals(messageEntity.getEnabledMark(), 1)
                                && (entity.getExpirationTime() == null || entity.getExpirationTime().getTime() > System.currentTimeMillis())) {
                            messageEntity.setId(messageReceiveEntity.getId());
                            list.add(messageEntity);
                        }
                    } else {
                        entity.setId(messageReceiveEntity.getId());
                        list.add(entity);
                    }
                }
            } catch (Exception e) {
                MessageEntity messageEntity = JsonUtil.getJsonToBean(messageReceiveEntity, MessageEntity.class);
                list.add(messageEntity);
            }
            if (list.size() > 49) {
                break;
            }
        }
        list = list.stream().sorted(Comparator.comparing(MessageEntity::getLastModifyTime, Comparator.nullsFirst(Comparator.naturalOrder())).reversed()).collect(Collectors.toList());
        return list;
    }

    @Override
    public List<MessageReceiveEntity> getMessageList3(PaginationMessage pagination) {
        return messagereceiveMapper.getMessageColumnList(pagination);
    }

    @Override
    public List<MessageReceiveEntity> getMessageList(Pagination pagination) {
        PaginationMessage paginationMessage = BeanUtil.copyProperties(pagination, PaginationMessage.class);
        return this.getMessageList3(paginationMessage);
    }

    @Override
    public MessageEntity getInfo(String id) {
        return this.baseMapper.getInfo(id);
    }

    @Override
    public MessageEntity getInfoDefault(int type) {
        return this.baseMapper.getInfoDefault(type);
    }

    @Override
    @DSTransactional
    public void delete(MessageEntity entity) {
        entity.setEnabledMark(-1);
        this.updateById(entity);
        this.removeById(entity.getId());
    }

    @Override
    public void create(MessageEntity entity) {
        this.baseMapper.create(entity);
    }

    @Override
    public boolean update(String id, MessageEntity entity) {
        return this.baseMapper.update(id, entity);
    }

    @Override
    public MessageReceiveEntity messageRead(String messageId) {
        return messagereceiveMapper.messageRead(messageId);
    }

    @Override
    @DSTransactional
    public void messageRead(List<String> idList) {
        messagereceiveMapper.messageRead(idList);
    }

    @Override
    @DSTransactional
    public void deleteRecord(List<String> messageIds) {
        messagereceiveMapper.deleteRecord(messageIds);
    }

    @Override
    public int getUnreadCount(String userId, Integer type) {
        return this.baseMapper.getUnreadCount(userId, type);
    }

    @Override
    @DSTransactional
    public void sentMessage(List<String> toUserIds, String title, String bodyText) {
        messagereceiveMapper.sentMessage(toUserIds, title, bodyText);
    }

    @Override
    @DSTransactional
    public void sentMessage(List<String> toUserIds, String title, String bodyText, UserInfo userInfo, Integer source, Integer type) {
        sentMessage(toUserIds, title, bodyText, userInfo, source, type, false);
    }

    @Override
    @DSTransactional
    public void sentMessage(List<String> toUserIds, String title, String bodyText, UserInfo userInfo, Integer source, Integer type, boolean testMessage) {
        MessageReceiveEntity messageReceiveEntity = new MessageReceiveEntity();
        messageReceiveEntity.setTitle(title);
        messageReceiveEntity.setType(source);
        messageReceiveEntity.setFlowType(1);
        messageReceiveEntity.setBodyText(bodyText);
        messageReceiveEntity.setIsRead(0);
        Map<String, MessageReceiveEntity> map = new HashMap<>();
        for (String item : toUserIds) {
            MessageReceiveEntity messageReceiveEntitys = new MessageReceiveEntity();
            BeanUtils.copyProperties(messageReceiveEntity, messageReceiveEntitys);
            messageReceiveEntitys.setId(RandomUtil.uuId());
            messageReceiveEntitys.setUserId(item);
            messagereceiveMapper.insert(messageReceiveEntitys);
            map.put(messageReceiveEntitys.getUserId(), messageReceiveEntitys);
        }
        //消息监控写入
        MessageMonitorEntity monitorEntity = new MessageMonitorEntity();
        monitorEntity.setId(RandomUtil.uuId());
        monitorEntity.setTitle(title);
        monitorEntity.setMessageType(String.valueOf(type));
        monitorEntity.setMessageSource(String.valueOf(source));
        monitorEntity.setReceiveUser(JsonUtil.getObjectToString(toUserIds));
        monitorEntity.setSendTime(DateUtil.getNowDate());
        monitorEntity.setCreatorTime(DateUtil.getNowDate());
        monitorEntity.setCreatorUserId(userInfo.getUserId());
        messageMonitorMapper.insert(monitorEntity);
        PushMessageUtil.pushMessage(map, userInfo, source);
    }

    @Override
    public void logoutWebsocketByToken(String token, String userId) {
        if (StringUtil.isNotEmpty(token)) {
            OnlineUserProvider.removeWebSocketByToken(token.split(","));
        } else {
            OnlineUserProvider.removeWebSocketByUser(userId);
        }
    }

    @Override
    public Boolean updateEnabledMark() {
        return this.baseMapper.updateEnabledMark();
    }

    public List<NoticeVO> getNoticeList(List<String> list) {
        List<MessageEntity> dashboardNoticeList = this.getDashboardNoticeList(list);
        List<UserEntity> userList = userApi.getUserName(dashboardNoticeList.stream().map(MessageEntity::getCreatorUserId).collect(Collectors.toList()));
        List<DictionaryDataEntity> noticeType = dictionaryDataMapper.getListByTypeDataCode("NoticeType");
        dashboardNoticeList.forEach(t -> {
            // 转换创建人、发布人
            UserEntity user = userList.stream().filter(ul -> ul.getId().equals(t.getCreatorUserId())).findFirst().orElse(null);
            t.setCreatorUserId(user != null ? user.getRealName() + "/" + user.getAccount() : "");
            if (t.getEnabledMark() != null && t.getEnabledMark() != 0) {
                UserEntity entity = userApi.getInfo(t.getLastModifyUserId());
                t.setLastModifyUserId(entity != null ? entity.getRealName() + "/" + entity.getAccount() : "");
            }
            DictionaryDataEntity dictionaryDataEntity = noticeType.stream().filter(notice -> notice.getEnCode().equals(t.getCategory())).findFirst().orElse(new DictionaryDataEntity());
            t.setCategory(dictionaryDataEntity.getFullName());
        });
        List<NoticeVO> jsonToList = new ArrayList<>();
        dashboardNoticeList.forEach(t -> {
            NoticeVO vo = JsonUtil.getJsonToBean(t, NoticeVO.class);
            vo.setReleaseTime(t.getLastModifyTime() != null ? t.getLastModifyTime().getTime() : null);
            vo.setReleaseUser(t.getLastModifyUserId());
            vo.setCreatorUser(t.getCreatorUserId());
            jsonToList.add(vo);
        });
        return jsonToList;
    }

    @Override
    public List<MessageInfoVO> getUserMessageList() {
        List<MessageInfoVO> listVO = new ArrayList<>();
        PaginationMessage pagination = new PaginationMessage();
        pagination.setPageSize(5);
        pagination.setUserId(UserProvider.getUser().getUserId());
        List<MessageReceiveEntity> list = this.getMessageList3(pagination);
        List<UserEntity> userList = userApi.getUserName(list.stream().map(MessageReceiveEntity::getCreatorUserId).collect(Collectors.toList()));
        List<DictionaryDataEntity> msgSourceTypeList = dictionaryDataMapper.getListByTypeDataCode("msgSourceType");
        list.forEach(t -> {
            MessageInfoVO vo = JsonUtil.getJsonToBean(t, MessageInfoVO.class);
            UserEntity user = userList.stream().filter(ul -> ul.getId().equals(t.getCreatorUserId())).findFirst().orElse(null);
            if (user != null) {
                vo.setReleaseTime(t.getCreatorTime() != null ? t.getCreatorTime().getTime() : null);
                vo.setReleaseUser(user.getRealName() + "/" + user.getAccount());
                vo.setCreatorUser(user.getRealName() + "/" + user.getAccount());
            }
            if (t.getType() != null) {
                msgSourceTypeList.stream().filter(m -> t.getType().toString().equals(m.getEnCode())).findFirst()
                        .ifPresent(n -> vo.setTypeName(n.getFullName()));
            }
            listVO.add(vo);
        });
        return listVO;
    }

}

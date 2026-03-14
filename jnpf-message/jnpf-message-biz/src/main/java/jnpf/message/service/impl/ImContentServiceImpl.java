package jnpf.message.service.impl;

import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import jnpf.base.PageModel;
import jnpf.base.service.SuperServiceImpl;
import jnpf.message.entity.ImContentEntity;
import jnpf.message.entity.ImReplyEntity;
import jnpf.message.mapper.ImContentMapper;
import jnpf.message.model.ImReplySavaModel;
import jnpf.message.model.ImUnreadNumModel;
import jnpf.message.service.ImContentService;
import jnpf.message.service.ImReplyService;
import jnpf.util.JsonUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 聊天内容
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
@Service
@RequiredArgsConstructor
public class ImContentServiceImpl extends SuperServiceImpl<ImContentMapper, ImContentEntity> implements ImContentService {

    private final ImReplyService imReplyService;

    @Override
    public List<ImContentEntity> getMessageList(String sendUserId, String receiveUserId, PageModel pageModel) {
        return this.baseMapper.getMessageList(sendUserId, receiveUserId, pageModel);
    }

    @Override
    public List<ImUnreadNumModel> getUnreadList(String receiveUserId) {
        List<ImUnreadNumModel> list = this.baseMapper.getUnreadList(receiveUserId);
        List<ImUnreadNumModel> list1 = this.baseMapper.getUnreadLists(receiveUserId);
        for (ImUnreadNumModel item : list) {
            Optional<ImUnreadNumModel> first = list1.stream().filter(q -> q.getSendUserId().equals(item.getSendUserId())).findFirst();
            if (first.isPresent()) {
                ImUnreadNumModel defaultItem = first.get();
                item.setDefaultMessage(defaultItem.getDefaultMessage());
                item.setDefaultMessageType(defaultItem.getDefaultMessageType());
                item.setDefaultMessageTime(defaultItem.getDefaultMessageTime());
            }
        }
        return list;
    }

    @Override
    public int getUnreadCount(String sendUserId, String receiveUserId) {
        return this.baseMapper.getUnreadCount(sendUserId, receiveUserId);
    }

    @Override
    @DSTransactional
    public void sendMessage(String sendUserId, String receiveUserId, String message, String messageType) {
        if (StringUtil.isEmpty(sendUserId)) return;
        ImContentEntity entity = new ImContentEntity();
        entity.setId(RandomUtil.uuId());
        entity.setSendUserId(sendUserId);
        entity.setSendTime(new Date());
        entity.setReceiveUserId(receiveUserId);
        entity.setEnabledMark(0);
        entity.setContent(message);
        entity.setContentType(messageType);
        this.save(entity);

        //写入到会话表中
        ImReplySavaModel imReplySavaModel = new ImReplySavaModel(sendUserId, receiveUserId, entity.getSendTime());
        ImReplyEntity imReplyEntity = JsonUtil.getJsonToBean(imReplySavaModel, ImReplyEntity.class);
        imReplyService.saveImReply(imReplyEntity);
    }

    @Override
    public void readMessage(String sendUserId, String receiveUserId) {
        this.baseMapper.readMessage(sendUserId, receiveUserId);
    }

    @Override
    public boolean deleteChatRecord(String sendUserId, String receiveUserId) {
        return this.baseMapper.deleteChatRecord(sendUserId, receiveUserId);
    }
}

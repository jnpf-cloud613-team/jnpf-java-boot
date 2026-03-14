package jnpf.message.mapper;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.Pagination;
import jnpf.base.UserInfo;
import jnpf.base.mapper.SuperMapper;
import jnpf.message.entity.MessageReceiveEntity;
import jnpf.message.model.message.PaginationMessage;
import jnpf.message.util.PushMessageUtil;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;
import org.springframework.beans.BeanUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息接收
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface MessagereceiveMapper extends SuperMapper<MessageReceiveEntity> {

    default List<MessageReceiveEntity> getMessageList(Pagination pagination) {
        PaginationMessage paginationMessage = BeanUtil.copyProperties(pagination, PaginationMessage.class);
        return this.getMessageList3(paginationMessage);
    }

    default List<MessageReceiveEntity> getMessageList3(PaginationMessage pagination) {
        PaginationMessage paginationMessage = BeanUtil.copyProperties(pagination, PaginationMessage.class);
        List<MessageReceiveEntity> messageColumnList = getMessageColumnList(paginationMessage,
                MessageReceiveEntity::getId,
                MessageReceiveEntity::getUserId,
                MessageReceiveEntity::getType,
                MessageReceiveEntity::getTitle,
                MessageReceiveEntity::getFlowType,
                MessageReceiveEntity::getIsRead,
                MessageReceiveEntity::getSortCode,
                MessageReceiveEntity::getCreatorTime,
                MessageReceiveEntity::getCreatorUserId);
        return pagination.setData(messageColumnList, paginationMessage.getTotal());
    }

    default List<MessageReceiveEntity> getMessageColumnList(PaginationMessage pagination, SFunction<MessageReceiveEntity, ?>... columns) {
        QueryWrapper<MessageReceiveEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(MessageReceiveEntity::getId);
        if (StringUtil.isNotEmpty(pagination.getUserId())) {
            queryWrapper.lambda().eq(MessageReceiveEntity::getUserId, pagination.getUserId());
        } else {
            queryWrapper.lambda().eq(MessageReceiveEntity::getUserId, UserProvider.getLoginUserId());
        }
        if (pagination.getType() != null) {
            queryWrapper.lambda().eq(MessageReceiveEntity::getType, pagination.getType());
        }

        if (pagination.getNotType() != null) {
            queryWrapper.lambda().ne(MessageReceiveEntity::getType, pagination.getNotType());
        }

        if (pagination.getIsRead() != null) {
            queryWrapper.lambda().eq(MessageReceiveEntity::getIsRead, pagination.getIsRead());
        }
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().like(MessageReceiveEntity::getTitle, pagination.getKeyword());
        }
        queryWrapper.lambda().orderByDesc(MessageReceiveEntity::getCreatorTime);
        Page<MessageReceiveEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<MessageReceiveEntity> userIPage = this.selectPage(page, queryWrapper);

        List<String> ids = userIPage.getRecords().stream().map(MessageReceiveEntity::getId).collect(Collectors.toList());
        List<MessageReceiveEntity> list = new ArrayList<>();
        if (CollUtil.isNotEmpty(ids)) {
            QueryWrapper<MessageReceiveEntity> wrapper = new QueryWrapper<>();
            if (columns != null && columns.length > 0) {
                wrapper.lambda().select(columns);
            }
            wrapper.lambda().in(MessageReceiveEntity::getId, ids);
            wrapper.lambda().orderByDesc(MessageReceiveEntity::getCreatorTime);
            list = this.selectList(wrapper);
        }
        return pagination.setData(list, page.getTotal());
    }

    default MessageReceiveEntity messageRead(String messageId) {
        String userId = UserProvider.getUser().getUserId();
        QueryWrapper<MessageReceiveEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageReceiveEntity::getUserId, userId).eq(MessageReceiveEntity::getId, messageId);
        MessageReceiveEntity entity = this.selectOne(queryWrapper);
        if (entity != null) {
            entity.setIsRead(1);
            this.updateById(entity);
        }
        return entity;
    }

    default void messageRead(List<String> idList) {
        String userId = UserProvider.getUser().getUserId();
        QueryWrapper<MessageReceiveEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(MessageReceiveEntity::getId)
                .eq(MessageReceiveEntity::getUserId, userId).eq(MessageReceiveEntity::getIsRead, 0);
        List<MessageReceiveEntity> entitys = this.selectList(queryWrapper);
        if (!entitys.isEmpty()) {
            for (MessageReceiveEntity entity : entitys) {
                entity.setIsRead(1);
                this.updateById(entity);
            }
        }
    }

    default void deleteRecord(List<String> messageIds) {
        // 删除已读表
        QueryWrapper<MessageReceiveEntity> queryWrapper = new QueryWrapper<>();
        if (!messageIds.isEmpty()) {
            queryWrapper.lambda().in(MessageReceiveEntity::getId, messageIds);
        }
        // 通过id删除无需判断接收人
        queryWrapper.lambda().eq(MessageReceiveEntity::getUserId, UserProvider.getLoginUserId());
        this.deleteByIds(selectList(queryWrapper));
    }

    default void sentMessage(List<String> toUserIds, String title) {
        this.sentMessage(toUserIds, title, null);
    }

    default void sentMessage(List<String> toUserIds, String title, String bodyText) {
        UserInfo userInfo = UserProvider.getUser();

        MessageReceiveEntity messageReceiveEntity = new MessageReceiveEntity();
        messageReceiveEntity.setTitle(title);
        messageReceiveEntity.setType(2);
        messageReceiveEntity.setFlowType(1);
        messageReceiveEntity.setIsRead(0);
        Map<String, MessageReceiveEntity> map = new HashMap<>();
        for (String item : toUserIds) {
            MessageReceiveEntity messageReceiveEntitys = new MessageReceiveEntity();
            BeanUtils.copyProperties(messageReceiveEntity, messageReceiveEntitys);
            messageReceiveEntitys.setId(RandomUtil.uuId());
            messageReceiveEntitys.setUserId(item);
            messageReceiveEntitys.setBodyText(bodyText);
            this.insert(messageReceiveEntitys);
            map.put(messageReceiveEntitys.getUserId(), messageReceiveEntitys);
        }
        //消息推送 - PC端
        PushMessageUtil.pushMessage(map, userInfo, 2);
    }

    default void sentMessage(List<String> toUserIds, String title, String bodyText, Map<String, String> contentMsg, UserInfo userInfo) {
        MessageReceiveEntity messageReceiveEntity = new MessageReceiveEntity();
        messageReceiveEntity.setTitle(title);
        messageReceiveEntity.setType(2);
        messageReceiveEntity.setFlowType(1);
        messageReceiveEntity.setIsRead(0);
        Map<String, MessageReceiveEntity> map = new HashMap<>();
        for (String item : toUserIds) {
            MessageReceiveEntity messageReceiveEntitys = new MessageReceiveEntity();
            BeanUtils.copyProperties(messageReceiveEntity, messageReceiveEntitys);
            messageReceiveEntitys.setId(RandomUtil.uuId());
            messageReceiveEntitys.setUserId(item);
            String msg = contentMsg.get(item) != null ? contentMsg.get(item) : "{}";
            messageReceiveEntitys.setBodyText(msg);
            messageReceiveEntitys.setCreatorUserId(userInfo.getUserId());
            messageReceiveEntitys.setCreatorTime(new Date());
            this.insert(messageReceiveEntitys);
            map.put(messageReceiveEntitys.getUserId(), messageReceiveEntitys);
        }

        //消息推送 - PC端
        PushMessageUtil.pushMessage(map, userInfo, 2);
    }
}

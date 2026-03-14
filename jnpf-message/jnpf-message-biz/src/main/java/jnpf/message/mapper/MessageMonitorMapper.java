package jnpf.message.mapper;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.mapper.SuperMapper;
import jnpf.message.entity.MessageMonitorEntity;
import jnpf.message.model.messagemonitor.MessageMonitorPagination;
import jnpf.util.StringUtil;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

/**
 * 消息监控
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-22
 */
public interface MessageMonitorMapper extends SuperMapper<MessageMonitorEntity> {

    default List<MessageMonitorEntity> getList(MessageMonitorPagination messageMonitorPagination) {
        return getTypeList(messageMonitorPagination, messageMonitorPagination.getDataType());
    }

    default List<MessageMonitorEntity> getTypeList(MessageMonitorPagination messageMonitorPagination, String dataType) {
        QueryWrapper<MessageMonitorEntity> messageMonitorQueryWrapper = new QueryWrapper<>();
        //关键字
        if (ObjectUtil.isNotEmpty(messageMonitorPagination.getKeyword())) {
            messageMonitorQueryWrapper.lambda().and(t -> t.like(MessageMonitorEntity::getTitle, messageMonitorPagination.getKeyword()));
        }
        //消息类型
        if (ObjectUtil.isNotEmpty(messageMonitorPagination.getMessageType())) {
            messageMonitorQueryWrapper.lambda().eq(MessageMonitorEntity::getMessageType, messageMonitorPagination.getMessageType());
        }
        //发送时间
        if (ObjectUtil.isNotEmpty(messageMonitorPagination.getStartTime()) && ObjectUtil.isNotEmpty(messageMonitorPagination.getEndTime())) {
            messageMonitorQueryWrapper.lambda().ge(MessageMonitorEntity::getSendTime, new Date(messageMonitorPagination.getStartTime()))
                    .le(MessageMonitorEntity::getSendTime, new Date(messageMonitorPagination.getEndTime()));

        }
        //消息来源
        if (ObjectUtil.isNotEmpty(messageMonitorPagination.getMessageSource())) {
            messageMonitorQueryWrapper.lambda().eq(MessageMonitorEntity::getMessageSource, messageMonitorPagination.getMessageSource());
        }
        //排序
        if (StringUtil.isEmpty(messageMonitorPagination.getSidx())) {
            messageMonitorQueryWrapper.lambda().orderByDesc(MessageMonitorEntity::getSendTime);
        } else {
            try {
                String sidx = messageMonitorPagination.getSidx();
                String sortOrder = messageMonitorPagination.getSort().toLowerCase();
                Field declaredField = MessageMonitorEntity.class.getDeclaredField(sidx);
                TableField tableField = declaredField.getAnnotation(TableField.class);
                if (tableField != null && !tableField.value().isEmpty()) {
                    String columnName = tableField.value();
                    if ("asc".equals(sortOrder)) {
                        messageMonitorQueryWrapper.orderByAsc(columnName);
                    } else {
                        messageMonitorQueryWrapper.orderByDesc(columnName);
                    }
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        if (!"1".equals(dataType)) {
            Page<MessageMonitorEntity> page = new Page<>(messageMonitorPagination.getCurrentPage(), messageMonitorPagination.getPageSize());
            IPage<MessageMonitorEntity> userIPage = this.selectPage(page, messageMonitorQueryWrapper);
            return messageMonitorPagination.setData(userIPage.getRecords(), userIPage.getTotal());
        } else {
            return this.selectList(messageMonitorQueryWrapper);
        }
    }

    default void create(MessageMonitorEntity entity) {
        this.insert(entity);
    }

    default MessageMonitorEntity getInfo(String id) {
        QueryWrapper<MessageMonitorEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageMonitorEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void emptyMonitor() {
        this.deleteByIds(this.selectList(new QueryWrapper<>()));
    }

    default void delete(String[] ids) {
        if (ids.length > 0) {
            QueryWrapper<MessageMonitorEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().in(MessageMonitorEntity::getId, ids);
            this.deleteByIds(selectList(queryWrapper));
        }
    }
}

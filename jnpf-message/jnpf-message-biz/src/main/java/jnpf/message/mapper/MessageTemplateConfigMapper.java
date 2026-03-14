package jnpf.message.mapper;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.mapper.SuperMapper;
import jnpf.message.entity.MessageTemplateConfigEntity;
import jnpf.message.model.messagetemplateconfig.MessageTemplateConfigPagination;
import jnpf.util.StringUtil;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 消息模板（新）
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-18
 */
public interface MessageTemplateConfigMapper extends SuperMapper<MessageTemplateConfigEntity> {

    default List<MessageTemplateConfigEntity> getList(MessageTemplateConfigPagination pagination) {
        return getTypeList(pagination, pagination.getDataType());
    }

    default List<MessageTemplateConfigEntity> getTypeList(MessageTemplateConfigPagination pagination, String dataType) {
        QueryWrapper<MessageTemplateConfigEntity> messageTemplateNewQueryWrapper = new QueryWrapper<>();
        //关键字
        if (ObjectUtil.isNotEmpty(pagination.getKeyword())) {
            messageTemplateNewQueryWrapper.lambda().and(t -> t.like(MessageTemplateConfigEntity::getEnCode, pagination.getKeyword()).
                    or().like(MessageTemplateConfigEntity::getFullName, pagination.getKeyword()));
        }
        //模板类型
        if (ObjectUtil.isNotEmpty(pagination.getTemplateType())) {
            messageTemplateNewQueryWrapper.lambda().eq(MessageTemplateConfigEntity::getTemplateType, pagination.getTemplateType());
        }
        //消息类型
        if (ObjectUtil.isNotEmpty(pagination.getMessageType())) {
            messageTemplateNewQueryWrapper.lambda().eq(MessageTemplateConfigEntity::getMessageType, pagination.getMessageType());
        }
        //消息来源
        if (ObjectUtil.isNotEmpty(pagination.getMessageSource())) {
            messageTemplateNewQueryWrapper.lambda().eq(MessageTemplateConfigEntity::getMessageSource, pagination.getMessageSource());
        }
        //状态
        if (ObjectUtil.isNotEmpty(pagination.getEnabledMark())) {
            int enabledMark = Integer.parseInt(pagination.getEnabledMark());
            messageTemplateNewQueryWrapper.lambda().eq(MessageTemplateConfigEntity::getEnabledMark, enabledMark);
        }

        //排序
        if (StringUtil.isEmpty(pagination.getSidx())) {
            messageTemplateNewQueryWrapper.lambda().orderByAsc(MessageTemplateConfigEntity::getSortCode).orderByDesc(MessageTemplateConfigEntity::getCreatorTime).orderByDesc(MessageTemplateConfigEntity::getLastModifyTime);
        } else {
            try {
                String sidx = pagination.getSidx();
                String sortOrder = pagination.getSort().toLowerCase();
                Field declaredField = MessageTemplateConfigEntity.class.getDeclaredField(sidx);
                TableField tableField = declaredField.getAnnotation(TableField.class);
                if (tableField != null && !tableField.value().isEmpty()) {
                    String columnName = tableField.value();
                    if ("asc".equals(sortOrder)) {
                        messageTemplateNewQueryWrapper.orderByAsc(columnName);
                    } else {
                        messageTemplateNewQueryWrapper.orderByDesc(columnName);
                    }
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        if (!"1".equals(dataType)) {
            Page<MessageTemplateConfigEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
            IPage<MessageTemplateConfigEntity> userIPage = this.selectPage(page, messageTemplateNewQueryWrapper);
            return pagination.setData(userIPage.getRecords(), userIPage.getTotal());
        } else {
            return this.selectList(messageTemplateNewQueryWrapper);
        }
    }


    default MessageTemplateConfigEntity getInfo(String id) {
        QueryWrapper<MessageTemplateConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageTemplateConfigEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default MessageTemplateConfigEntity getInfoByEnCode(String enCode, String messageType) {
        QueryWrapper<MessageTemplateConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageTemplateConfigEntity::getEnCode, enCode);
        queryWrapper.lambda().eq(MessageTemplateConfigEntity::getMessageType, messageType);
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<MessageTemplateConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageTemplateConfigEntity::getFullName, fullName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(MessageTemplateConfigEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String enCode, String id) {
        QueryWrapper<MessageTemplateConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageTemplateConfigEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(MessageTemplateConfigEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }
}

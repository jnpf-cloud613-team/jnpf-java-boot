package jnpf.message.mapper;


import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jnpf.base.mapper.SuperMapper;
import jnpf.message.entity.SendMessageConfigEntity;
import jnpf.message.model.sendmessageconfig.SendMessageConfigPagination;
import jnpf.util.StringUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息发送配置
 * 版本： V3.2.0
 * 版权： 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * 作者： JNPF开发平台组
 * 日期： 2022-08-19
 */
public interface SendMessageConfigMapper extends SuperMapper<SendMessageConfigEntity> {

    default List<SendMessageConfigEntity> getList(SendMessageConfigPagination sendMessageConfigPagination, String dataType) {
        QueryWrapper<SendMessageConfigEntity> sendMessageConfigQueryWrapper = new QueryWrapper<>();
        //关键字
        if (ObjectUtil.isNotEmpty(sendMessageConfigPagination.getKeyword())) {
            sendMessageConfigQueryWrapper.lambda().and(t -> t.like(SendMessageConfigEntity::getEnCode, sendMessageConfigPagination.getKeyword()).
                    or().like(SendMessageConfigEntity::getFullName, sendMessageConfigPagination.getKeyword()));
        }
        //模板类型
        if (ObjectUtil.isNotEmpty(sendMessageConfigPagination.getTemplateType())) {
            sendMessageConfigQueryWrapper.lambda().eq(SendMessageConfigEntity::getTemplateType, sendMessageConfigPagination.getTemplateType());
        }
        //状态
        if (ObjectUtil.isNotEmpty(sendMessageConfigPagination.getEnabledMark())) {
            int enabledMark = Integer.parseInt(sendMessageConfigPagination.getEnabledMark());
            sendMessageConfigQueryWrapper.lambda().eq(SendMessageConfigEntity::getEnabledMark, enabledMark);
        }
        //消息来源
        if (ObjectUtil.isNotEmpty(sendMessageConfigPagination.getMessageSource())) {
            List<String> split = Arrays.asList(sendMessageConfigPagination.getMessageSource().split(","));
            sendMessageConfigQueryWrapper.lambda().in(SendMessageConfigEntity::getMessageSource, split);
        }

        //排序
        if (StringUtil.isEmpty(sendMessageConfigPagination.getSidx())) {
            sendMessageConfigQueryWrapper.lambda().orderByAsc(SendMessageConfigEntity::getSortCode).orderByDesc(SendMessageConfigEntity::getCreatorTime).orderByDesc(SendMessageConfigEntity::getLastModifyTime);
        } else {
            try {
                String sidx = sendMessageConfigPagination.getSidx();
                String sortOrder = sendMessageConfigPagination.getSort().toLowerCase();
                Field declaredField = SendMessageConfigEntity.class.getDeclaredField(sidx);
                TableField tableField = declaredField.getAnnotation(TableField.class);
                if (tableField != null && !tableField.value().isEmpty()) {
                    String columnName = tableField.value();
                    if ("asc".equals(sortOrder)) {
                        sendMessageConfigQueryWrapper.orderByAsc(columnName);
                    } else {
                        sendMessageConfigQueryWrapper.orderByDesc(columnName);
                    }
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        if (!"1".equals(dataType)) {
            Page<SendMessageConfigEntity> page = new Page<>(sendMessageConfigPagination.getCurrentPage(), sendMessageConfigPagination.getPageSize());
            IPage<SendMessageConfigEntity> userIPage = this.selectPage(page, sendMessageConfigQueryWrapper);
            return sendMessageConfigPagination.setData(userIPage.getRecords(), userIPage.getTotal());
        } else {
            return this.selectList(sendMessageConfigQueryWrapper);
        }
    }

    default List<SendMessageConfigEntity> getSelectorList(SendMessageConfigPagination sendMessageConfigPagination) {
        QueryWrapper<SendMessageConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SendMessageConfigEntity::getMessageSource, 5).eq(SendMessageConfigEntity::getEnabledMark, 1);
        queryWrapper.lambda().eq(SendMessageConfigEntity::getTemplateType, 0);
        Page<SendMessageConfigEntity> page = new Page<>(sendMessageConfigPagination.getCurrentPage(), sendMessageConfigPagination.getPageSize());
        IPage<SendMessageConfigEntity> userIPage = this.selectPage(page, queryWrapper);
        return sendMessageConfigPagination.setData(userIPage.getRecords(), userIPage.getTotal());
    }


    default SendMessageConfigEntity getInfo(String id) {
        QueryWrapper<SendMessageConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SendMessageConfigEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default SendMessageConfigEntity getInfoByEnCode(String enCode) {
        QueryWrapper<SendMessageConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SendMessageConfigEntity::getEnCode, enCode);
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<SendMessageConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SendMessageConfigEntity::getFullName, fullName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(SendMessageConfigEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String enCode, String id) {
        QueryWrapper<SendMessageConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SendMessageConfigEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(SendMessageConfigEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default List<SendMessageConfigEntity> getList(List<String> idList) {
        QueryWrapper<SendMessageConfigEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(SendMessageConfigEntity::getId, idList);
        return this.selectList(queryWrapper);
    }


    default List<String> getIdList(String usedId) {
        List<String> idList = new ArrayList<>();
        QueryWrapper<SendMessageConfigEntity> queryWrapper = new QueryWrapper<>();
        if (this.selectList(queryWrapper) != null && !this.selectList(queryWrapper).isEmpty()) {
            idList = this.selectList(queryWrapper).stream().distinct().map(t -> t.getId()).collect(Collectors.toList());
        }
        return idList;
    }

    default boolean idUsed(String id) {
        SendMessageConfigEntity entity = this.getInfo(id);
        return entity != null;
    }
}

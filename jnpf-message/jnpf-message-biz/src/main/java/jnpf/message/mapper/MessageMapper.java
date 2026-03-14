package jnpf.message.mapper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.mapper.SuperMapper;
import jnpf.message.entity.MessageEntity;
import jnpf.message.model.NoticePagination;
import jnpf.util.RandomUtil;
import jnpf.util.UserProvider;
import jnpf.util.XSSEscape;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 消息实例
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司（https://www.jnpfsoft.com）
 * @date 2019年9月27日 上午9:18
 */
public interface MessageMapper extends SuperMapper<MessageEntity> {

    int getUnreadCount(@Param("userId") String userId, @Param("type") Integer type);

    default List<MessageEntity> getNoticeList(NoticePagination pagination) {
        // 定义变量判断是否需要使用修改时间倒序
        QueryWrapper<MessageEntity> queryWrapper = new QueryWrapper<>();
        //关键词（消息标题）
        if (!StringUtils.isEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().like(MessageEntity::getTitle, pagination.getKeyword());
        }
        // 类型
        if (CollUtil.isNotEmpty(pagination.getType())) {
            queryWrapper.lambda().in(MessageEntity::getCategory, pagination.getType());
        }
        // 状态
        if (CollUtil.isNotEmpty(pagination.getEnabledMark())) {
            queryWrapper.lambda().in(MessageEntity::getEnabledMark, pagination.getEnabledMark());
        } else {
            queryWrapper.lambda().and(t -> t.ne(MessageEntity::getEnabledMark, 3)
                    .or().isNull(MessageEntity::getEnabledMark));
            queryWrapper.lambda().ne(MessageEntity::getEnabledMark, -1);
        }
        // 发布人
        if (CollUtil.isNotEmpty(pagination.getReleaseUser())) {
            queryWrapper.lambda().in(MessageEntity::getLastModifyUserId, pagination.getReleaseUser());
        }
        // 发布时间
        if (CollUtil.isNotEmpty(pagination.getReleaseTime())) {
            queryWrapper.lambda().between(MessageEntity::getLastModifyTime, new Date(pagination.getReleaseTime().get(0)), new Date(pagination.getReleaseTime().get(1)));
        }
        // 失效时间
        if (CollUtil.isNotEmpty(pagination.getExpirationTime())) {
            queryWrapper.lambda().between(MessageEntity::getExpirationTime, new Date(pagination.getExpirationTime().get(0)), new Date(pagination.getExpirationTime().get(1)));
        }
        // 创建人
        if (CollUtil.isNotEmpty(pagination.getCreatorUser())) {
            queryWrapper.lambda().in(MessageEntity::getCreatorUserId, pagination.getCreatorUser());
        }
        // 创建时间
        if (CollUtil.isNotEmpty(pagination.getCreatorTime())) {
            queryWrapper.lambda().between(MessageEntity::getCreatorTime, new Date(pagination.getCreatorTime().get(0)), new Date(pagination.getCreatorTime().get(1)));
        }
        //默认排序
        queryWrapper.lambda().orderByAsc(MessageEntity::getEnabledMark).orderByDesc(MessageEntity::getLastModifyTime).orderByDesc(MessageEntity::getCreatorTime);
        queryWrapper.lambda().select(MessageEntity::getId, MessageEntity::getCreatorUserId, MessageEntity::getEnabledMark,
                MessageEntity::getLastModifyTime, MessageEntity::getTitle, MessageEntity::getCreatorTime,
                MessageEntity::getLastModifyUserId, MessageEntity::getExpirationTime, MessageEntity::getCategory);
        Page<MessageEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<MessageEntity> userIPage = this.selectPage(page, queryWrapper);
        return pagination.setData(userIPage.getRecords(), page.getTotal());
    }

    default List<MessageEntity> getNoticeList() {
        QueryWrapper<MessageEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageEntity::getEnabledMark, 1);
        queryWrapper.lambda().orderByAsc(MessageEntity::getSortCode);
        return this.selectList(queryWrapper);
    }

    default MessageEntity getInfo(String id) {
        QueryWrapper<MessageEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default MessageEntity getInfoDefault(int type) {
        QueryWrapper<MessageEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageEntity::getEnabledMark, 1);
        if (type == 1) {
            queryWrapper.lambda().orderByDesc(MessageEntity::getCreatorTime);
        } else {
            queryWrapper.lambda().orderByDesc(MessageEntity::getLastModifyTime);
        }
        // 只查询id
        queryWrapper.lambda().select(MessageEntity::getId, MessageEntity::getTitle, MessageEntity::getCreatorTime);
        List<MessageEntity> list = this.selectPage(new Page<>(1, 1, false), queryWrapper).getRecords();
        MessageEntity entity = new MessageEntity();
        if (!list.isEmpty()) {
            entity = list.get(0);
        }
        return entity;
    }

    default void create(MessageEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setBodyText(XSSEscape.escapeImgOnlyBase64(entity.getBodyText()));
        entity.setEnabledMark(0);
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        this.insert(entity);
    }

    default boolean update(String id, MessageEntity entity) {
        entity.setId(id);
        entity.setBodyText(XSSEscape.escapeImgOnlyBase64(entity.getBodyText()));
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyUserId(null);
        entity.setLastModifyTime(null);
        return SqlHelper.retBool(this.updateById(entity));
    }

    default Boolean updateEnabledMark() {
        QueryWrapper<MessageEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().and(t -> t.eq(MessageEntity::getEnabledMark, 1).lt(MessageEntity::getExpirationTime, new Date()));
        List<MessageEntity> list = this.selectList(queryWrapper);
        list.forEach(t -> {
            t.setEnabledMark(2);
            this.updateById(t);
        });
        return true;
    }
}

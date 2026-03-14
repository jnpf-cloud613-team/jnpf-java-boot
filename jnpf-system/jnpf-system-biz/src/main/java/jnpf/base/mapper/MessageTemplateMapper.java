package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.Pagination;
import jnpf.base.entity.MessageTemplateEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.Date;
import java.util.List;

/**
 * 消息模板
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2021年12月8日17:40:37
 */
public interface MessageTemplateMapper extends SuperMapper<MessageTemplateEntity> {

    default List<MessageTemplateEntity> getList() {
        QueryWrapper<MessageTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageTemplateEntity::getEnabledMark, 1);
        queryWrapper.lambda().orderByDesc(MessageTemplateEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<MessageTemplateEntity> getList(Pagination pagination, Boolean filter) {
        QueryWrapper<MessageTemplateEntity> queryWrapper = new QueryWrapper<>();
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(MessageTemplateEntity::getFullName, pagination.getKeyword())
                            .or().like(MessageTemplateEntity::getTitle, pagination.getKeyword())
                            .or().like(MessageTemplateEntity::getEnCode, pagination.getKeyword())
            );
        }
        if (Boolean.TRUE.equals(filter)) {
            queryWrapper.lambda().eq(MessageTemplateEntity::getEnabledMark, 1);
        }
        queryWrapper.lambda().orderByDesc(MessageTemplateEntity::getCreatorTime);
        Page<MessageTemplateEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<MessageTemplateEntity> userPage = this.selectPage(page, queryWrapper);
        return pagination.setData(userPage.getRecords(), page.getTotal());
    }

    default MessageTemplateEntity getInfo(String id) {
        QueryWrapper<MessageTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageTemplateEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void create(MessageTemplateEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        this.insert(entity);
    }


    default boolean update(String id, MessageTemplateEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        return SqlHelper.retBool(this.updateById(entity));
    }

    default boolean isExistByFullName(String fullName, String id) {
        QueryWrapper<MessageTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageTemplateEntity::getFullName, fullName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(MessageTemplateEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String enCode, String id) {
        QueryWrapper<MessageTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MessageTemplateEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(MessageTemplateEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }
}





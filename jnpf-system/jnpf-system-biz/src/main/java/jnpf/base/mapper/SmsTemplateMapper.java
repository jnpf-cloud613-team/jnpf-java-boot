package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.Pagination;
import jnpf.base.entity.SmsTemplateEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.Date;
import java.util.List;

/**
 * @author Administrator
 * @description 针对表【base_sms_template】的数据库操作Mapper
 * @createDate 2021-12-09 10:12:52
 * @Entity generator.domain.SmsTemplate
 */
public interface SmsTemplateMapper extends SuperMapper<SmsTemplateEntity> {

    default List<SmsTemplateEntity> getList(String keyword) {
        QueryWrapper<SmsTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SmsTemplateEntity::getEnabledMark, 1);
        if (!StringUtil.isEmpty(keyword)) {
            queryWrapper.lambda().and(
                    t -> t.like(SmsTemplateEntity::getTemplateId, keyword)
                            .or().like(SmsTemplateEntity::getFullName, keyword)
                            .or().like(SmsTemplateEntity::getEnCode, keyword)
            );
        }
        queryWrapper.lambda().orderByDesc(SmsTemplateEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<SmsTemplateEntity> getList(Pagination pagination) {
        QueryWrapper<SmsTemplateEntity> queryWrapper = new QueryWrapper<>();
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(SmsTemplateEntity::getTemplateId, pagination.getKeyword())
                            .or().like(SmsTemplateEntity::getFullName, pagination.getKeyword())
                            .or().like(SmsTemplateEntity::getEnCode, pagination.getKeyword())
            );
        }
        queryWrapper.lambda().orderByDesc(SmsTemplateEntity::getCreatorTime);
        Page<SmsTemplateEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<SmsTemplateEntity> userPage = this.selectPage(page, queryWrapper);
        return pagination.setData(userPage.getRecords(), page.getTotal());
    }

    default SmsTemplateEntity getInfo(String id) {
        QueryWrapper<SmsTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SmsTemplateEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default void create(SmsTemplateEntity entity) {
        entity.setId(RandomUtil.uuId());
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        this.insert(entity);
    }

    default boolean update(String id, SmsTemplateEntity entity) {
        entity.setId(id);
        entity.setLastModifyTime(new Date());
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        return SqlHelper.retBool(this.updateById(entity));
    }

    default boolean isExistByTemplateName(String templateName, String id) {
        QueryWrapper<SmsTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SmsTemplateEntity::getFullName, templateName);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(SmsTemplateEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String enCode, String id) {
        QueryWrapper<SmsTemplateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(SmsTemplateEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(SmsTemplateEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }
}





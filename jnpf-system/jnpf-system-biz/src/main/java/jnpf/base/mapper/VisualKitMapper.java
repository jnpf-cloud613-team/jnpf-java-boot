package jnpf.base.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import jnpf.base.entity.VisualKitEntity;
import jnpf.base.model.visualkit.KitPagination;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.Date;
import java.util.List;

/**
 * 表单套件
 *
 * @author JNPF开发平台组
 * @version v5.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2024/8/22 11:03:36
 */
public interface VisualKitMapper extends SuperMapper<VisualKitEntity> {

    default List<VisualKitEntity> getList(KitPagination pagination) {
        // 定义变量判断是否需要使用修改时间倒序
        boolean flag = false;
        QueryWrapper<VisualKitEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().select(VisualKitEntity::getId, VisualKitEntity::getFullName, VisualKitEntity::getEnCode, VisualKitEntity::getCategory,
                VisualKitEntity::getIcon, VisualKitEntity::getCreatorUserId, VisualKitEntity::getCreatorTime, VisualKitEntity::getLastModifyTime,
                VisualKitEntity::getSortCode, VisualKitEntity::getEnabledMark);
        if (StringUtil.isNotEmpty(pagination.getKeyword())) {
            flag = true;
            queryWrapper.lambda().and(t -> t.like(VisualKitEntity::getFullName, pagination.getKeyword())
                    .or().like(VisualKitEntity::getEnCode, pagination.getKeyword())
            );
        }

        if (StringUtil.isNotEmpty(pagination.getCategory())) {
            flag = true;
            queryWrapper.lambda().eq(VisualKitEntity::getCategory, pagination.getCategory());
        }

        if (pagination.getEnabledMark() != null) {
            flag = true;
            queryWrapper.lambda().eq(VisualKitEntity::getEnabledMark, pagination.getEnabledMark());
        }

        //排序
        queryWrapper.lambda().orderByAsc(VisualKitEntity::getSortCode)
                .orderByDesc(VisualKitEntity::getCreatorTime);
        if (flag) {
            queryWrapper.lambda().orderByDesc(VisualKitEntity::getLastModifyTime);
        }
        Page<VisualKitEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<VisualKitEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default Boolean isExistByEnCode(String enCode, String id) {
        if (StringUtil.isEmpty(enCode)) return false;
        QueryWrapper<VisualKitEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(VisualKitEntity::getEnCode, enCode);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(VisualKitEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default void create(VisualKitEntity entity) {
        if (StringUtil.isEmpty(entity.getId())) {
            entity.setId(RandomUtil.uuId());
        }
        entity.setCreatorUserId(UserProvider.getUser().getUserId());
        entity.setCreatorTime(new Date());
        this.insert(entity);
    }

    default boolean update(String id, VisualKitEntity entity) {
        entity.setId(id);
        entity.setLastModifyUserId(UserProvider.getUser().getUserId());
        entity.setLastModifyTime(new Date());
        return SqlHelper.retBool(this.updateById(entity));
    }
}

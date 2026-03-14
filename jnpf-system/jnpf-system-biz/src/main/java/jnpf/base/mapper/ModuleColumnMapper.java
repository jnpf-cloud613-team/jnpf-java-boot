package jnpf.base.mapper;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import jnpf.base.Pagination;
import jnpf.base.entity.ModuleColumnEntity;
import jnpf.util.RandomUtil;
import jnpf.util.StringUtil;
import jnpf.util.UserProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 列表权限
 *
 * @author JNPF开发平台组
 * @version V3.1.0
 * @copyright 引迈信息技术有限公司
 * @date 2019年9月27日 上午9:18
 */
public interface ModuleColumnMapper extends SuperMapper<ModuleColumnEntity> {

    default List<ModuleColumnEntity> getList() {
        QueryWrapper<ModuleColumnEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleColumnEntity::getEnabledMark, 1);
        // 排序
        queryWrapper.lambda().orderByAsc(ModuleColumnEntity::getSortCode)
                .orderByDesc(ModuleColumnEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleColumnEntity> getEnabledMarkList(String enabledMark) {
        QueryWrapper<ModuleColumnEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleColumnEntity::getEnabledMark, enabledMark);
        // 排序
        queryWrapper.lambda().orderByAsc(ModuleColumnEntity::getSortCode)
                .orderByDesc(ModuleColumnEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleColumnEntity> getList(String moduleId, Pagination pagination) {
        QueryWrapper<ModuleColumnEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleColumnEntity::getModuleId, moduleId);
        if (!StringUtil.isEmpty(pagination.getKeyword())) {
            queryWrapper.lambda().and(
                    t -> t.like(ModuleColumnEntity::getEnCode, pagination.getKeyword()).or().like(ModuleColumnEntity::getFullName, pagination.getKeyword())
            );
        }
        // 排序
        queryWrapper.lambda().orderByAsc(ModuleColumnEntity::getSortCode)
                .orderByDesc(ModuleColumnEntity::getCreatorTime);

        Page<ModuleColumnEntity> page = new Page<>(pagination.getCurrentPage(), pagination.getPageSize());
        IPage<ModuleColumnEntity> iPage = this.selectPage(page, queryWrapper);
        return pagination.setData(iPage.getRecords(), iPage.getTotal());
    }

    default List<ModuleColumnEntity> getList(String moduleId) {
        QueryWrapper<ModuleColumnEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleColumnEntity::getModuleId, moduleId);
        // 排序
        queryWrapper.lambda().orderByAsc(ModuleColumnEntity::getSortCode)
                .orderByDesc(ModuleColumnEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default ModuleColumnEntity getInfo(String id) {
        QueryWrapper<ModuleColumnEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleColumnEntity::getId, id);
        return this.selectOne(queryWrapper);
    }

    default ModuleColumnEntity getInfo(String id, String moduleId) {
        QueryWrapper<ModuleColumnEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleColumnEntity::getId, id);
        queryWrapper.lambda().eq(ModuleColumnEntity::getModuleId, moduleId);
        return this.selectOne(queryWrapper);
    }

    default boolean isExistByFullName(String moduleId, String fullName, String id) {
        QueryWrapper<ModuleColumnEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleColumnEntity::getFullName, fullName).eq(ModuleColumnEntity::getModuleId, moduleId);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(ModuleColumnEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default boolean isExistByEnCode(String moduleId, String enCode, String id) {
        QueryWrapper<ModuleColumnEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ModuleColumnEntity::getEnCode, enCode).eq(ModuleColumnEntity::getModuleId, moduleId);
        if (!StringUtil.isEmpty(id)) {
            queryWrapper.lambda().ne(ModuleColumnEntity::getId, id);
        }
        return this.selectCount(queryWrapper) > 0;
    }

    default void create(ModuleColumnEntity entity) {
        entity.setSortCode(entity.getSortCode());
        entity.setId(RandomUtil.uuId());
        this.insert(entity);
    }

    default void create(List<ModuleColumnEntity> entitys) {
        Long sortCode = RandomUtil.parses();
        String userId = UserProvider.getUser().getUserId();
        for (ModuleColumnEntity entity : entitys) {
            entity.setId(RandomUtil.uuId());
            entity.setSortCode(sortCode++);
            entity.setEnabledMark("1".equals(String.valueOf(entity.getEnabledMark())) ? 0 : 1);
            entity.setCreatorUserId(userId);
            this.insert(entity);
        }
    }

    default void deleteByModuleId(String moduleId) {
        QueryWrapper<ModuleColumnEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ModuleColumnEntity::getModuleId, moduleId);
        this.deleteByIds(selectList(wrapper));
    }

    default List<ModuleColumnEntity> getListByModuleId(List<String> ids, Integer type) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ModuleColumnEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(ids, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().and(t ->
                t.in(ModuleColumnEntity::getModuleId, list).or()
            );
        }
        if (type == 1) {
            queryWrapper.lambda().eq(ModuleColumnEntity::getEnabledMark, 1);
        }
        queryWrapper.lambda().orderByAsc(ModuleColumnEntity::getSortCode).orderByDesc(ModuleColumnEntity::getCreatorTime);
        return this.selectList(queryWrapper);
    }

    default List<ModuleColumnEntity> getListByIds(List<String> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ModuleColumnEntity> queryWrapper = new QueryWrapper<>();
        List<List<String>> lists = Lists.partition(ids, 1000);
        for (List<String> list : lists) {
            queryWrapper.lambda().or().in(ModuleColumnEntity::getId, list);
        }
        queryWrapper.lambda().eq(ModuleColumnEntity::getEnabledMark, 1);
        return this.selectList(queryWrapper);
    }
}
